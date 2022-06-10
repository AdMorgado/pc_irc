package server;

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
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
 * The server instance will acquire ownership of [executor],
 * becoming the one responsible of shutting it down once it's job has finished
 *
 * @property address
 * @property executor
 */
class Server(
    private val address : InetSocketAddress,
    private val executor : ExecutorService = Executors.newSingleThreadExecutor()) {

    private enum class State { NOT_STARTED, STARTED, STOPPED }

    private lateinit var serverLoopJob : Job;
    private lateinit var serverSocket : AsynchronousServerSocketChannel;

    private val guard = Mutex();
    // Shared Mutable State, guarded by [guard]

    suspend fun run()
    {
        serverSocket = createChannel(address, executor)

        val scope = CoroutineScope(executor.asCoroutineDispatcher());
        serverLoopJob = scope.launch {
            try {
                // clientID could be improved and not sequential as to prevent attacks
                val idGenerator = AtomicInteger(0);

                while(true) {
                    val socket = serverSocket.acceptConnection();
                    println("New Session!");
                    val session = Session(idGenerator.incrementAndGet(), socket);
                    // Reader Coroutine
                    session.start(this);
                }
            }
            catch(ex: ClosedChannelException) {
                logger.info("Channel has closed!")
            }
            finally {
                logger.info("Loop Job Shutting down!");
            }
        }
    }

    suspend fun shutdownAndJoin()
    {
        serverSocket.close();
        serverLoopJob.cancelAndJoin();

        executor.shutdown();
        if(!executor.awaitTermination(10, TimeUnit.SECONDS))
        {
            executor.shutdownNow();
        }
    }
}

