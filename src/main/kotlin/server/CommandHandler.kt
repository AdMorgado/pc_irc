package server

const val COMMAND_PROMPT = '/'

const val COMMAND_WHO = "who"
const val COMMAND_EXIT = "exit"
const val COMMAND_ENTER = "enter"
const val COMMAND_LEAVE = "leave"

/**
 * @return returns the corresponding command to the input, null if the string does not represent a valid command
 */
fun String.toCommandOrNull(): Command? {
    if (isBlank()) return null

    if (first() != COMMAND_PROMPT)
        return SayCommand(this)

    val (cmd, args) = toLineCommand()

    return when (cmd.drop(1)) {
        COMMAND_WHO -> WhoCommand()
        COMMAND_EXIT -> ExitCommand()
        COMMAND_LEAVE -> LeaveCommand()
        COMMAND_ENTER -> args.firstOrNull()?.let{ EnterCommand(it) }

        else -> null
    }
}




