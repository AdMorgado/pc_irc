package server;

import java.util.concurrent.ConcurrentHashMap


/**
 *
 * This class is thread-safe
 */
class RoomSet {

    private val rooms = ConcurrentHashMap<String, Room>()
    val size : Int
        get() { return rooms.size }

    /**
     * @return the room that has been joined
     */
    fun getRoom(roomName: String): Room {
        return rooms.computeIfAbsent(roomName) { Room() };
    }

    /**
     *
     */
    fun removeRoom(roomName : String) {
        rooms.remove(roomName);
    }
}