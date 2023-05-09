import sttp.client3.Request
import sttp.client3.httpclient.zio.SttpClient
import sttp.client3.ziojson.asJson
import zio.*
import zio.redis.*
import zio.schema.DeriveSchema.gen

trait SttpRequestCaching {

  def sendCached[T](
    r: Request[Either[String, String], Any],
  )(implicit codec: zio.json.JsonCodec[T], schema: zio.schema.Schema[T]): ZIO[Redis & SttpClient, Throwable, T]

}

case class DefaultSttpRequestCaching(backend: SttpClient, redis: Redis, ttl: Option[Duration] = None) extends SttpRequestCaching {

  def sendCached[T](
    r: Request[Either[String, String], Any],
  )(implicit codec: zio.json.JsonCodec[T], schema: zio.schema.Schema[T]): ZIO[Redis & SttpClient, Throwable, T] =
    redis
      .get(r.uri.toString())
      .returning[T]
      .someOrElseZIO(
        for {
          response <- r.response(asJson[T]).send(backend).map(_.body).absolve
          _        <- redis.set(r.uri.toString(), response, ttl)
        } yield response,
      )

}

object SttpRequestCaching {
  val layer: URLayer[SttpClient & Redis & Option[Duration], SttpRequestCaching] = ZLayer.fromFunction(DefaultSttpRequestCaching.apply _)
}
