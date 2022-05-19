package io.chrisdavenport.honeycomb.api

import cats.syntax.all._
import cats.effect.kernel._
import org.http4s.client.Client
import org.http4s._
import org.http4s.implicits._

import org.http4s.circe.CirceEntityCodec._
import io.circe.Encoder
import io.circe.Decoder
import io.circe.Json
import io.circe.syntax._
import io.circe.HCursor

trait SLOs[F[_]]{
  import SLOs._
  def create(name: String, description: String, timePeriodDays: Int, targetPerMillion: Int, alias: String): F[SLO]
  def update(id: String, name: String, description: String, timePeriodDays: Int, targetPerMillion: Int, alias: String): F[SLO]
  def get(id: String): F[Option[SLO]]
  def delete(id: String): F[Boolean]
  def list: F[List[SLO]]
}
object SLOs {

  def impl[F[_]: Concurrent](client: Client[F], apiKey: String, dataset: String, baseUri: Uri = uri"https://api.honeycomb.io/1"): SLOs[F] = 
    new SLOsImpl(Api.honeycombClient(client, apiKey), dataset, baseUri)

  private class SLOsImpl[F[_]: Concurrent](
    client: Client[F],
    dataset: String,
    baseUri: Uri
  ) extends SLOs[F]{
    def create(name: String, description: String, timePeriodDays: Int, targetPerMillion: Int, alias: String): F[SLO] = 
      client.expectOr(Request[F](Method.POST, baseUri / "slos" / dataset).withEntity(ModifySLO(name, description, timePeriodDays, targetPerMillion, alias)))(resp => resp.as[CreationError].widen)
    
    def update(id: String, name: String, description: String, timePeriodDays: Int, targetPerMillion: Int, alias: String): F[SLO] = 
      client.expectOr(Request[F](Method.PUT, baseUri / "slos" / dataset / id).withEntity(ModifySLO(name, description, timePeriodDays, targetPerMillion, alias)))(resp => resp.as[CreationError].widen)

    def get(id: String): F[Option[SLO]] = 
      client.expectOption[SLO](Request[F](Method.GET, baseUri / "slos" / dataset / id))
    
    def delete(id: String): F[Boolean] = 
      client.successful(Request[F](Method.DELETE, baseUri / "slos" / dataset / id))
    
    def list: F[List[SLO]] = 
      client.expect(Request[F](Method.GET, baseUri / "slos" / "dataset"))
  }

  case class ModifySLO(name: String, description: String, timePeriodDays: Int, targetPerMillion: Int, alias: String)
  object ModifySLO {
    implicit val encoder: Encoder[ModifySLO] = new Encoder[ModifySLO]{
      def apply(a: ModifySLO): Json = Json.obj(
        "name" -> a.name.asJson,
        "description" -> a.description.asJson,
        "time_period_days" -> a.timePeriodDays.asJson,
        "target_per_million" -> a.targetPerMillion.asJson,
        "sli" -> Json.obj(
          "alias" -> a.alias.asJson
        )
      )
    }
  }

  case class SLO(id: String, name: String, description: String, timePeriodDays: Int, targetPerMillion: Int, alias: String, expression: String, createdAt: String, updatedAt: String)
  object SLO {
    implicit val decoder: Decoder[SLO] = new Decoder[SLO]{
      def apply(c: HCursor): Decoder.Result[SLO] = 
        (
          c.downField("id").as[String],
          c.downField("name").as[String],
          c.downField("description").as[String],
          c.downField("time_period_days").as[Int],
          c.downField("target_per_million").as[Int],
          c.downField("sli").downField("alias").as[String],
          c.downField("sli").downField("expression").as[String],
          c.downField("created_at").as[String], // ISO 8601
          c.downField("updated_at").as[String]
        ).mapN(SLO.apply)
    }
  }

  case class ErrorDetail(field: String, code: String, description: String)
  object ErrorDetail{ implicit val decoder: Decoder[ErrorDetail] = io.circe.generic.semiauto.deriveDecoder}

  case class CreationError(status: Int, `type`: String, title: String, typeDetail: List[ErrorDetail], error: String)
    extends RuntimeException(s"Failed To Create SLO, status: $status type: ${`type`} title: $title typeDetail: $typeDetail error: $error")
  object CreationError{
    implicit val decoder: Decoder[CreationError] = new Decoder[CreationError]{
      def apply(c: HCursor): Decoder.Result[CreationError] = 
        (
          c.downField("status").as[Int],
          c.downField("type").as[String],
          c.downField("title").as[String],
          c.downField("type_detail").as[List[ErrorDetail]],
          c.downField("error").as[String]
        ).mapN(CreationError.apply)
    }
  }
}