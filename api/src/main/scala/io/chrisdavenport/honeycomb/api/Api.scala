package io.chrisdavenport.honeycomb.api

import cats._
import cats.syntax.all._
import org.http4s._
import org.http4s.implicits._
import org.http4s.client._
import cats.effect.kernel._

import org.http4s.ember.client.EmberClientBuilder
import io.chrisdavenport.env.Env
import javax.naming.ConfigurationException

trait Api[F[_]]{
  def datasets: Datasets[F]
  def datasetApi(dataset: String): DatasetApi[F]
}
object Api {

  /**
    * A default API Implementation. Taking care of the pesky configurations.
    *
    * @param apiKey An override for the apiKey, by default attempt to use HONEYCOMB_TOKEN, failing if not present.
    * @param baseUri The base uri of the honeycomb api.
    * @return A Resource of an API that can be used for interacting with Honeycomb.io
    */
  def default[F[_]: Async](
    apiKey: Option[String] = None,
    baseUri: Uri = uri"https://api.honeycomb.io"
  ): Resource[F, Api[F]] = {
    implicit val env: Env[F] = Env.make[F]
    apiKey.fold(Resource.eval(getHoneycombToken[F]))(_.pure[Resource[F, *]]).flatMap(apiKey => 
      EmberClientBuilder.default[F]
        .withUserAgent(org.http4s.headers.`User-Agent`(ProductId("honeycomb-api-scala", "0.0.1".some)))
        .build
        .map(impl(_, apiKey, baseUri))
    )
  }

  private def getHoneycombToken[F[_]: Env: MonadThrow]: F[String] = 
    Env[F].get("HONEYCOMB_TOKEN").flatMap{
      case Some(a) => a.pure[F]
      case None => new ConfigurationException("Missing Env Variable - HONEYCOMB_TOKEN").raiseError
  }

  def impl[F[_]: Concurrent](
    client: Client[F],
    apiKey: String,
    baseUri: Uri = uri"https://api.honeycomb.io"
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