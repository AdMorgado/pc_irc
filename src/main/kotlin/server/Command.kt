package server;


sealed class Command();

class SayCommand(val text : String) : Command();
class HearCommand(val text : String) : Command();
class EnterCommand(val roomName : String) : Command();
class LeaveCommand() : Command();
class ExitCommand() : Command();

fun buildMessage(name : String, msg : String) =
    "[${name}]: $msg";
