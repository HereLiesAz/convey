package compose.conveyance

import platform.Foundation.NSLog

internal actual fun defaultViolationHandler(): (String) -> Unit = { message ->
    NSLog("Convey violation: %s", message)
}

internal actual fun conveyViolation(message: String) {
    defaultViolationHandler()(message)
}
