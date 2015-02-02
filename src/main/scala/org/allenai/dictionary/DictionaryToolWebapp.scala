package org.allenai.dictionary

import spray.routing.HttpService
import spray.httpx.SprayJsonSupport
import spray.json._
import DefaultJsonProtocol._
import akka.actor.Actor
import spray.util.LoggingContext
import spray.routing.ExceptionHandler
import spray.http.StatusCodes
import spray.http.HttpHeaders
import spray.http.CacheDirectives
import akka.actor.ActorSystem
import akka.actor.Props
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import akka.io.IO
import spray.can.Http
import akka.pattern.ask
import spray.json._
import DefaultJsonProtocol._
import JsonSerialization._
import akka.actor.ActorContext
import com.typesafe.config.ConfigFactory

object DictionaryToolWebapp extends App {
  val name = "dictionary-tool"
  override def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("dictionary-tool")
    val service = system.actorOf(Props[DictionaryToolActor], "webapp-actor")

    {
      implicit val timeout = Timeout(30.seconds)
      IO(Http) ? Http.Bind(service, interface = "0.0.0.0", port = 8080)
    }
  }
}

trait BasicService extends HttpService {
  val staticContentRoot = "public"
  val basicRoute =
    path("") {
      get {
        getFromFile(staticContentRoot + "/index.html")
      }
    } ~
      get {
        unmatchedPath { p => getFromFile(staticContentRoot + p) }
      }
}

class DictionaryToolActor extends Actor with BasicService with SprayJsonSupport {
  import JsonSerialization._
  val config = ConfigFactory.load
  val searchApp = SearchApp(config.getConfig("index"))
  val serviceRoute = pathPrefix("api" / "groupedSearch") {
    post {
      entity(as[SearchRequest]) { req =>
        complete(searchApp.groupedSearch(req))
      }
    }
  }

  implicit def myExceptionHandler(implicit log: LoggingContext): ExceptionHandler =
    ExceptionHandler {
      case e: Exception =>
        requestUri { uri =>
          log.error(toString, e)
          complete(StatusCodes.InternalServerError -> e.getMessage)
        }
    }
  def actorRefFactory: ActorContext = context
  val cacheControlMaxAge = HttpHeaders.`Cache-Control`(CacheDirectives.`max-age`(0))
  def receive: Actor.Receive = runRoute(basicRoute ~ serviceRoute)
}
