package compose.conveyance

import android.util.Log

internal actual fun defaultViolationHandler(): (String) -> Unit = { message ->
    if (BuildConfig.DEBUG) {
        throw ConveyViolationException(message)
    } else {
        Log.w("Convey", message)
    }
}

internal actual fun conveyViolation(message: String) {
    defaultViolationHandler()(message)
}
