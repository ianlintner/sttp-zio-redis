package services

import zio.{ULayer, ZLayer}
import zio.redis.CodecSupplier
import zio.schema.Schema
import zio.schema.codec.{BinaryCodec, JsonCodec}

object JsonCodecSupplier extends CodecSupplier {
  def get[A: Schema]: BinaryCodec[A] = JsonCodec.schemaBasedBinaryCodec

  def layer: ULayer[JsonCodecSupplier.type] = ZLayer.succeed(JsonCodecSupplier)
}
