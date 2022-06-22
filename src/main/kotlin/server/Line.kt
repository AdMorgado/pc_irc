package server

fun String.sanitize(): String {
    //TODO: improve sanitiation
    return trim()
}

fun String.toLineCommand(): Pair<String, List<String>> {
    if (isBlank()) return "" to emptyList()

    val inputList = split(" ").filter { it.isNotBlank() }
    return inputList.first().lowercase() to inputList.drop(1)
}
