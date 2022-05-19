package io.chrisdavenport.honeycomb.api

import cats.syntax.all._
import cats.effect.kernel._
import org.http4s.client.Client
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import io.circe.Decoder
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import io.circe.syntax._

trait Markers[F[_]]{
  import Markers._
  def create(
    message: String,
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    `type`: Option[String] = None,
    url: Option[String] = None
  ): F[Marker]

  def update(
    id: String,
    message: Option[String],
    startTime: Option[Long] = None,
    endTime: Option[Long] = None,
    `type`: Option[String] = None,
    url: Option[String] = None
  ): F[Marker]

  def delete(id: String): F[Boolean]
  def list: F[List[Marker]]
}

object Markers {
  def impl[F[_]: Concurrent](client: Client[F], apiKey: String, dataset: String, baseUri: Uri = uri"https://api.honeycomb.io/1"): Markers[F] = {
    new MarkersImpl[F](Api.honeycombClient(client, apiKey), dataset, baseUri)
  }

  private class MarkersImpl[F[_]: Concurrent](
    client: Client[F],
    dataset: String,
    baseUri: Uri
  ) extends Markers[F]{
    def create(message: String, startTime: Option[Long], endTime: Option[Long], `type`: Option[String], url: Option[String]): F[Marker] = 
      client.expect(Request[F](Method.POST, baseUri / "markers" / dataset).withEntity(MarkerModify(message.some, startTime, endTime, `type`, url)))
    
    def update(id: String, message: Option[String], startTime: Option[Long], endTime: Option[Long], `type`: Option[String], url: Option[String]): F[Marker] = 
      client.expect(Request[F](Method.PUT, baseUri / "markers" / dataset / id))
    
    def delete(id: String): F[Boolean] = 
      client.successful(Request[F](Method.DELETE, baseUri / "markers" / dataset / id))
    
    def list: F[List[Marker]] = 
      client.expect(Request[F](Method.GET, baseUri / "markers" / dataset))
    
  }

  private case class MarkerModify(
    message: Option[String],
    startTime: Option[Long],
    endTime: Option[Long],
    `type`: Option[String],
    url: Option[String]
  )
  private object MarkerModify {
    implicit val encoder: Encoder[MarkerModify] = new Encoder[MarkerModify]{
      def apply(a: MarkerModify): Json = Json.obj(
        "message" -> a.message.asJson,
        "start_time" -> a.startTime.asJson,
        "end_time" -> a.endTime.asJson,
        "type" -> a.`type`.asJson,
        "url" -> a.url.asJson
      ).dropNullValues
    }
  }

  case class Marker(
    id: String,
    message: String,
    startTime: Long,
    endTime: Option[Long],
    `type`: Option[String],
    url: Option[String],
    createdAt: String,
    updatedAt: String,
    color: Option[String]
  )
  object Marker {
    implicit val decoder: Decoder[Marker] = new Decoder[Marker]{
      def apply(c: HCursor): Decoder.Result[Marker] = (
        c.downField("id").as[String],
        c.downField("message").as[String],
        c.downField("start_time").as[Long],
        c.downField("end_time").as[Option[Long]],
        c.downField("type").as[Option[String]],
        c.downField("url").as[Option[String]],
        c.downField("created_at").as[String],
        c.downField("updated_at").as[String],
        c.downField("color").as[Option[String]]
      ).mapN(Marker.apply)
    }
  }
}