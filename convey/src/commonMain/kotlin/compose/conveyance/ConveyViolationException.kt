package compose.conveyance

/** Thrown by the default [conveyViolation] handler. See [ConveySystem]'s `onViolation` param. */
class ConveyViolationException(message: String) : IllegalStateException(message)
