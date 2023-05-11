package model

import zio.json.DeriveJsonCodec

case class Dt(datetime: String, timezone: String)

object Dt {
  implicit val codec: zio.json.JsonCodec[Dt] = DeriveJsonCodec.gen[Dt]
}
