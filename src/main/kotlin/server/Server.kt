

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.*
import java.nio.channels.CompletionHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

fun createChannel(host : String, port : Int, executor : ExecutorService) : AsynchronousServerSocketChannel
{
    val group = AsynchronousChannelGroup.withThreadPool(executor);
    val socket = AsynchronousServerSocketChannel.open(group);
    socket.bind(InetSocketAddress(host, port))
    return socket;
}


class Server(private val hostname : String, private val port : Int, private val executor : ExecutorService) {

    fun start()
    {
        val serverSocket = createChannel(hostname, port, executor);

        val scope = CoroutineScope(executor.asCoroutineDispatcher());



        val serverLoopJob = scope.launch {

            // clientID could be improved and not sequential as to prevent attacks
            val idGenerator = AtomicInteger(0)

            while(true) {

                val socket = serverSocket.acceptConnection();
                val session = Session(idGenerator.incrementAndGet(), "King", socket);
                // Reader Coroutine
                launch {
                    while(true)
                    {
                        val userInput
                    }
                }
                // Transmitter Coroutine
                launch {
                    while(true) {

                        session.write("Ligmaballs");
                    }
                }
            }
        }

        pollForAdminCommands();

        runBlocking {
            serverLoopJob.cancelAndJoin();
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
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

