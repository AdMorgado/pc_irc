
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

const val ENV_NAME_SERVER_PORT = "SERVER_PORT";

const val DEFAULT_HOST = "localhost";
const val DEFAULT_PORT : Short = 8080;

private val logger = LoggerFactory.getLogger("Coroutines and NIO")


fun main(args : Array<String>)
{
    val host = System.getenv("ENV_NAME_SERVER_HOST") ?: DEFAULT_HOST;
    val port = System.getenv(ENV_NAME_SERVER_PORT)?.toShortOrNull() ?: DEFAULT_PORT;
    logger.info("Starting.");
    logger.info("Host: $host, Port: $port");

    val executor = Executors.newSingleThreadExecutor()

    val scope = CoroutineScope(executor.asCoroutineDispatcher())
    val serverLoopJob = scope.launch {

    }

    serverLoopJob.cancel()

    logger.info("Shutting down.")
}


