package server;

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.MalformedInputException
import java.util.concurrent.TimeUnit

/**
 *
 */
class Session(
    val id : Int,
    val channel : Channel<Message>,
    private val socket : AsynchronousSocketChannel)
{

    private lateinit var txJob : Job
    private lateinit var rxJob : Job

    suspend fun start(scope : CoroutineScope)
    {
        txJob = startTxJob(scope);
        rxJob = startRxJob(scope);
    }

    suspend fun startTxJob(scope : CoroutineScope) =
        scope.launch {
            try {
                socket.use {
                    while (true) {
                        val msg = channel.receive()
                        println("Sending msg ${msg.text}")
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

    suspend fun startRxJob(scope : CoroutineScope) =
        scope.launch {
            try {
                while (true) {
                    val userInput = socket.suspendingRead(30, TimeUnit.SECONDS);
                    if (userInput == null) {
                        break;
                    }

                    channel.send(Message(userInput));
                }
            }
            catch(e : IOException) {
                println("Exception caught in RX for session $id");
                txJob.cancel(CancellationException("An error occurred while reading from the session socket"))
            }
            finally {

            }
        }
}



