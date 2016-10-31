package com.rest

import com.foodtstats.JsonProtocol.{TranslateAnswerBody, TranslateRequestBody}
import com.foodtstats._
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}


class ReviewTranslateTest extends FlatSpec with Matchers {

  import scala.concurrent.ExecutionContext.Implicits.global

  val testReviews = List("I have bought several of the Vitality canned dog food products and" +
    " have found them all to be of good quality.",
  "\"Product arrived labeled as Jumbo Salted Peanuts...the peanuts were actually small sized unsalted.",
  "\"This is a confection that has been around a few centuries.",
    "If you are looking for the secret ingredient in Robitussin I believe I have found it.",
    "\"Great taffy at a great price.  There was a wide assortment of yummy taffy.",
  "\"This saltwater taffy had great flavors and was very soft and chewy.",
  "This taffy is so good.  It is very soft and chewy.",
    "Right now I'm mostly just sprouting this so my cats can eat the grass. They love it."
  )

  val testParams = new TranslateParameters() {
    override val baseUrl: String = "http://localhost:10000/translate"
    override val fileName : String = "/testData.csv"
    override val chunkSize = 5
    override val limitPerMessage = 100
  }

  case class MockRequest() extends BaseClient {

    override def sendTranslateRequest(translateRequest: TranslateRequestBody,
                                      baseUrl: String): Future[TranslateAnswerBody] = Future[TranslateAnswerBody] {
      Thread.sleep(10)
      TranslateAnswerBody(translateRequest.text)
    }
  }

case class TestTranslator(val client: BaseClient) extends BaseTranslator {
  override val params: TranslateParameters = testParams
  override var buffer = ""

}
  "test source chunk loading" should
    "load correctly" in {
    val testTranslator = TestTranslator(MockRequest())
    val iter=  testTranslator.getSourceIter.get
    testTranslator.loadNextSourceChunk(iter,0)
    val testAnswer = testReviews.take(testParams.chunkSize).mkString("\n")
    assert(testTranslator.buffer==testAnswer)

  }
    it should
      "create correct batch to load" in {
      val testTranslator = TestTranslator(MockRequest())
      val iter=  testTranslator.getSourceIter.get
      val chunkFutures = testTranslator.getNextChunkToTranslate(0, iter)
      val futureList = Future.sequence(chunkFutures)
      val res= Await.result(futureList, 1.seconds)

      assert(chunkFutures.length==testParams.chunkSize)
      assert(res.foldLeft("")(_+_.text).length<testParams.limitPerMessage*testParams.limitPerMessage)
    }
    it should
      "correct translate" in {
      class  TestBuffer() extends OutBuffer {
        val buff = ArrayBuffer[TranslateAnswerBody]()
        override def processTranslated(answer: List[TranslateAnswerBody]): Unit = buff++=answer
      }

      implicit val buffer =new  TestBuffer()
      val testTranslator = TestTranslator(MockRequest())
      testTranslator.runFlow(NonWaitStrategy)

      assert(buffer.buff.map(_.text).mkString("").contains(testReviews.mkString("\n")))

    }
}
