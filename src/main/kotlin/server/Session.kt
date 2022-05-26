
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Session(val id : Int, val name : String, val socket : AsynchronousSocketChannel);

private val encoder = Charsets.UTF_8.newEncoder();
private val decoder = Charsets.UTF_8.newDecoder();

suspend fun Session.write(line : String) : Int {
    return suspendCancellableCoroutine { continuation ->
        val toSend = CharBuffer.wrap(line + "\n");

        //TODO: not production ready code, write may be splti up
        socket.write(encoder.encode(toSend), null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                continuation.resumeWithException(exc)
            }
        });
    }
}

suspend fun Session.read(timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) : String? {
    return suspendCancellableCoroutine { continuation ->

        val buffer = ByteBuffer.allocate(1024); //TODO: Averiguar tamanho do buffer
        socket.read(buffer, timeout, unit, null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {

            }
            override fun failed(error : Throwable, attachment: Any?) {
                if(error is InterruptedByTimeoutException) {
                    continuation.resume(null);
                } else {
                    continuation.resumeWithException(error);
                }
            }
        });
    }
}