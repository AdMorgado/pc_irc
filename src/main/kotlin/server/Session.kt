package server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger(Session::class.java)

/**
 *
 * @property id
 * @property channel
 * @property sessionSocket
 * @property channel
 */
class Session(
    val id: Int,
    private val sessionSocket: AsynchronousSocketChannel,
    private val roomSet: RoomSet
) {
    private enum class SessionState {
        NOT_STARTED, STARTED, STOPPED
    }

    private val channel = Channel<Command>(UNLIMITED)

    private val guard = Mutex()

    //shared mutable state guarded by [guard]
    private var state = SessionState.NOT_STARTED
    private var stopHandler: ((Session) -> Unit)? = null
    private lateinit var txJob: Job
    private lateinit var rxJob: Job
    private var room: Room? = null

    /**
     *  @return returns itself for syntax-sugar
     */
    suspend fun start(scope: CoroutineScope): Session {
        guard.withLock {
            check(state === SessionState.NOT_STARTED) { "Session has already started!" }
            txJob = startTxJob(scope)
            rxJob = startRxJob(scope)

            state = SessionState.STARTED
        }

        return this
    }

    /**
     * @return returns itself for syntax-sugar
     */
    suspend fun stop(): Session {
        guard.withLock {
            check(state === SessionState.STARTED) { "Session has not started" }
            state = SessionState.STOPPED
        }
        rxJob.cancel()
        channel.send(ExitCommand())
        return this
    }


    /**
     *  @return returns itself for syntax-sugar
     */
    suspend fun onStop(handler: (Session) -> Unit): Session {
        guard.withLock {
            stopHandler = handler
        }
        return this
    }

    /**
     * Send commands to the session to be queued and processed
     * @param msg command to be queued and processed
     */
    suspend fun send(msg: Command) {
        guard.withLock {
            check(state === SessionState.STARTED) { "Session $id has not started" }
        }
        channel.send(msg)
    }

    private suspend fun joinRoom(roomName: String) {
        guard.withLock {
            if (room != null) return
        }
        val newRoom = roomSet.getRoom(roomName)
        newRoom.join(this)
        guard.withLock {
            room = newRoom
        }
    }

    /**
     * If the session is present in a room, it will leave it, furthermore signalling the roomSet that it may close the room
     */
    private suspend fun leaveRoom() {
        guard.withLock {
            with(room) {
                if (this == null) return
                this.leave(this@Session.id)
                roomSet.checkRoom(name)
            }
            room = null
        }
    }

    /**
     * Posts a message to the room
     */
    private suspend fun say(socket: AsynchronousSocketChannel, msg: String) {
        val currRoom = guard.withLock { room }
        if (currRoom == null) {
            socket.suspendingWrite("You can only send messages inside a room!")
        } else {
            val formattedText = buildMessage(currRoom.name, id.toString(), msg)
            currRoom.post(formattedText)
        }
    }

    private suspend fun who(socket: AsynchronousSocketChannel) {
        val currRoom = guard.withLock { room }
        if (currRoom == null) {
            socket.suspendingWrite("Must be inside of a room to call this command!")
            return
        }
        val playerNames = currRoom.getPlayerNames()
        socket.suspendingWrite("${playerNames.size} clients in the room")
        playerNames.forEach {
            socket.suspendingWrite(it)
        }
    }

    /**
     * Initiates the coroutine responsible for receiving commands from [channel], processing them and sending messages
     * to the session.
     * @param scope the scope where to launch the coroutine on
     */
    private fun startTxJob(scope: CoroutineScope) =
        scope.launch {
            try {
                sessionSocket.use {
                    while (true) {
                        val msg = channel.receive()
                        when (msg) {
                            is EnterCommand -> joinRoom(msg.roomName)
                            is HearCommand  -> it.suspendingWrite(msg.text)
                            is LeaveCommand -> leaveRoom()
                            is SayCommand   -> say(it, msg.text)
                            is WhoCommand   -> who(it)
                            is ExitCommand  -> break
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error on TX $id: ${e.message}")
                rxJob.cancelAndJoin()
            } finally {
                leaveRoom()
                guard.withLock { stopHandler }?.invoke(this@Session)
                logger.info("User $id has disconnected!")
            }
        }


    /**
     * Initiates the coroutine responsible for receiving input from the session client, processing it
     * and sending commands to [channel]
     * @param scope the scope where to launch the coroutine on
     */
    private fun startRxJob(scope: CoroutineScope) =
        scope.launch {
            try {
                while (true) {
                    val userInput = sessionSocket.suspendingRead(5, TimeUnit.MINUTES)
                    if (userInput == null) {
                        stop()
                    } else {
                        val cmd = userInput.sanitize().toCommandOrNull()
                        if (cmd == null)
                            channel.send(HearCommand("Invalid input!"))
                        else
                            channel.send(cmd)
                    }
                }
            } catch (e: IOException) {
                logger.error("Exception caught in RX for session $id -${e.message}")
                txJob.cancel(CancellationException("An error occurred while reading from the session socket"))
            }
        }
}



