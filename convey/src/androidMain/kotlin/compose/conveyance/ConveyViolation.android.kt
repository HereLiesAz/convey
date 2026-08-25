package compose.conveyance

/**
 * Android's default [conveyViolation] handler throws unconditionally.
 *
 * There is no reliable way to tell a debug build from a release build inside a library function
 * with no [android.content.Context] to read `ApplicationInfo.FLAG_DEBUGGABLE` from. Rather than
 * guess, the default always throws -- the same fail-loud stance [ConveyGrammar] already takes for
 * unknown meanings. A consumer that wants violations logged instead of thrown in its own release
 * builds passes `onViolation` to [ConveySystem] and makes that call itself.
 */
internal actual fun defaultViolationHandler(): (String) -> Unit = { message ->
    throw ConveyViolationException(message)
}

internal actual fun conveyViolation(message: String) {
    defaultViolationHandler()(message)
}
