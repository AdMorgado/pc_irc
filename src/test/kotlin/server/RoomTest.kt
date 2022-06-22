package server;

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.Room
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.channels.AsynchronousServerSocketChannel
import java.util.concurrent.Executors
import kotlin.concurrent.thread
import kotlin.test.*

class RoomTest {

    private val executor = Executors.newSingleThreadExecutor()

    @Test
    fun `Basic Room Behaviour`()
    {
        val room = Room("");
        assertEquals(0, room.size) //no users in room
        assertTrue { room.getPlayerNames().isEmpty() }

        runBlocking {
            val serverSocket = createChannel(defaultServerAddress, executor)

            createFakeClientSocket()

            val sessionSocket = serverSocket.acceptConnection()
            val session = Session(0, sessionSocket, RoomSet());

            //no clients
            assertNull(room.leave(session.id))

            //1 client
            room.join(session)
            assertTrue {room.getPlayerNames().isNotEmpty() }
            assertSame(session, room.leave(session.id))
        }
    }
}


