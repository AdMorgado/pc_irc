
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.RoomSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class RoomSetTest {

    private val NUM_OF_REPS = 1_000_000
    private val NUM_OF_ROOMS = 100

    @Test
    fun `Basic Room Set Test Behaviour`() {
        runBlocking {
            val roomSet = RoomSet()
            assertEquals(0, roomSet.size)
            val newRoomName = "test"
            val newRoom = roomSet.getRoom(newRoomName)
            assertSame(newRoomName, newRoom.name)
            assertEquals(1, roomSet.size)
            roomSet.checkRoom(newRoomName)
            assertEquals(0, roomSet.size)
        }
    }

    @Test
    fun `Stress test Room`() {

        runBlocking {
            val roomSet = RoomSet()

            (0 until NUM_OF_ROOMS).map {
                for(i in 0 until NUM_OF_REPS) {
                    roomSet.checkRoom(roomSet.getRoom(it.toString()).name)
                }
            }
            assertEquals(0, roomSet.size)
        }
    }
}