import model.Dt
import services.{JsonCodecSupplier, SttpRequestCaching}
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import sttp.client3.{basicRequest, Response, UriContext}
import zio.*
import zio.redis.*
import zio.redis.embedded.EmbeddedRedis
import zio.schema.DeriveSchema.gen
import zio.test.*

object SttpRequestCachingSpec extends ZIOSpecDefault {

  val stub = HttpClientZioBackend.stub
    .whenRequestMatches(r => r.uri.toString().endsWith("api/ip"))
    .thenRespondF(_ =>
      for {
        time <- Clock.currentDateTime
        r    <- ZIO.succeed(Response.ok(s"""{
                                        |"abbreviation": "CDT",
                                        |"client_ip": "23.88.134.51",
                                        |"datetime": "${time.toString}",
                                        |"day_of_week": 4,
                                        |"day_of_year": 131,
                                        |"dst": true,
                                        |"dst_from": "2023-03-12T08:00:00+00:00",
                                        |"dst_offset": 3600,
                                        |"dst_until": "2023-11-05T07:00:00+00:00",
                                        |"raw_offset": -21600,
                                        |"timezone": "America/Chicago",
                                        |"unixtime": 1683827093,
                                        |"utc_datetime": "2023-05-11T17:44:53.103605+00:00",
                                        |"utc_offset": "-05:00",
                                        |"week_number": 19
                                        |}""".stripMargin))
      } yield r,
    )

  def exampleRequestToCache: ZIO[Redis & SttpClient & SttpRequestCaching, Throwable, Dt] = ZIO.serviceWithZIO[SttpRequestCaching] {
    _.sendCached[Dt](basicRequest.get(uri"http://worldtimeapi.org/api/ip"), Option("api/ip"))
  }

  override def spec = {
    suite("SttpRequestCachingSpec")(
      test("test") {
        for {
          response1 <- exampleRequestToCache
          _         <- ZIO.logInfo(response1.datetime)
          s         <- ZIO.sleep(50.millis).fork
          _         <- TestClock.adjust(50.millis)
          _         <- s.join
          response2 <- exampleRequestToCache
        } yield assertTrue(response1.datetime == response2.datetime)
      },
    ).provide(
      JsonCodecSupplier.layer,
      EmbeddedRedis.layer,
      RedisExecutor.layer,
      Redis.layer,
      ZLayer.succeed(stub),
      ZLayer.succeed(Some(5.minutes)),
      SttpRequestCaching.layer,
    )
  }

}
