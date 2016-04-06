package au.com.agiledigital.rest.tests

import java.util.concurrent.TimeUnit

import org.specs2.control.NoLanguageFeatures
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.duration.FiniteDuration

/**
 * A base trait for Specs2 specifications.
 */
trait BaseSpec extends Specification with Mockito with NoLanguageFeatures {

  /**
   * Default number to retry when polling for an expected result (e.g. with eventually).
   */
  val defaultRetries: Int = 5

  /**
   * Default amount of time to wait between retries when polling for an expected result (e.g. with eventually).
   */
  val defaultTimeout: FiniteDuration = new FiniteDuration(2, TimeUnit.SECONDS)

  /**
   * Default amount of time to wait for a result (e.g. when dealing with Futures).
   */
  val defaultAwait: FiniteDuration = defaultTimeout * defaultRetries.toLong

  /**
   * Default number of seconds to wait for a result (e.g. when dealing with Futures).
   */
  val defaultDurationInSeconds: Long = defaultAwait.toSeconds
}
