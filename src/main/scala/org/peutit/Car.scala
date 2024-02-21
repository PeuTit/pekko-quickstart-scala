package org.peutit

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.typed.PostStop

object Car {
  def apply(make: String, model: String, vin: String): Behavior[Command] =
    Behaviors.setup[Command](context => new Car(context, make, model, vin))

  sealed trait Command
  final case class ReadSpeed(requestId: Long, replyTo: ActorRef[RespondSpeed])
      extends Command
  final case class RespondSpeed(requestId: Long, value: Option[Double])

  final case class RecordSpeed(
      requestId: Long,
      value: Double,
      replyTo: ActorRef[SpeedRecorded]
  ) extends Command
  final case class SpeedRecorded(requestId: Long)
}

class Car(
    context: ActorContext[Car.Command],
    make: String,
    model: String,
    vin: String
) extends AbstractBehavior[Car.Command](context) {
  import Car._

  context.log.info("{} {} Started With Vin: {}", make, model, vin)

  val speedMeasurements: Seq[Option[Double]] = Seq.empty[Option[Double]]
  def lastSpeedMeasurement(): Option[Double] = if (speedMeasurements.isEmpty) {
    None
  } else { speedMeasurements.last }

  var speedMeasure: Option[Double] = None

  override def onMessage(msg: Command): Behavior[Command] =
    msg match {
      case RecordSpeed(requestId, value, replyTo) =>
        context.log.info("Speed Recorded: {} with id: {}", value, requestId)
        replyTo ! SpeedRecorded(requestId)
        speedMeasure = Some(value)
        this
      case ReadSpeed(requestId, replyTo) =>
        replyTo ! RespondSpeed(requestId, speedMeasure)
        this
      case _ =>
        Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("{} {} Stopped With Vin: {}", make, model, vin)
      this
  }
}
