package server;

import java.nio.channels.AsynchronousSocketChannel

class Session(val id : Int, val name : String, val socket : AsynchronousSocketChannel);

