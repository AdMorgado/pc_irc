package server;

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit

private val encoder = Charsets.UTF_8.newEncoder();
private val decoder = Charsets.UTF_8.newDecoder();

suspend fun AsynchronousSocketChannel.write(line : String) : Int {
    return suspendCancellableCoroutine { continuation ->
        val toSend = CharBuffer.wrap(line + "\n");

        //TODO: not production ready code, write may be split up
        write(encoder.encode(toSend), null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                continuation.resumeWithException(exc)
            }
        });
    }
}

suspend fun AsynchronousSocketChannel.read(timeout: Long = 0, unit: TimeUnit = TimeUnit.MILLISECONDS) : String? {
    return suspendCancellableCoroutine { continuation ->

        val buffer = ByteBuffer.allocate(1024); //TODO: Averiguar tamanho do buffer
        read(buffer, timeout, unit, null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                if (continuation.isCancelled) {
                    continuation.resumeWithException(CancellationException())
                } else {
                    val received = decoder.decode(buffer).toString();
                    continuation.resume(received);
                }
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



