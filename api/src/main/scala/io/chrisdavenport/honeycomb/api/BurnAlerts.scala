package io.chrisdavenport.honeycomb.api

import cats.syntax.all._
import cats.effect.kernel._
import org.http4s.client.Client
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import io.circe.Decoder
import io.circe.HCursor
import io.circe.Encoder
import io.circe.syntax._
import io.circe.Json

trait BurnAlerts[F[_]]{
  import BurnAlerts._
  def create(sloId: String, exhaustionMinutes: Int, recipient: Option[List[Recipient]]): F[BurnAlert]
  def modify(id: String, exhaustionMinutes: Int, recipient: Option[List[Recipient]]): F[BurnAlert]
  def delete(id: String): F[Boolean]
  def get(id: String): F[Option[BurnAlert]]
  def getAllForSLO(sloId: String): F[List[BurnAlert]]
}

object BurnAlerts {

  def impl[F[_]: Concurrent](
    client: Client[F],
    apiKey: String, 
    dataset: String,
    baseUri: Uri = uri"https://api.honeycomb.io"
  ): BurnAlerts[F] = new BurnAlertsImpl[F](Api.honeycombClient(client, apiKey), dataset, baseUri)

  private class BurnAlertsImpl[F[_]: Concurrent](
    client: Client[F],
    dataset: String,
    baseUri: Uri
  ) extends BurnAlerts[F]{
    def create(sloId: String, exhaustionMinutes: Int, recipients: Option[List[Recipient]]): F[BurnAlert] = 
      client.expectOr(Request[F](Method.GET, baseUri / "1" / "burn_alerts" / dataset).withEntity(ModifyBurnAlert(sloId, exhaustionMinutes, recipients)))(_.as[BurnAlertError].widen)

    def delete(id: String): F[Boolean] = 
      client.successful(Request[F](Method.DELETE, baseUri / "1" / "burn_alerts" / dataset / id))

    
    def modify(id: String, exhaustionMinutes: Int, recipients: Option[List[Recipient]]): F[BurnAlert] = 
      client.expectOr(Request[F](Method.PUT, baseUri / "1" / "burn_alerts" / dataset / id).withEntity(Json.obj("exhaustion_minutes" -> exhaustionMinutes.asJson, "recipients" -> recipients.asJson)))(_.as[BurnAlertError].widen)
    
    def get(id: String): F[Option[BurnAlert]] = 
      client.expectOption(Request[F](Method.GET, baseUri / "1" / "burn_alerts" / dataset / id))
    
    def getAllForSLO(sloId: String): F[List[BurnAlert]] = 
      client.expectOr(Request[F](Method.GET, (baseUri / "1" / "burn_alerts" / dataset).withQueryParam("slo_id", sloId)))(_.as[BurnAlertError].widen)
    
  }

  case class Recipient(`type`: String, target: String, id: String)
  object Recipient {
    implicit val codec: io.circe.Codec[Recipient] = io.circe.generic.semiauto.deriveCodec
  }

  case class BurnAlert(id: String, exhaustionMinutes: Int, createdAt: String, updatedAt: String, sloId: String, recipients: Option[List[Recipient]])
  object BurnAlert {
    implicit val decoder: Decoder[BurnAlert] = new Decoder[BurnAlert]{
      def apply(c: HCursor): Decoder.Result[BurnAlert] = (
        c.downField("id").as[String],
        c.downField("exhaustion_minutes").as[Int],
        c.downField("created_at").as[String],
        c.downField("updated_at").as[String],
        c.downField("slo").downField("id").as[String],
        c.downField("recipients").as[Option[List[Recipient]]]
      ).mapN(BurnAlert.apply)
    }
  }

  private case class ModifyBurnAlert(sloId: String, exhaustionMinutes: Int, recipients: Option[List[Recipient]])
  private object ModifyBurnAlert {
    implicit val encoder: Encoder[ModifyBurnAlert] = new Encoder[ModifyBurnAlert]{
      def apply(a: ModifyBurnAlert): Json = Json.obj(
        "slo" -> Json.obj(
          "id" -> a.sloId.asJson
        ),
        "exhaustion_minutes" -> a.exhaustionMinutes.asJson,
        "recipients" -> a.recipients.asJson
      )
    }
  }


  case class ErrorDetail(field: String, code: String, description: String)
  object ErrorDetail{ 
    implicit val decoder: Decoder[ErrorDetail] = io.circe.generic.semiauto.deriveDecoder
  }

  case class BurnAlertError(status: Int, `type`: String, title: String, typeDetail: List[ErrorDetail], error: String)
    extends RuntimeException(s"Failed To Create BurnAlert, status: $status type: ${`type`} title: $title typeDetail: $typeDetail error: $error")
  object BurnAlertError{
    implicit val decoder: Decoder[BurnAlertError] = new Decoder[BurnAlertError]{
      def apply(c: HCursor): Decoder.Result[BurnAlertError] = 
        (
          c.downField("status").as[Int],
          c.downField("type").as[String],
          c.downField("title").as[String],
          c.downField("type_detail").as[List[ErrorDetail]],
          c.downField("error").as[String]
        ).mapN(BurnAlertError.apply)
    }
  }
}