package server;

sealed class Command();

class EnterCommand(val roomName : String) : Command();
class LeaveCommand() : Command();
class ExitCommand() : Command();
