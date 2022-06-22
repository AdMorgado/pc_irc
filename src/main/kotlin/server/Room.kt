package server

import java.util.concurrent.ConcurrentHashMap

/**
 * This class is thread-safe
 */
class Room(val name: String) {

    private val connectedSessions = ConcurrentHashMap<Int, Session>()

    private val roaster: List<Session>
        get() = connectedSessions.values.toList()
    val size: Int
        get() = connectedSessions.values.size

    suspend fun post(msg: String) {
        roaster.forEach {
            it.send(HearCommand(msg))
        }
    }

    fun getPlayerNames(): List<String> {
        return roaster.map { it.id.toString() }
    }

    /**
     * sesions joins the roaster in the room, for transfering messages in between
     * @param session the session to join
     */
    fun join(session: Session) {
        connectedSessions[session.id] = session
    }

    /**
     * @return returns null if the session was not present, otherwise the instance of the session
     */
    fun leave(sessionId: Int): Session? {
        return connectedSessions.remove(sessionId)
    }
}





