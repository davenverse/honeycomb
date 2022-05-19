package io.chrisdavenport.honeycomb.api

import cats.syntax.all._
import cats.effect.kernel._
import org.http4s.client.Client
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import io.circe.Decoder
import io.circe.HCursor

trait Columns[F[_]]{
  import Columns._
  def getById(id: String): F[Option[Column]]
  def getByKeyName(keyname: String): F[Option[Column]]
  def list: F[List[Column]]

}
object Columns {

  def impl[F[_]: Concurrent](client: Client[F], apiKey: String, dataset: String, baseUri: Uri = uri"https://api.honeycomb.io/1"): Columns[F] = 
    new ColumnsImpl(Api.honeycombClient(client, apiKey), dataset, baseUri)

  private class ColumnsImpl[F[_]: Concurrent](
    client: Client[F],
    dataset: String,
    baseUri: Uri
  ) extends Columns[F]{
    def getById(id: String): F[Option[Column]] = 
      client.expectOption(Request[F](Method.GET, baseUri / "columns" / dataset / id))
    
    def getByKeyName(keyname: String): F[Option[Column]] = 
      client.expectOption(Request[F](Method.GET, (baseUri / "columns" / dataset).withQueryParam("key_name", keyname)))
    
    def list: F[List[Column]] = 
      client.expect(Request[F](Method.GET, baseUri / "columns" / dataset))
    
  }

  case class Column(id: String, keyName: String, hidden: Boolean, description: String, `type`: String)
  object Column {
    implicit val decoder: Decoder[Column] = new Decoder[Column]{
      def apply(c: HCursor): Decoder.Result[Column] =
        (
          c.downField("id").as[String],
          c.downField("key_name").as[String],
          c.downField("hidden").as[Boolean],
          c.downField("description").as[String],
          c.downField("type").as[String]
        ).mapN(Column.apply)
    }
  }
}