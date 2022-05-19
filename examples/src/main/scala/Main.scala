package io.chrisdavenport.honeycomb

import cats.syntax.all._
import cats.effect._
import io.chrisdavenport.honeycomb.api.Api

object Main extends IOApp {

  // Remember to set your HONEYCOMB_TOKEN environment variable
  def run(args: List[String]): IO[ExitCode] = Api.default[IO]().use{ api =>
    api.datasetApi("production").derivedColumns
      .list
      .flatMap(_.traverse(IO.println))
      .as(ExitCode.Success)
  }

}