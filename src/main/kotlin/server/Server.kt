package server;

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
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

fun createChannel(address : InetSocketAddress, executor : ExecutorService) : AsynchronousServerSocketChannel
{
    val group = AsynchronousChannelGroup.withThreadPool(executor);
    val socket = AsynchronousServerSocketChannel.open(group);
    socket.bind(address)
    return socket;
}

/**
 *
 *  The server instance will acquire ownership of [executor],
 *  becoming the one responsible of shutting it down once it's job has finished
 *  This instance is thread-safe
 *
 * @property address
 * @property executor
 */
class Server(
    private val address : InetSocketAddress,
    private val executor : ThreadPoolExecutor) {

    private enum class State { NOT_STARTED, STARTED, STOPPED }


    private val sessionManager = SessionManager();
    private val roomSet = RoomSet();

    private val guard = Mutex();
    // Shared Mutable State, guarded by [guard]
    private lateinit var serverLoopJob : Job;
    private lateinit var serverSocket : AsynchronousServerSocketChannel;
    private var state = State.NOT_STARTED

    /**
     *
     * @throws IllegalStateException if server has already started
     */
    suspend fun run()
    {
        guard.withLock {
            check(state === State.NOT_STARTED) { "Server can only run in a non started state" }
            serverSocket = createChannel(address, executor)
            val scope = CoroutineScope(executor.asCoroutineDispatcher());

            serverLoopJob = acceptLoop(scope)
            state = State.STARTED;
        }
    }

    private suspend fun acceptLoop(scope : CoroutineScope) =
        scope.launch {
            try {
                while(true) {
                    val socket = serverSocket.acceptConnection();
                    println("New Session!");
                    val session = sessionManager.createSession(socket, roomSet);
                    session.onStop {
                        sessionManager.removeSession(it.id);
                    }
                        .start(this);
                }
            }
            catch(ex: ClosedChannelException) {
                logger.info("Channel has closed!")
            }
            finally {
                logger.info("Loop Job Shutting down!");
            }
        }

    suspend fun pollForCommands()
    {
        while(true)
        {
            val input = readlnOrNull() ?: break;
            val sanitizedInput = input.sanitize();
            val (cmd, args) = sanitizedInput.toLineCommand()

            when(cmd)
            {
                "${COMMAND_PROMPT}shutdown" -> {
                    if(args.isEmpty()) continue;
                    val timeout = args.first().toLongOrNull() ?: continue
                    shutdownAndJoin(timeout, TimeUnit.SECONDS)
                    break;
                }
                "${COMMAND_PROMPT}exit" ->  {
                    shutdown();
                    break
                }
                "${COMMAND_PROMPT}rooms" -> {
                    logger.info("${roomSet.size} open rooms")
                }
                "${COMMAND_PROMPT}threads" -> {
                    logger.info("${executor.activeCount} active threads")
                }
            }
        }
    }

    private suspend fun shutdown()
    {
        guard.withLock {
            check(state === State.STARTED) { "Server can only be shutdown in a started state" }
            state = State.STOPPED;
        }

        sessionManager.roaster.forEach { it.stop() }

        serverSocket.close();
        serverLoopJob.cancelAndJoin();
    }

    /**
     *
     * @throws IllegalStateException if server has not started already
     */
    suspend fun shutdownAndJoin(timeout : Long = 0, unit : TimeUnit = TimeUnit.MILLISECONDS)
    {
        shutdown();

        if(!executor.awaitTermination(timeout, unit))
        {
            executor.shutdownNow();
        }
    }
}

