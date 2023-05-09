import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import sttp.client3.{basicRequest, UriContext}
import zio.*
import zio.json.DeriveJsonCodec
import zio.redis.*
import zio.schema.DeriveSchema.gen
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, JsonCodec}

object Main extends ZIOAppDefault {

  case class dt(datetime: String, timezone: String)
  implicit val codec: zio.json.JsonCodec[dt] = DeriveJsonCodec.gen[dt]

  object CodecSupplier extends CodecSupplier {
    def get[A: Schema]: BinaryCodec[A] = JsonCodec.schemaBasedBinaryCodec
  }

  def exampleRequestToCache: ZIO[Redis & SttpClient & SttpRequestCaching, Throwable, dt] = ZIO.serviceWithZIO[SttpRequestCaching] {
    _.sendCached[dt](basicRequest.get(uri"http://worldtimeapi.org/api/ip"))
  }

  override def run: ZIO[Any, Throwable, Unit] = {
    for {
      response1 <- exampleRequestToCache
      _         <- ZIO.logInfo(response1.datetime)
      response2 <- exampleRequestToCache.delay(1.seconds)
      _         <- ZIO.logInfo(response2.datetime)
      response3 <- exampleRequestToCache.delay(1.seconds)
      _         <- ZIO.logInfo(response3.datetime)
    } yield ()
  }.provide(
    ZLayer.succeed(CodecSupplier),
    ZLayer.succeed(RedisConfig.Default),
    RedisExecutor.layer,
    Redis.layer,
    HttpClientZioBackend.layer(),
    ZLayer.succeed(Some(5.minutes)),
    SttpRequestCaching.layer,
  )

}
