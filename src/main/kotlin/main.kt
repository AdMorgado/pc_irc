import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import server.Server
import java.net.InetSocketAddress
import java.sql.SQLException
import java.util.concurrent.*

const val ARGS_MAX_USERS = "--max-users"
const val ARGS_SINGLE_THREADED = "--single-threaded"

const val ENV_NAME_SERVER_HOST = "SERVER_HOST"
const val ENV_NAME_SERVER_PORT = "SERVER_PORT"

const val DEFAULT_HOST = "localhost"
const val DEFAULT_PORT = 8888

const val SERVER_MAX_USERS = 100

private val logger = LoggerFactory.getLogger("pc_irc")

fun getMaxUsers(args: Array<String>): Int {
    return if (args.contains(ARGS_MAX_USERS)) {
        val idx = args.indexOfFirst { it == ARGS_MAX_USERS }
        if (idx == -1) {
            SERVER_MAX_USERS
        } else {
            args.getOrNull(idx + 1)?.toIntOrNull() ?: SERVER_MAX_USERS
        }
    } else {
        SERVER_MAX_USERS
    }
}

fun main(args: Array<String>) {
    try {
        val numOfThreads = if (args.contains(ARGS_SINGLE_THREADED)) 1 else Runtime.getRuntime().availableProcessors()
        val host = System.getenv(ENV_NAME_SERVER_HOST) ?: DEFAULT_HOST
        val port = System.getenv(ENV_NAME_SERVER_PORT)?.toIntOrNull() ?: DEFAULT_PORT

        val maxUsers = getMaxUsers(args)

        logger.info("Starting.")
        logger.info("Args: ${args.asList()}")
        logger.info("Number of Threads: $numOfThreads")
        logger.info("Host: $host, Port: $port")
        logger.info("Max Users: $maxUsers")

        val executor = ThreadPoolExecutor(1, numOfThreads,
            60L, TimeUnit.SECONDS,
            LinkedBlockingQueue())

        val server = Server(InetSocketAddress(host, port), executor, maxUsers)

        runBlocking {
            server.run()
            server.pollForCommands()
        }

    } catch (ex: SQLException) {
        logger.error("An unhandled exception has ocurred on the main thread")
        logger.error(ex.message)
    } finally {
        logger.info("Main thread finished.")
    }
}


