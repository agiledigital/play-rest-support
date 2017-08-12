package au.com.agiledigital.rest.controllers.transport

import java.net.InetAddress
import java.security.cert.X509Certificate

import play.api.libs.typedmap.TypedMap
import play.api.mvc.request.{ RemoteConnection, RequestTarget }
import play.api.mvc.{ Headers, RequestHeader }

/**
  * Empty request header implementation.
  */
class EmptyRequestHeader extends RequestHeader {
  override def connection: RemoteConnection = new RemoteConnection {
    override def remoteAddress: InetAddress = InetAddress.getByName("")

    override def secure: Boolean = false

    override def clientCertificateChain: Option[Seq[X509Certificate]] = None
  }

  override def method: String = ""

  override def target: RequestTarget = RequestTarget("", "", Map.empty)

  override def version: String = ""

  override def headers: Headers = Headers()

  override def attrs: TypedMap = TypedMap.empty
}