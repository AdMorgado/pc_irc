package server;

const val COMMAND_PROMPT = '/'

const val COMMAND_ENTER = "enter"
const val COMMAND_LEAVE = "leave"
const val COMMAND_EXIT = "exit"

/**
 *
 */
fun String.toCommandOrNull() : Command?
{
    if(isEmpty()) return null;

    if(first() != COMMAND_PROMPT)
        return SayCommand(this);

    val (cmd, args) = toLineCommand()

    return when(cmd.drop(1))
    {
        COMMAND_ENTER -> EnterCommand(args.first())
        COMMAND_LEAVE -> LeaveCommand()
        COMMAND_EXIT -> ExitCommand()
        else -> null
    }
}




