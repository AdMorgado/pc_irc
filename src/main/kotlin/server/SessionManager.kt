package server

import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 *
 * This instance is thread-safe.
 */
class SessionManager {
    private val sessionCount = AtomicInteger(0)

    private val openSessions = ConcurrentHashMap<Int, Session>()

    /**
     * Creates a session
     * @param socket client's socket
     */
    fun createSession(socket: AsynchronousSocketChannel, roomSet: RoomSet): Session {
        return Session(sessionCount.incrementAndGet(), socket, roomSet).also {
            openSessions[it.id] = it
        }
    }

    /**
     * Removes a session by ID
     * @param id id of the session to remove
     */
    fun removeSession(id: Int) = openSessions.remove(id)

    /**
     *  Acquires a list of all open sessions created by this session manager
     */
    val roaster: List<Session>
        get() = openSessions.values.toList()
}



