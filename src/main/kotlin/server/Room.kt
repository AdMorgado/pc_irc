package server;

import java.util.concurrent.ConcurrentHashMap


class Room {

    private val connectedSessions = ConcurrentHashMap<Int, Session>()

    val roaster : List<Session>
        get() = connectedSessions.values.toList()

    suspend fun join(session : Session)
    {
        connectedSessions[session.id] = session;
    }

    suspend fun leave(session : Session)
    {
        connectedSessions.remove(session.id);
    }

    suspend fun sendMessage(msg : Message)
    {
        roaster.forEach {
            it.send(msg);
        }
    }
}





