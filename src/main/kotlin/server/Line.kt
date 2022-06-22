package server

const val ALLOWED_SPECIAL_CHARACTERS = "/(){}[]!?#$%&='«»<>@£§"

/**
 * sanitizes a string and cleans it, used in user input
 */
fun String.sanitize(): String {
    //TODO: improve sanitation
    return trim().filter {
        it.isLetterOrDigit() || it in ALLOWED_SPECIAL_CHARACTERS
    }
}

/**
 * @return a pair, the first token is the first word of the string passed, and the second token is the list of
 * the other words
 */
fun String.toLineCommand(): Pair<String, List<String>> {
    if (isBlank()) return "" to emptyList()

    val inputList = split(" ").filter { it.isNotBlank() }
    return inputList.first().lowercase() to inputList.drop(1)
}
