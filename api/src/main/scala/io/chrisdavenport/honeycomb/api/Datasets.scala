package io.chrisdavenport.honeycomb.api

import cats.effect.kernel._
import org.http4s.client.Client
import org.http4s._
import org.http4s.implicits._
import org.http4s.circe.CirceEntityCodec._
import io.circe.Decoder
import io.circe.Json
import io.circe.syntax._


trait Datasets[F[_]]{
  import Datasets._
  def create(name: String): F[Dataset]
  def get(dataset: String): F[Option[Dataset]]
  def list: F[List[Dataset]]
}
object Datasets {

  def impl[F[_]: Concurrent](client: Client[F], apiKey: String, baseUri: Uri = uri"https://api.honeycomb.io"): Datasets[F] = 
    new DatasetsImpl(Api.honeycombClient(client, apiKey), baseUri)

  private class DatasetsImpl[F[_]: Concurrent](
    client: Client[F],
    baseUri: Uri
  ) extends Datasets[F]{
    def create(name: String): F[Dataset] =
      client.expect[Dataset](Request[F](Method.POST, baseUri / "1" / "datasets").withEntity(Json.obj("name" -> name.asJson)))
    
    def get(dataset: String): F[Option[Dataset]] = 
      client.expectOption(Request[F](Method.GET, baseUri / "1" / "datasets" / dataset))
    
    def list: F[List[Dataset]] = 
      client.expect(Request[F](Method.GET, baseUri / "1" / "datasets"))
    
  }

  case class Dataset(name: String, slug: String)
  object Dataset {
    implicit val decoder: Decoder[Dataset] = io.circe.generic.semiauto.deriveDecoder
  }
}