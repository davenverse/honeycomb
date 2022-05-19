package io.chrisdavenport.honeycomb.api

import org.http4s._
import org.http4s.implicits._
import org.http4s.client._
import cats.effect.kernel._

trait Api[F[_]]{
  def datasets: Datasets[F]
  def datasetApi(dataset: String): DatasetApi[F]
}
object Api {

  def impl[F[_]: Concurrent](
    client: Client[F],
    apiKey: String,
    baseUri: Uri = uri"https://api.honeycomb.io/1"
  ): Api[F] = new ApiImplementation[F](client, apiKey, baseUri)

  private class ApiImplementation[F[_]: Concurrent](
    client: Client[F],
    apiKey: String,
    baseUri: Uri
  ) extends Api[F]{

    def datasets: Datasets[F] =
      Datasets.impl(client, apiKey, baseUri)
    def datasetApi(dataset: String): DatasetApi[F] = 
      DatesetApi.impl(client, apiKey, dataset, baseUri)
  }

  private[api] def honeycombClient[F[_]: MonadCancelThrow](client: Client[F], apiKey: String): Client[F] = 
    Client{(request: Request[F]) => client.run(request.putHeaders("X-Honeycomb-Team" -> apiKey))}
}