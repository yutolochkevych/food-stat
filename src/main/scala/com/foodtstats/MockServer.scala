package com.foodtstats

import com.foodtstats.JsonProtocol.TranslateRequestBody
import org.http4s.server.blaze._
import org.http4s._
import org.http4s.MediaType._
import org.http4s.dsl._
import org.http4s.headers.`Content-Type`
import org.json4s.DefaultFormats
import org.http4s.json4s.jackson._
import org.http4s.server.Server
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

object MockServices{

  implicit val formats = DefaultFormats

  val helloWorldService = HttpService {

    case req @ POST -> Root / "translate" =>

      req.as(jsonExtract[TranslateRequestBody]).flatMap(mess =>

        Ok( compact(render("text"-> mess.text))).putHeaders(`Content-Type`(`application/json`)))

  }
}

class MockServer {
  var server: Server = _
  def run (): Unit = {
    val builder = BlazeBuilder.bindHttp(10000, "localhost").mountService(MockServices.helloWorldService, "/")
    server = builder.run
  }

  def stop (): Unit = {
    server.shutdownNow()
  }
}
