package org.peutit

import scala.concurrent.duration.DurationInt

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef

import Car.{SpeedRecorded, RecordSpeed, Stop}
import CarManager.{
  CarRegistered,
  RequestTrackCar,
  RequestCarList,
  ReplyCarList,
  Speed,
  RespondAllSpeeds,
  RequestAllSpeeds,
  SpeedNotAvailable
}

class CarGroupSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  "CarGroup Actor" should {
    "be able to register a Car Actor" in {
      val makeGroup: String = "Mercedes-Benz"

      val registeredProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val groupActor = spawn(CarGroup(makeGroup = makeGroup))

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = "Viano",
        vin = "lapapelmobile",
        registeredProbe.ref
      )
      val registered1: CarRegistered = registeredProbe.receiveMessage()
      val carActor1: ActorRef[Car.Command] = registered1.car

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = "G-class",
        vin = "benztruck",
        registeredProbe.ref
      )
      val registered2: CarRegistered = registeredProbe.receiveMessage()
      val carActor2: ActorRef[Car.Command] = registered2.car
      carActor1 shouldNot equal(carActor2)

      val recordProbe: TestProbe[SpeedRecorded] =
        createTestProbe[SpeedRecorded]()
      carActor1 ! RecordSpeed(requestId = 1, value = 160, recordProbe.ref)
      recordProbe.expectMessage(SpeedRecorded(requestId = 1))
      carActor2 ! RecordSpeed(requestId = 2, value = 250, recordProbe.ref)
      recordProbe.expectMessage(SpeedRecorded(requestId = 2))
    }

    "ignore request for wrong make group" in {
      val makeGroup: String = "Peugeot"
      val wrongMakeGroup: String = "BMW"

      val registeredProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val groupActor = spawn(CarGroup(makeGroup = makeGroup))

      groupActor ! RequestTrackCar(
        make = wrongMakeGroup,
        model = "Z3",
        vin = "beamerboy",
        registeredProbe.ref
      )
      registeredProbe.expectNoMessage(10.seconds)
    }

    "return same actor for same car model" in {
      val makeGroup: String = "Porsche"
      val model: String = "924"

      val registeredProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val groupActor: ActorRef[CarGroup.Command] =
        spawn(CarGroup(makeGroup = makeGroup))

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = model,
        vin = "needmoneyforporsche",
        registeredProbe.ref
      )
      val registered1: CarRegistered = registeredProbe.receiveMessage()

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = model,
        vin = "needmoneyforporsche",
        registeredProbe.ref
      )
      val registered2: CarRegistered = registeredProbe.receiveMessage()

      registered1.car shouldBe registered2.car
    }

    "be able to retrieve a list of Car models" in {
      val makeGroup: String = "AlfaRomeo"
      val model1: String = "Mito"
      val model2: String = "Giulia"
      val carModels: Set[String] = Set(model1, model2)
      val vin1 = "lavoituredugrossimonquimarchepas"
      val vin2 = "tresbellevoitureitalienne"

      val registeredProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val groupActor: ActorRef[CarGroup.Command] =
        spawn(CarGroup(makeGroup = makeGroup))

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = model1,
        vin = vin1,
        registeredProbe.ref
      )

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = model2,
        vin = vin2,
        registeredProbe.ref
      )

      val replyProbe: TestProbe[ReplyCarList] = createTestProbe[ReplyCarList]()
      groupActor ! RequestCarList(requestId = 0, makeGroup, replyProbe.ref)
      replyProbe.expectMessage(ReplyCarList(requestId = 0, carModels))
    }

    "ignore CarList request for wrong make group" in {
      val makeGroup: String = "Citroen"
      val wrongMakeGroup: String = "Alpine"

      val replyProbe: TestProbe[ReplyCarList] =
        createTestProbe[ReplyCarList]()
      val groupActor = spawn(CarGroup(makeGroup = makeGroup))

      groupActor ! RequestCarList(
        requestId = 0,
        make = wrongMakeGroup,
        replyProbe.ref
      )
      replyProbe.expectNoMessage(10.seconds)
    }

    "be able to stop a single car and retrieve the updated list" in {
      val makeGroup: String = "Fiat"
      val model1: String = "500"
      val model2: String = "Panda"
      val carModels: Set[String] = Set(model1, model2)
      val vin1 = "voituredepouffe"
      val vin2 = "jamesmayfavoritecar"

      val registeredProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val groupActor: ActorRef[CarGroup.Command] =
        spawn(CarGroup(makeGroup = makeGroup))

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = model1,
        vin = vin1,
        registeredProbe.ref
      )
      val registered1 = registeredProbe.receiveMessage()
      val car1 = registered1.car

      groupActor ! RequestTrackCar(
        make = makeGroup,
        model = model2,
        vin = vin2,
        registeredProbe.ref
      )
      val registered2 = registeredProbe.receiveMessage()

      val replyProbe: TestProbe[ReplyCarList] = createTestProbe[ReplyCarList]()
      groupActor ! RequestCarList(requestId = 0, makeGroup, replyProbe.ref)
      replyProbe.expectMessage(ReplyCarList(requestId = 0, carModels))

      car1 ! Stop
      registeredProbe.expectTerminated(car1, registeredProbe.remainingOrDefault)

      registeredProbe.awaitAssert {
        groupActor ! RequestCarList(requestId = 1, makeGroup, replyProbe.ref)
        replyProbe.expectMessage(ReplyCarList(requestId = 1, Set(model2)))
      }
    }

    "be able to collect speed readings from all cars" in {
      val makeGroup: String = "Alpine"
      val model1 = "A110-2017"
      val model2 = "A110s-2017"
      val model3 = "A110"

      val registerProbe: TestProbe[CarRegistered] =
        createTestProbe[CarRegistered]()
      val groupActor: ActorRef[CarGroup.Command] = spawn(CarGroup(makeGroup))

      groupActor ! RequestTrackCar(
        makeGroup,
        model1,
        "fastestfrenchyontheroad",
        registerProbe.ref
      )
      val carActor1 = registerProbe.receiveMessage().car

      groupActor ! RequestTrackCar(
        makeGroup,
        model2,
        "thisisthefastestfrenchyontheroad",
        registerProbe.ref
      )
      val carActor2 = registerProbe.receiveMessage().car

      groupActor ! RequestTrackCar(
        makeGroup,
        model3,
        "theoriginalfrenchy",
        registerProbe.ref
      )
      registerProbe.receiveMessage().car

      val recordProbe: TestProbe[SpeedRecorded] =
        createTestProbe[SpeedRecorded]()
      carActor1 ! RecordSpeed(requestId = 0, 1234, recordProbe.ref)
      recordProbe.expectMessage(SpeedRecorded(requestId = 0))

      carActor2 ! RecordSpeed(requestId = 1, 7890, recordProbe.ref)
      recordProbe.expectMessage(SpeedRecorded(requestId = 1))
      // No Speed Recorded for Car3

      val allSpeedProbe: TestProbe[RespondAllSpeeds] =
        createTestProbe[RespondAllSpeeds]()
      groupActor ! RequestAllSpeeds(
        requestId = 0,
        make = makeGroup,
        allSpeedProbe.ref
      )

      allSpeedProbe.expectMessage(
        RespondAllSpeeds(
          requestId = 0,
          speeds = Map(
            model1 -> Speed(1234),
            model2 -> Speed(7890),
            model3 -> SpeedNotAvailable
          )
        )
      )
    }
  }
}
