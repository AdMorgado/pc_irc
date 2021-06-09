package server;

import kotlinx.coroutines.*
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val TIMEOUT_DURATION : Long = 60;

const val COMMAND_SHUTDOWN = "/shutdown"

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
 * The server instance will acquire ownership of [executor],
 * becoming the one responsible of shutting it down once it's job has finished
 *
 * @property hostname
 * @property port
 * @property executor
 */
class Server(
    private val address : InetSocketAddress,
    private val executor : ExecutorService = Executors.newSingleThreadExecutor()) {

    private lateinit var serverLoopJob : Job;
    private lateinit var serverSocket : AsynchronousServerSocketChannel


    fun run()
    {
        serverSocket = createChannel(address, executor);

        val scope = CoroutineScope(executor.asCoroutineDispatcher());

        serverLoopJob = scope.launch {

            // clientID could be improved and not sequential as to prevent attacks
            val idGenerator = AtomicInteger(0)

            while(true) {

                val socket = serverSocket.acceptConnection();
                println("New Session!");
                val session = Session(idGenerator.incrementAndGet(), "King", socket);
                // Reader Coroutine
                val rxJob = launch {
                    try {
                        while (true) {
                            val userInput = session.read(TIMEOUT_DURATION, TimeUnit.SECONDS);
                            //if(userInput != null)
                            //    session.write(userInput);
                        }
                    }
                    finally {

                    }
                }
                // Transmitter Coroutine
                val txJob = launch {
                    try {
                        while(true)
                        {

                        }
                    }
                    finally {

                    }
                }
            }
        }
    }

    fun shutdownAndJoin()
    {

        serverSocket.close();

    }

    private fun pollForAdminCommands()
    {
        while(true)
        {
            val input = readln();

            //TODO: Improve command handling
            when(input)
            {
                COMMAND_SHUTDOWN -> {
                    break;
                }
                else -> {
                    println("Unrecognized Command!");
                }
            }
        }
    }
}

