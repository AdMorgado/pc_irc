
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.nio.channels.AsynchronousServerSocketChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

const val ARGS_SINGLE_THREADED = "--single-threaded"

const val ENV_NAME_SERVER_HOST = "SERVER_HOST"
const val ENV_NAME_SERVER_PORT = "SERVER_PORT";

const val DEFAULT_HOST = "localhost";
const val DEFAULT_PORT = 8888;

private val logger = LoggerFactory.getLogger("Coroutines and NIO")


fun main(args : Array<String>)
{
    val numOfThreads = if(args.contains(ARGS_SINGLE_THREADED)) 1 else Runtime.getRuntime().availableProcessors();
    val host = System.getenv(ENV_NAME_SERVER_HOST) ?: DEFAULT_HOST;
    val port = System.getenv(ENV_NAME_SERVER_PORT)?.toIntOrNull() ?: DEFAULT_PORT;

    logger.info("Starting.");
    logger.info("Args: ${args.asList()}")
    logger.info("Number of Threads: $numOfThreads");
    logger.info("Host: $host, Port: $port");

    // minimum of threads 1 as to always have an available thread to accept connections
    val executor = ThreadPoolExecutor(1, numOfThreads,
        60L, TimeUnit.SECONDS,
        SynchronousQueue())

    val server = Server(host, port, executor);

    server.start();

    logger.info("Shutting down.")
}


