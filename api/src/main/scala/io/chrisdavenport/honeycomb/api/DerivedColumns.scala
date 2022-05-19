package io.chrisdavenport.honeycomb.api

import cats.effect.kernel._
import org.http4s.client.Client
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import io.circe.Encoder
import io.circe.Decoder

trait DerivedColumns[F[_]]{
  import DerivedColumns._
  def create(alias: String, description: String, expression: String): F[DerivedColumn]
  def update(id: String, alias: String, description: String, expression: String): F[DerivedColumn]
  def delete(id: String): F[Boolean]
  def getById(id: String): F[Option[DerivedColumn]]
  def getByAlias(alias: String): F[Option[DerivedColumn]]
  def list: F[List[DerivedColumn]]
}

object DerivedColumns{

  def impl[F[_]: Concurrent](client: Client[F], apiKey: String, dataset: String, baseUri: Uri = uri"https://api.honeycomb.io/1"): DerivedColumns[F] = 
    new DerivedColumnsImpl(Api.honeycombClient(client, apiKey), dataset, baseUri)

  private class DerivedColumnsImpl[F[_]: Concurrent](
    client: Client[F],
    dataset: String,
    baseUri: Uri
  ) extends DerivedColumns[F]{
    def create(alias: String, description: String, expression: String): F[DerivedColumn] = 
      client.expect(Request[F](Method.POST, baseUri / "derived_columns" / dataset).withEntity(ModifyDerivedColumn(alias, description, expression)))
    
    def update(id: String, alias: String, description: String, expression: String): F[DerivedColumn] = 
      client.expect(Request[F](Method.PUT, baseUri / "derived_columns" / dataset / id).withEntity(ModifyDerivedColumn(alias, description, expression)))
    
    def delete(id: String): F[Boolean] = 
      client.successful(Request[F](Method.DELETE, baseUri / "derived_columns" / dataset / id))
    
    def getById(id: String): F[Option[DerivedColumn]] = 
      client.expectOption(Request[F](Method.GET, baseUri / "derived_columns" / dataset / id))
    
    def getByAlias(alias: String): F[Option[DerivedColumn]] = 
      client.expectOption(Request[F](Method.GET, (baseUri / "derived_columns" / dataset).withQueryParam("alias", alias)))
    
    def list: F[List[DerivedColumn]] = 
      client.expect(Request[F](Method.GET, baseUri / "derived_columns" / dataset))
  }

  case class ModifyDerivedColumn(alias: String, description: String, expression: String)
  object ModifyDerivedColumn {
    implicit val encoder: Encoder[ModifyDerivedColumn] = io.circe.generic.semiauto.deriveEncoder
  }

  case class DerivedColumn(id: String, alias: String, description: String, expression: String)
  object DerivedColumn {
    implicit val decoder: Decoder[DerivedColumn] = io.circe.generic.semiauto.deriveDecoder
  }



}