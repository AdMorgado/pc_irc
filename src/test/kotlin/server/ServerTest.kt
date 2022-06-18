package server;

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import server.Server
import java.net.InetSocketAddress

class ServerTest {

    private val defaultServerAddress = InetSocketAddress("localhost", 8888);

    @Test
    fun `Basic Server Test`() {
        val server = Server(defaultServerAddress)
    }

    @Test
    fun `Illegal Uses`() {
        val server = Server(defaultServerAddress)

        assertThrows<IllegalStateException> {
            runBlocking {
                server.shutdownAndJoin()
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





