package server;

import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

const val defaultHost = "localhost";
const val defaultPort = 8008;
val defaultAddress = InetSocketAddress(defaultHost, defaultPort)
val defaultServerAddress = InetSocketAddress(defaultHost, defaultPort)

fun createFakeClientSocket()
{
    thread {
        val socket = Socket(defaultHost, defaultPort);
        val dout = DataOutputStream(socket.getOutputStream())
    }
}