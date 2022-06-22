package server

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val INPUT_BUFFER_SIZE = 1024

val CHARSET = Charsets.UTF_8
private val encoder = CHARSET.newEncoder()
private val decoder = CHARSET.newDecoder()

/**
 *
 */
suspend fun AsynchronousSocketChannel.suspendingWrite(line: String): Int {
    return suspendCancellableCoroutine { continuation ->
        val toSend = CharBuffer.wrap(line + "\n")

        //TODO: not production ready code, write may be split up
        write(encoder.encode(toSend), null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                continuation.resume(result)
            }

            override fun failed(exc: Throwable, attachment: Any?) {
                continuation.resumeWithException(exc)
            }
        })
    }
}

/**
 *  @param timeout
 *  @param unit
 *  @return returns the string sent by session client, an empty string if a character coding exception occurred and
 *  and null if the reading has timed out
 */
suspend fun AsynchronousSocketChannel.suspendingRead(
    timeout: Long = 0,
    unit: TimeUnit = TimeUnit.MILLISECONDS
): String? {
    return suspendCancellableCoroutine { continuation ->
        val buffer = ByteBuffer.allocate(INPUT_BUFFER_SIZE)
        read(buffer, timeout, unit, null, object : CompletionHandler<Int, Any?> {
            override fun completed(result: Int, attachment: Any?) {
                if (continuation.isCancelled) {
                    continuation.resumeWithException(CancellationException())
                } else {
                    // Try to catch malformed inputs that decoder produces, if any is caught, return an empty string
                    try {
                        val received = decoder.decode(
                            buffer.flip()
                        ).toString().trim()
                        continuation.resume(received)
                    } catch (ex: CharacterCodingException) {
                        println("A character coding exception!")
                        continuation.resume("")
                    }
                }
            }

            override fun failed(error: Throwable, attachment: Any?) {
                if (error is InterruptedByTimeoutException) {
                    continuation.resume(null)
                } else {
                    continuation.resumeWithException(error)
                }
            }
        })
    }
}



