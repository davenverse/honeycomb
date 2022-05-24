package io.chrisdavenport.honeycomb

import cats.syntax.all._
import cats.effect._
import io.chrisdavenport.honeycomb.api.Api

object Main extends IOApp {

  // Remember to set your HONEYCOMB_TOKEN environment variable
  def run(args: List[String]): IO[ExitCode] = Api.default[IO]().use{ api =>
    val ds = api.datasetApi("production")
    val _ = ds
    // GET Id for notifications
    // Create Derived Column
    // Create SLO
    // Create Burn Alerts for SLO with notification id
    IO.unit
      .as(ExitCode.Success)
  }

}