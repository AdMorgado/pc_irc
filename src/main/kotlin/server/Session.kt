package server;

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.MalformedInputException
import java.util.concurrent.TimeUnit

/**
 * @property id
 * @property channel
 * @property socket
 */
class Session(
    val id : Int,
    private val socket : AsynchronousSocketChannel
    private val inputHandler : ((String) -> Unit)? = null)
{
    private enum class SessionState {
        NOT_STARTED, STARTED
    }

    private val channel = Channel<Message>();

    val guard = Mutex();

    //shared mutable state guarded by [guard]
    private var state = SessionState.NOT_STARTED;
    private lateinit var txJob : Job;
    private lateinit var rxJob : Job;

    suspend fun start(scope : CoroutineScope)
    {
        txJob = startTxJob(scope);
        if(inputHandler != null)
            rxJob = startRxJob(scope);

        guard.withLock { state = SessionState.STARTED }
    }

    suspend fun send(msg : Message)
    {
        guard.withLock {
            check(state === SessionState.STARTED) { "Session $id has not started" };
        }

        channel.send(msg);
    }

    private fun startTxJob(scope : CoroutineScope) =
        scope.launch {
            try {
                socket.use {
                    while (true) {
                        val msg = channel.receive()

                        it.suspendingWrite(msg.text);
                    }
                }
            } catch (e: Exception) {
                println("Error on TX $id: ${e.message}")
                rxJob.cancelAndJoin()
            }
            finally {
                println("User $id has disconnected!");
            }
        }

    private fun startRxJob(scope : CoroutineScope) =
        scope.launch {
            try {
                while (true) {
                    val userInput = socket.suspendingRead(30, TimeUnit.SECONDS);
                    if (userInput == null) break;
                    inputHandler?.invoke(userInput);
                }
            }
            catch(e : IOException) {
                println("Exception caught in RX for session $id");
                println(e.message);
                txJob.cancel(CancellationException("An error occurred while reading from the session socket"))
            }
            finally {
                println("RX $id has stopped!");
            }
        }
}



