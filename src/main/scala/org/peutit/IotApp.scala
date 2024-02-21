package org.peutit

import org.apache.pekko.actor.typed.ActorSystem

object IotApp {
  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](IotSupervisor(), "iot-system")
  }
}
