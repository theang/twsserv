package serv1.backend

import serv1.backend.app.HistoricalData
import serv1.backend.serialize.SerializeTOHLCV
import serv1.db.schema.TickerData
import zio.json._
import zio.test._

object TickerDataSpec extends ZIOSpecDefault {
  val TEST_JSON = "[{\"id\":0,\"time\":10,\"open\":20,\"high\":30,\"low\":40,\"close\":50,\"vol\":10.1}]"
  val TEST_BINARY: String = "0A 00 00 00 00 00 00 00" +
    " 00 00 00 00 00 00 34 40" +
    " 00 00 00 00 00 00 3E 40" +
    " 00 00 00 00 00 00 44 40" +
    " 00 00 00 00 00 00 49 40" +
    " 33 33 33 33 33 33 24 40"

  def spec: Spec[Any, Nothing] = suite("TickerData serialization tests")(
    test("Test json serialization of TickerData") {
      implicit val jsonCodec: JsonCodec[TickerData] = HistoricalData.tickerDataJson
      val ticker = Seq(TickerData(0, 10, 20, 30, 40, 50, 10.10))
      val tickerJson = ticker.toJson
      assertTrue(TEST_JSON == tickerJson)
    }
    , test("Test binary serialization of TickerData") {
      val ticker = Seq(TickerData(0, 10, 20, 30, 40, 50, 10.10))

      //val stringBuilder: StringBuilder = new StringBuilder()
      def hex = { a: Integer =>
        if (a < 10) {
          ('0'.toInt + a).toChar
        }
        else {
          ('A'.toInt + (a - 10)).toChar
        }
      }

      def byteToString = { b: Byte => s"${hex(b / 16 % 16)}${hex(b % 16)}" }

      val tickerBinary = SerializeTOHLCV.serialize(ticker).map(byteToString).mkString(" ")
      assertTrue(TEST_BINARY == tickerBinary)
    }
  )
}
