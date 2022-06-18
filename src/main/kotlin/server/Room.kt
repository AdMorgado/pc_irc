package server;

import java.util.concurrent.ConcurrentHashMap

/**
 * This class is thread-safe
 */
class Room {

    private val connectedSessions = ConcurrentHashMap<Int, Session>()

    private val roaster : List<Session>
        get() = connectedSessions.values.toList()

    suspend fun post(msg : String) {
        roaster.forEach {
            it.send(HearCommand(msg));
        }
    }

    fun join(session : Session)
    {
        connectedSessions[session.id] = session;
    }

    /**
     * @return returns null if the session was not present, otherwise the instance of the session
     */
    fun leave(sessionId : Int) : Session?
    {
        return connectedSessions.remove(sessionId);
    }
}





