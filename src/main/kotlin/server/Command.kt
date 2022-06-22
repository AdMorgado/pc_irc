package server

/**
 * Sum-Type abstract class to represent Client Commands
 */
sealed class Command

class SayCommand(val text: String) : Command()
class HearCommand(val text: String) : Command()
class WhoCommand : Command()
class EnterCommand(val roomName: String) : Command()
class LeaveCommand : Command()
class ExitCommand : Command()

/**
 * Builds a formatted string to be read on the group chat
 * @return returns the built message
 */
fun buildMessage(roomName: String, name: String, msg: String) =
    "[$roomName] $name: $msg"
