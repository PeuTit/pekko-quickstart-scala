package org.peutit

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.typed.PostStop

class Car(context: ActorContext[Car.Command])(
    make: String,
    model: String,
    vin: String
) extends AbstractBehavior[Car.Command](context) {
  import Car._

  context.log.info("🚗🚗 {} {} Started With Vin: {}", make, model, vin)

  var speedMeasure: Option[Double] = None

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case RecordSpeed(requestId, value, replyTo) =>
        context.log.info(
          "🏎️💨💨Speed Recorded: {} with id: {}",
          value,
          requestId
        )
        replyTo ! SpeedRecorded(requestId)
        speedMeasure = Some(value)
        this

      case ReadSpeed(requestId, replyTo) =>
        replyTo ! RespondSpeed(requestId, speedMeasure)
        this

      case Stop =>
        Behaviors.stopped

      case _ =>
        Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("🏎️🛑{} {} Stopped With Vin: {}", make, model, vin)
      this
  }
}

object Car {
  def apply(make: String, model: String, vin: String): Behavior[Car.Command] =
    Behaviors.setup[Car.Command](
      new Car(_: ActorContext[Car.Command])(make, model, vin)
    )

  sealed trait Command
  final case class ReadSpeed(requestId: Long, replyTo: ActorRef[RespondSpeed])
      extends Car.Command
  final case class RespondSpeed(requestId: Long, value: Option[Double])

  final case class RecordSpeed(
      requestId: Long,
      value: Double,
      replyTo: ActorRef[SpeedRecorded]
  ) extends Car.Command
  final case class SpeedRecorded(requestId: Long)

  final object Stop extends Car.Command
}
