package au.com.agiledigital.rest.tests

/**
 * Disables the stack trace that is normally set on Throwables. Use this if you want to make the logs produced when
 * testing failure scenarios less noisy and slightly less scary looking.
 */
class NoStackTraceException(message: String) extends RuntimeException(message) {
  override def fillInStackTrace(): Throwable = this
}
