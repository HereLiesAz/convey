package compose.conveyance.internal

import android.util.Log
import compose.conveyance.defaultViolationHandler
import compose.conveyance.conveyViolation

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

class ConveyViolationException(message: String) : IllegalStateException(message)
