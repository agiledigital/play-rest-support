package au.com.agiledigital.rest.tests

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.WithApplication

/**
 * Around Scope that takes a ApplicationBuilder uses it to build an Application for a test.
 */
class WithApplicationBuilder(builder: GuiceApplicationBuilder) extends WithApplication(builder.build())
