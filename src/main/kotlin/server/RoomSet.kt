package server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger(RoomSet::class.java)

/**
 * A thread-safe collection of Rooms
 */
class RoomSet {

    // For compound actions on [rooms] the mutex is used to guarantee operation atomicity
    private val guard = Mutex()
    private val rooms = ConcurrentHashMap<String, Room>()
    val size: Int
        get() {
            return rooms.size
        }

    /**
     * @return the room that has been joined
     */
    suspend fun getRoom(roomName: String): Room {
        guard.withLock {
            return rooms.computeIfAbsent(roomName) { Room(roomName) }
        }
    }

    /**
     *  Checks if the room is empty, if it is remove it
     */
    suspend fun checkRoom(roomName: String) {
        guard.withLock {
            val room = rooms[roomName] ?: return
            if (room.size == 0) {
                rooms.remove(roomName)
            }
        }
    }

    /**
     * Prints to the console the active room names and how many sessions are connected to a room
     * This value may not represent the actual number of sessions on a given instance.
     */
    suspend fun printActiveUsers() {
        guard.withLock {
            logger.info("$size open rooms")
            rooms.forEach {
                val room = it.value
                logger.info("${it.key}: ${room.size}")
            }
        }
    }
}