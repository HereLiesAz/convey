package compose.conveyance

internal actual fun defaultViolationHandler(): (String) -> Unit = { message ->
    println("Convey violation: $message")
}

internal actual fun conveyViolation(message: String) {
    defaultViolationHandler()(message)
}
