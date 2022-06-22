
import server.ALLOWED_SPECIAL_CHARACTERS
import server.sanitize
import kotlin.test.Test
import kotlin.test.assertEquals

class LineTest {
    @Test
    fun `Test sanitization`() {
        val empty = ""
        assertEquals(empty, empty.sanitize())
        assertEquals(ALLOWED_SPECIAL_CHARACTERS, ALLOWED_SPECIAL_CHARACTERS.sanitize())
        assertEquals(empty, "\\\"".sanitize()) //illegal characters
    }
}