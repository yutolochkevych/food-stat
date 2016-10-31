package com.foodtstats

import akka.actor.ActorSystem
import com.foodtstats.JsonProtocol.{TranslateAnswerBody, TranslateRequestBody}
import org.apache.commons.lang.StringUtils
import spray.client.pipelining._
import spray.http.HttpRequest

import scala.concurrent.Future
import scala.io.Source
import scala.util.{Failure, Success, Try}
import spray.httpx.Json4sSupport
import org.json4s.{DefaultFormats, Formats}


case class ReviewMassage(toTranslate: List[Future[TranslateAnswerBody]])

object JsonProtocol extends Json4sSupport {
  implicit def json4sFormats: Formats = DefaultFormats

  case class TranslateRequestBody(input_lang: String, output_lang: String, text: String)

  case class TranslateAnswerBody(text: String)

}


trait TranslateParameters {
  val baseUrl: String
  val fileName: String = "/Reviews.csv"
  val chunkSize = 100
  val limitPerMessage = 1000
}


trait BaseTranslator {

  import scala.concurrent.ExecutionContext.Implicits.global

  val params: TranslateParameters
  val client: BaseClient
  var buffer: String

  def getSourceIter: Try[Iterator[String]] = {
    Try {
      val source = Source.fromURL(getClass.getResource(params.fileName))
      source.getLines()
    }
  }

  def canLoadMore(iter: Iterator[String]): Boolean = {
    val startSize = buffer.length
    loadNextSourceChunk(iter, 0)
    startSize != buffer.length
  }

  def loadNextSourceChunk(iter: Iterator[String], counter: Int): Unit = {
    if (iter.hasNext && counter < params.chunkSize) {
      val record: String = iter.next
      val review = record.substring(StringUtils.ordinalIndexOf(record, ",", 9) + 1)
      buffer += s"$review\n"
      loadNextSourceChunk(iter, counter + 1)
    }
  }

  def getNextChunkToTranslate(currentSize: Int, iter: Iterator[String]): List[Future[TranslateAnswerBody]] = {
    currentSize match {
      case size if size < params.chunkSize =>
        if (buffer.isEmpty && !canLoadMore(iter)) List()
        else {
          val body = divideMessageForRequest
          val futRequest = client.sendTranslateRequest(TranslateRequestBody("en", "ua", body), params.baseUrl)
          futRequest :: getNextChunkToTranslate(size + 1, iter)
        }
      case size if size >= params.chunkSize => List()
    }
  }

  def divideMessageForRequest(): String = {
    if (buffer.length < params.limitPerMessage) {
      val body = buffer
      buffer = ""
      body
    } else {
      val lastIndex = buffer.substring(0, params.limitPerMessage).lastIndexOf(" ")
      if (lastIndex == -1) buffer
      else {
        val body = buffer.substring(0, lastIndex)
        buffer = buffer.substring(lastIndex)
        body
      }
    }
  }

  def waitForTimeOut(futList: Future[List[TranslateAnswerBody]], strategy: ResponseWaitStrategy): Boolean = {
    Thread.sleep(20)
    if (!futList.isCompleted) {
      strategy match {
        case NonWaitStrategy => false
        case RepeatStrategy(t) if t > 0 => waitForTimeOut(futList, RepeatStrategy(t - 1))
        case RepeatStrategy(t) if t <= 0 => false
      }
    } else true
  }

  def runFlow(strategy: ResponseWaitStrategy)(implicit outBuffer: OutBuffer): Boolean = {

    val sourceIter = getSourceIter match {
      case Success(iter) => iter
      case Failure(error) =>
        println("Couldn't load source data")
        return false
    }
    while ((sourceIter.hasNext || !buffer.isEmpty)) {
      val futureSeq = Future.sequence(getNextChunkToTranslate(0, sourceIter))
      if (!waitForTimeOut(futureSeq, strategy)) {
        println("Server not answered for delay timeout. Exiting..")
        return false
      }
      futureSeq onComplete {
        case Success(posts) =>
          println("batch translated. Size:" + posts.size)
          outBuffer.processTranslated(posts)
        case Failure(t) =>
          println("An error has occured: " + t.getMessage)
      }
    }
    true
  }
}

case class Translator(params: TranslateParameters) extends BaseTranslator {
  override val client: BaseClient = RestClient()
  override var buffer: String = ""
}


trait BaseClient {
  def sendTranslateRequest(translateRequest: TranslateRequestBody, baseUrl: String): Future[TranslateAnswerBody]

}

case class RestClient() extends BaseClient {

  implicit val system = ActorSystem()

  import system.dispatcher

  override def sendTranslateRequest(translateRequest: TranslateRequestBody,
                                    baseUrl: String): Future[TranslateAnswerBody] = {
    import JsonProtocol._
    val pipeline: HttpRequest => Future[TranslateAnswerBody] = sendReceive ~> unmarshal[TranslateAnswerBody]
    pipeline(Post(baseUrl, translateRequest))
  }
}

trait ResponseWaitStrategy

object NonWaitStrategy extends ResponseWaitStrategy

case class RepeatStrategy(val times: Int) extends ResponseWaitStrategy

trait OutBuffer {
  def processTranslated(answerList: List[TranslateAnswerBody])
}

class NullBuffer extends OutBuffer {
  override def processTranslated(answer: List[TranslateAnswerBody]): Unit = ()
}
