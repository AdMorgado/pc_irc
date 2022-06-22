package server;

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import server.Server
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.test.assertTrue

class ServerTest {

    private val defaultServerAddress = InetSocketAddress("localhost", 8888);
    private val executor = ThreadPoolExecutor(1, 1,
                        60L, TimeUnit.SECONDS,
                        LinkedBlockingQueue()
    )
    @Test
    fun `Basic Server Test`() {
        val server = Server(defaultServerAddress, executor, 1)
        runBlocking {
            server.run()
            server.sendCommand("/exit")
            assertTrue { server.hasStopped() }
        }
    }

    @Test
    fun `Illegal Uses`() {

        val server = Server(defaultServerAddress, executor, 5)

        assertThrows<IllegalStateException> {
            runBlocking {
                server.shutdownAndJoin()
            }
        }
        assertThrows<IllegalStateException> {
            runBlocking {
                server.sendCommand("")
            }
        }

        runBlocking {
            server.run()
        }

        assertThrows<IllegalStateException> {
            runBlocking {
                server.run()
            }
        }
    }
}





