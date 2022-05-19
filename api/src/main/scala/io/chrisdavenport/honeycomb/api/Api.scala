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
import cats.effect.IO

trait Api[F[_]]{
  def datasets: Datasets[F]
  def datasetApi(dataset: String): DatasetApi[F]
}
object Api {

  lazy val global: Api[IO] = default[IO]().allocated.map(_._1).unsafeRunSync()(cats.effect.unsafe.implicits.global)

  def default[F[_]: Async](
    apiKey: Option[String] = None,
    baseUri: Uri = uri"https://api.honeycomb.io/1"
  ): Resource[F, Api[F]] = {
    implicit val env: Env[F] = Env.make[F]
    apiKey.fold(Resource.eval(getHoneycombToken[F]))(_.pure[Resource[F, *]]).flatMap(apiKey => 
      EmberClientBuilder.default[F]
        .withHttp2
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