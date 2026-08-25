package compose.conveyance

/** See the Android actual for why this always throws rather than guessing at a build type. */
internal actual fun defaultViolationHandler(): (String) -> Unit = { message ->
    throw ConveyViolationException(message)
}

internal actual fun conveyViolation(message: String) {
    defaultViolationHandler()(message)
}
