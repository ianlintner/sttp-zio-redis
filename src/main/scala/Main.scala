import model.Dt.codec
import model.Dt
import services.{JsonCodecSupplier, SttpRequestCaching}
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import sttp.client3.{basicRequest, UriContext}
import zio.*
import zio.redis.{Redis, RedisConfig, RedisExecutor}
import zio.schema.DeriveSchema.gen

object Main extends ZIOAppDefault {

  def exampleRequestToCache: ZIO[Redis & SttpClient & SttpRequestCaching, Throwable, Dt] = ZIO.serviceWithZIO[SttpRequestCaching] {
    _.sendCached[Dt](basicRequest.get(uri"http://worldtimeapi.org/api/ip"), Option("api/ip"))
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
    JsonCodecSupplier.layer,
    ZLayer.succeed(RedisConfig.Default),
    RedisExecutor.layer,
    Redis.layer,
    HttpClientZioBackend.layer(),
    ZLayer.succeed(Some(5.minutes)),
    SttpRequestCaching.layer,
  )

}
