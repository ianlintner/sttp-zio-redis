package services

import sttp.capabilities.zio.ZioStreams
import sttp.client3.httpclient.zio.SttpClient
import sttp.client3.ziojson.asJson
import sttp.client3.{Request, SttpBackend}
import zio.*
import zio.redis.*
import zio.schema.DeriveSchema.gen

trait SttpRequestCaching {

  def sendCached[T](
    r: Request[Either[String, String], Any],
    k: Option[String] = None,
  )(implicit codec: zio.json.JsonCodec[T], schema: zio.schema.Schema[T]): ZIO[Redis & SttpClient, Throwable, T]

}

case class DefaultSttpRequestCaching(backend: SttpBackend[Task, ZioStreams], redis: Redis, ttl: Option[Duration] = None) extends SttpRequestCaching {

  override def sendCached[T](
    r: Request[Either[String, String], Any],
    k: Option[String] = None,
  )(implicit codec: zio.json.JsonCodec[T], schema: zio.schema.Schema[T]): ZIO[Redis & SttpClient, Throwable, T] = {
    val key: String = k.getOrElse(r.uri.toString())
    redis
      .get(key)
      .returning[T]
      .someOrElseZIO(
        for {
          response <- r.response(asJson[T]).send(backend)
          body     <- response.body match {
            case Left(err)   => ZIO.fail(err)
            case Right(b: T) => redis.set(k, b, ttl) *> ZIO.succeed(b)
          }
        } yield body,
      )
  }

}

object SttpRequestCaching {
  val layer: URLayer[SttpBackend[Task, ZioStreams] & Redis & Option[Duration], SttpRequestCaching] = ZLayer.fromFunction(DefaultSttpRequestCaching.apply _)
}
