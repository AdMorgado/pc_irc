package server;

import kotlin.check

const val COMMAND_PROMPT = '/'

/**
 *
 */
fun String.toCommand() : Command?
{
    //TODO: Add actual sanitization to client input!
    val sanitized = trim();
    if(sanitized.isEmpty()) return null;

    check(sanitized.first() == COMMAND_PROMPT);
    val sanitizedList = sanitized.split(" ");
    val (cmd, args) = sanitizedList.first() to sanitizedList.drop(1);

    //TODO: Improve command decision into a hashmap
    return when(cmd)
    {
        "enter" -> EnterCommand(args.first())
        "leave" -> LeaveCommand()
        "exit" -> ExitCommand()
        else -> null
    }
}




