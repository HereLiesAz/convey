package compose.conveyance

/**
 * wasmJs has no `BuildConfig.DEBUG` equivalent — nothing reliably distinguishes a debug run from
 * a shipped one — so enforcement is unconditional. See the `expect` declaration's doc for the
 * full per-platform accounting and the supported opt-out.
 */
internal actual fun defaultViolationHandler(): (String) -> Unit = { message ->
    throw ConveyViolationException(message)
}

internal actual fun conveyViolation(message: String) {
    defaultViolationHandler()(message)
}
