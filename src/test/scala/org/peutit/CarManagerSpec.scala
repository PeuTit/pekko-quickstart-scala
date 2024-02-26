package org.peutit

import org.scalatest.wordspec.AnyWordSpecLike
import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit

import CarManager.{CarRegistered, RequestTrackCar, CarGroupTerminated}
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef

class CarManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "CarManager" should {
    "be able to register a Car Maker Group Actor" in {
      val makeGroup1: String = "Maserati"
      val makeGroup2: String = "VolksWagen"

      val registeredProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val managerActor: ActorRef[CarManager.Command] = spawn(CarManager())

      managerActor ! RequestTrackCar(
        make = makeGroup1,
        model = "Ghibli",
        vin = "coollookingcar",
        registeredProbe.ref
      )
      val registered1: CarRegistered = registeredProbe.receiveMessage()

      managerActor ! RequestTrackCar(
        make = makeGroup2,
        model = "Golf",
        vin = "kidsthatthinktheyareadultscar",
        registeredProbe.ref
      )
      val registered2: CarRegistered = registeredProbe.receiveMessage()

      managerActor ! CarGroupTerminated(makeGroup1)
      managerActor ! CarGroupTerminated(makeGroup2)
    }
  }
}
