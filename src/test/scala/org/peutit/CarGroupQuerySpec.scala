package org.peutit

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef
import scala.concurrent.duration.DurationInt
import org.scalatest.Assertions

class CarGroupQuerySpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import CarGroupQuery._

  "CarGroupQuery" should {
    "return speed value for running cars" in {
      val requester: TestProbe[CarManager.RespondAllSpeeds] =
        createTestProbe[CarManager.RespondAllSpeeds]()

      val car1: TestProbe[Car.Command] = createTestProbe[Car.Command]()
      val car2: TestProbe[Car.Command] = createTestProbe[Car.Command]()

      val carModelToActor: Map[String, ActorRef[Car.Command]] =
        Map("car1" -> car1.ref, "car2" -> car2.ref)

      val queryActor: ActorRef[Command] =
        spawn(
          CarGroupQuery(
            carModelToActor,
            requestId = 1,
            requester = requester.ref,
            timeout = 3.seconds
          )
        )

      car1.expectMessageType[Car.ReadSpeed]
      car2.expectMessageType[Car.ReadSpeed]

      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(1.0), "car1")
      )
      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(2.0), "car2")
      )

      requester.expectMessage(
        CarManager.RespondAllSpeeds(
          requestId = 1,
          speeds = Map(
            "car1" -> CarManager.Speed(1.0),
            "car2" -> CarManager.Speed(2.0)
          )
        )
      )
    }

    "return SpeedNotAvailable for car with no readings" in {
      val requester: TestProbe[CarManager.RespondAllSpeeds] =
        createTestProbe[CarManager.RespondAllSpeeds]()

      val car1: TestProbe[Car.Command] = createTestProbe[Car.Command]()
      val car2: TestProbe[Car.Command] = createTestProbe[Car.Command]()

      val carModelToActor: Map[String, ActorRef[Car.Command]] =
        Map("car1" -> car1.ref, "car2" -> car2.ref)

      val queryActor = spawn(
        CarGroupQuery(
          carModelToActor,
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds
        )
      )

      car1.expectMessageType[Car.ReadSpeed]
      car2.expectMessageType[Car.ReadSpeed]

      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, None, "car1")
      )
      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(2.0), "car2")
      )

      requester.expectMessage(
        CarManager.RespondAllSpeeds(
          requestId = 1,
          speeds = Map(
            "car1" -> CarManager.SpeedNotAvailable,
            "car2" -> CarManager.Speed(2.0)
          )
        )
      )
    }

    "return CarNotAvailable for car that stopped before answering" in {
      val requester: TestProbe[CarManager.RespondAllSpeeds] =
        createTestProbe[CarManager.RespondAllSpeeds]()

      val car1: TestProbe[Car.Command] = createTestProbe[Car.Command]()
      val car2: TestProbe[Car.Command] = createTestProbe[Car.Command]()

      val carModelToActor: Map[String, ActorRef[Car.Command]] =
        Map("car1" -> car1.ref, "car2" -> car2.ref)

      val queryActor = spawn(
        CarGroupQuery(
          carModelToActor,
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds
        )
      )

      car1.expectMessageType[Car.ReadSpeed]
      car2.expectMessageType[Car.ReadSpeed]

      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(1234), "car1")
      )
      car2.stop()

      requester.expectMessage(
        CarManager.RespondAllSpeeds(
          requestId = 1,
          speeds = Map(
            "car1" -> CarManager.Speed(1234),
            "car2" -> CarManager.CarNotAvailable
          )
        )
      )
    }

    "return Speed reading even if car stops after answering" in {
      val requester: TestProbe[CarManager.RespondAllSpeeds] =
        createTestProbe[CarManager.RespondAllSpeeds]()

      val car1: TestProbe[Car.Command] = createTestProbe[Car.Command]()
      val car2: TestProbe[Car.Command] = createTestProbe[Car.Command]()

      val carModelToActor: Map[String, ActorRef[Car.Command]] =
        Map("car1" -> car1.ref, "car2" -> car2.ref)

      val queryActor = spawn(
        CarGroupQuery(
          carModelToActor,
          requestId = 1,
          requester = requester.ref,
          timeout = 3.seconds
        )
      )

      car1.expectMessageType[Car.ReadSpeed]
      car2.expectMessageType[Car.ReadSpeed]

      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(1234), "car1")
      )
      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(7890), "car2")
      )
      car2.stop()

      requester.expectMessage(
        CarManager.RespondAllSpeeds(
          requestId = 1,
          speeds = Map(
            "car1" -> CarManager.Speed(1234),
            "car2" -> CarManager.Speed(7890)
          )
        )
      )
    }

    "return CarTimedOut if car does not answer in time" in {
      val requester: TestProbe[CarManager.RespondAllSpeeds] =
        createTestProbe[CarManager.RespondAllSpeeds]()

      val car1: TestProbe[Car.Command] = createTestProbe[Car.Command]()
      val car2: TestProbe[Car.Command] = createTestProbe[Car.Command]()

      val carModelToActor: Map[String, ActorRef[Car.Command]] =
        Map("car1" -> car1.ref, "car2" -> car2.ref)

      val queryActor = spawn(
        CarGroupQuery(
          carModelToActor,
          requestId = 1,
          requester = requester.ref,
          timeout = 200.milliseconds
        )
      )

      car1.expectMessageType[Car.ReadSpeed]
      car2.expectMessageType[Car.ReadSpeed]

      queryActor ! WrappedRespondSpeed(
        Car.RespondSpeed(requestId = 0, Some(1.0), "car1")
      )
      // No Reply From Car2

      requester.expectMessage(
        CarManager.RespondAllSpeeds(
          requestId = 1,
          speeds = Map(
            "car1" -> CarManager.Speed(1.0),
            "car2" -> CarManager.CarTimedOut
          )
        )
      )
    }
  }
}
