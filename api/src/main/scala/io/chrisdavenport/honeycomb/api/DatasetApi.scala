package io.chrisdavenport.honeycomb.api

import org.http4s._
import org.http4s.implicits._
import org.http4s.client._
import cats.effect.kernel._

trait DatasetApi[F[_]]{
  def burnAlerts: BurnAlerts[F]
  def columns: Columns[F]
  def derivedColumns: DerivedColumns[F]
  def markers: Markers[F]
  def slos: SLOs[F]
}

object DatesetApi {

  def impl[F[_]: Concurrent](
    client: Client[F],
    apiKey: String,
    dataset: String,
    baseUri: Uri = uri"https://api.honeycomb.io/1"
  ): DatasetApi[F] = new DatasetApiImplementation(client, apiKey, dataset, baseUri)

  private class DatasetApiImplementation[F[_]: Concurrent](
    client: Client[F],
    apiKey: String,
    dataset: String,
    baseUri: Uri
  ) extends DatasetApi[F]{
    val burnAlerts: BurnAlerts[F] = BurnAlerts.impl(client, apiKey, dataset, baseUri)
    val columns: Columns[F] = Columns.impl(client, apiKey, dataset, baseUri)
    val derivedColumns: DerivedColumns[F] = 
      DerivedColumns.impl(client, apiKey, dataset, baseUri)

    val markers : Markers[F] =
      Markers.impl(client, apiKey, dataset, baseUri)

    val slos: SLOs[F] = SLOs.impl(client, apiKey, dataset, baseUri)
  }

}