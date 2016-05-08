package au.com.agiledigital.rest.controllers.transport

import java.security.cert.X509Certificate

import play.api.mvc.{ Headers, RequestHeader }

/**
  * Empty request header implementation.
  */
class EmptyRequestHeader extends RequestHeader {

  override def id: Long = 1L

  override def secure: Boolean = false

  override def uri: String = ""

  override def remoteAddress: String = ""

  override def queryString: Map[String, Seq[String]] = Map.empty

  override def method: String = ""

  override def headers: Headers = Headers()

  override def path: String = ""

  override def version: String = ""

  override def tags: Map[String, String] = Map.empty

  override def clientCertificateChain: Option[Seq[X509Certificate]] = None
}
