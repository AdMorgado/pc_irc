package server;

import java.nio.channels.AsynchronousSocketChannel

class Session(val id : Int, val name : String, val socket : AsynchronousSocketChannel);
<<<<<<< HEAD
=======

private val encoder = Charsets.UTF_8.newEncoder();
private val decoder = Charsets.UTF_8.newDecoder();

suspend fun Session.write(line : String) : Int {
    return suspendCancellableCoroutine { continuation ->
        val toSend = CharBuffer.wrap(line + "\n");

        //TODO: not production ready code, write may be split up
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
>>>>>>> 13cef4d8dfb8326d84426d04cc3f19d6b453fe78
