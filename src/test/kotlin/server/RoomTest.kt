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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RoomTest {

    private val executor = Executors.newSingleThreadExecutor()



    @Test
    fun `Basic Room Behaviour`()
    {
        val room = Room();
        assertEquals(0, room.roaster.size) //no users in room

        runBlocking {
            val serverSocket = createChannel(defaultServerAddress, executor)

            createFakeClientSocket()

            val sessionSocket = serverSocket.acceptConnection()
            val session = Session(0, sessionSocket);

            //no clients
            assertNull(room.leave(session))

            //1 client
            room.join(session)
            assertSame(session, room.roaster.first())
            assertSame(session, room.leave(session))
        }
    }
}


