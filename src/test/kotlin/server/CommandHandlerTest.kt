
import server.*
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandHandlerTest {

    @Test
    fun `toCommandOrNull Behavior`() {

        assertNull("".toCommandOrNull())

        assertTrue {
            "${COMMAND_PROMPT}${COMMAND_WHO}".toCommandOrNull() is WhoCommand &&
            "${COMMAND_PROMPT}${COMMAND_EXIT}".toCommandOrNull() is ExitCommand &&
            "${COMMAND_PROMPT}${COMMAND_LEAVE}".toCommandOrNull() is LeaveCommand &&
            "${COMMAND_PROMPT}${COMMAND_ENTER} TEST".toCommandOrNull() is EnterCommand
        }
        assertNull("${COMMAND_PROMPT}${COMMAND_ENTER}".toCommandOrNull())
        //testing not adding command prompt
        assertTrue {
            COMMAND_LEAVE.toCommandOrNull() is SayCommand &&
            COMMAND_ENTER.toCommandOrNull() is SayCommand &&
            COMMAND_LEAVE.toCommandOrNull() is SayCommand &&
            COMMAND_ENTER.toCommandOrNull() is SayCommand
        }
    }
}
