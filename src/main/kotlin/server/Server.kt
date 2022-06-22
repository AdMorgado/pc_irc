package server

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val logger = LoggerFactory.getLogger(Server::class.java)

suspend fun AsynchronousServerSocketChannel.acceptConnection(): AsynchronousSocketChannel {
    return suspendCancellableCoroutine { continuation ->
        accept(null, object : CompletionHandler<AsynchronousSocketChannel, Any?> {
            override fun completed(socket: AsynchronousSocketChannel, attachment: Any?) {
                continuation.resume(socket)
            }

            override fun failed(error: Throwable, attachment: Any?) {
                continuation.resumeWithException(error)
            }
        })
    }
}

/**
 * Creates a Server Socket channel for external connections
 * @param address
 * @param executor
 * @return A AsynchronousServerSocketChannel bound to [address] operating on [executor]
 */
fun createChannel(address: InetSocketAddress, executor: ExecutorService): AsynchronousServerSocketChannel {
    val group = AsynchronousChannelGroup.withThreadPool(executor)
    val socket = AsynchronousServerSocketChannel.open(group)
    socket.bind(address)
    return socket
}

/**
 *
 *  The server instance will acquire ownership of [executor],
 *  becoming the one responsible of shutting it down once it's job has finished
 *  This instance is thread-safe
 *
 * @property address    address where the socket will operate on
 * @property executor   Thread Pool Executor to run server operations
 */
class Server(
    private val address: InetSocketAddress,
    private val executor: ThreadPoolExecutor,
    private val maxSessions: Int
) {

    private enum class State { NOT_STARTED, STARTED, STOPPED }

    private val sessionManager = SessionManager()
    private val roomSet = RoomSet()

    private val guard = Mutex()

    // Shared Mutable State, guarded by [guard]
    private lateinit var serverLoopJob: Job
    private lateinit var serverSocket: AsynchronousServerSocketChannel
    private var state = State.NOT_STARTED

    suspend fun hasStopped() = guard.withLock { state === State.STOPPED }

    /**
     *
     * @throws IllegalStateException if server has already started
     */
    suspend fun run() {
        guard.withLock {
            check(state === State.NOT_STARTED) { "Server can only run in a non started state" }
            serverSocket = createChannel(address, executor)
            val scope = CoroutineScope(executor.asCoroutineDispatcher())

            serverLoopJob = acceptLoop(scope)
            state = State.STARTED
        }
    }

    /**
     * Begins a coroutine responsible of accepting new connections and creating sessions
     * @return a Job that represent the accept loop coroutine
     */
    private suspend fun acceptLoop(scope: CoroutineScope) =
        scope.launch {
            try {
                val semaphore = Semaphore(maxSessions) // note it's kotlinx.coroutines.sync, not java.util.concurrent
                while (true) {
                    semaphore.acquire()
                    val socket = serverSocket.acceptConnection()
                    println("New Session!")
                    val session = sessionManager.createSession(socket, roomSet)
                    session.onStop {
                        sessionManager.removeSession(it.id)
                        semaphore.release()
                    }.start(this)
                }
            } catch (ex: ClosedChannelException) {
                logger.info("Channel has closed!")
            } catch (ex: Exception) {
                logger.info("An unknown error has occurred!")
                logger.info(ex.message)
            } finally {
                logger.info("Loop Job Shutting down!")
            }
        }

    /**
     * Sends a command to be processed by the server, including shutting it down, sending commands is only allowed if
     * the server is running and has not stopped
     */
    suspend fun sendCommand(input : String)  {
        check(guard.withLock { state } === State.STARTED) { "Server has not started yet or has stopped!" }

        val sanitizedInput = input.sanitize()
        val (cmd, args) = sanitizedInput.toLineCommand()
        if (cmd.firstOrNull() != COMMAND_PROMPT) return
        when (cmd.drop(1)) {
            "shutdown" -> {
                if (args.isEmpty()) {
                    logger.warn("Must pass timeout for shutdown command")
                }
                val timeout = args.first().toLongOrNull() ?: return
                shutdownAndJoin(timeout, TimeUnit.SECONDS)
            }
            "exit" ->       shutdownAndJoin()
            "rooms" ->      roomSet.printActiveUsers()
            "threads" ->    logger.info("${executor.activeCount} active threads")
            "sessions" ->   logger.info("${sessionManager.roaster.size} clients connected!")
            else ->         logger.warn("Invalid command!")
        }
    }

    private suspend fun stop() {
        guard.withLock {
            check(state === State.STARTED) { "Server can only be shutdown in a started state" }
            state = State.STOPPED
        }

        serverLoopJob.cancelAndJoin()

        sessionManager.roaster.forEach { it.stop() };

        serverSocket.close()
    }

    /**
     *
     * @throws IllegalStateException if server has not started already
     */
    suspend fun shutdownAndJoin(timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) {
        stop()
        executor.shutdown()
        if (!executor.awaitTermination(timeout, unit)) {
            logger.warn("Await termination has timed out, forcing shutdown")
            executor.shutdownNow()
        }
    }
}

