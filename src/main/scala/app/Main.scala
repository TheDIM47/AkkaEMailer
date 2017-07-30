package app

import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import mq.RMQActor

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {
  def main(args: Array[String]): Unit = {
    val conf = ConfigFactory.load("application.conf")

    implicit val system = ActorSystem("akka-emailer")

    system.actorOf(RMQActor.props(conf), name = "rmq-listener")

    Await.ready(system.whenTerminated, Duration.Inf)
  }
}

