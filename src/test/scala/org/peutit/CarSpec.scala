package org.peutit

import org.apache.pekko.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.wordspec.AnyWordSpecLike
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.ActorRef

class CarSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import Car._

  "Car Actor" should {
    "reply with an empty measurement when speed is not known" in {
      val readProbe: TestProbe[RespondSpeed] = createTestProbe[RespondSpeed]()
      val carActor: ActorRef[Command] =
        spawn(Car("Renault", "Clio", "laclio2blanchedematheo"))

      carActor ! ReadSpeed(requestId = 1, readProbe.ref)
      val response: RespondSpeed = readProbe.receiveMessage()
      response.requestId shouldBe 1
      response.value shouldBe None
    }

    "reply with correct value when speed is recorded" in {
      val speedValue: Double = 190.00
      val otherSpeedValue: Double = 75.00

      val recordProbe: TestProbe[SpeedRecorded] =
        createTestProbe[SpeedRecorded]()
      val readProbe: TestProbe[RespondSpeed] = createTestProbe[RespondSpeed]()
      val carActor: ActorRef[Command] =
        spawn(Car("Saab", "93", "lasaabdugrossachalefauxgaucho"))

      carActor ! RecordSpeed(
        requestId = 2,
        value = speedValue,
        recordProbe.ref
      )
      val recordReponse: SpeedRecorded =
        recordProbe.expectMessage(SpeedRecorded(2))
      recordReponse.requestId shouldBe 2

      carActor ! ReadSpeed(requestId = 3, readProbe.ref)
      val readResponse: RespondSpeed = readProbe.receiveMessage()
      readResponse.requestId shouldBe 3
      readResponse.value shouldBe Some(speedValue)

      carActor ! RecordSpeed(
        requestId = 4,
        value = otherSpeedValue,
        recordProbe.ref
      )
      val secondRecordReponse: SpeedRecorded =
        recordProbe.expectMessage(SpeedRecorded(4))
      secondRecordReponse.requestId shouldBe 4

      carActor ! ReadSpeed(requestId = 5, readProbe.ref)
      val secondReadResponse: RespondSpeed = readProbe.receiveMessage()
      secondReadResponse.requestId shouldBe 5
      secondReadResponse.value shouldBe Some(otherSpeedValue)
    }
  }
}
