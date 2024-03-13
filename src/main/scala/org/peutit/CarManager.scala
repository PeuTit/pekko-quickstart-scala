package org.peutit

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor

class CarManager(context: ActorContext[CarManager.Command])
    extends AbstractBehavior[CarManager.Command](context) {
  import CarManager._

  private var makeToActor =
    scala.collection.mutable.Map.empty[String, ActorRef[CarGroup.Command]]

  override def onMessage(
      msg: CarManager.Command
  ): Behavior[CarManager.Command] =
    msg match {
      case trackMsg @ RequestTrackCar(make, _, _, replyTo) => {
        makeToActor.get(make) match {
          case Some(makeGroup) => makeGroup ! trackMsg
          case None =>
            context.log.info("🚓🚓 Creating Car Make Group Actor for {}", make)
            val makeActor: ActorRef[CarGroup.Command] =
              context.spawn(CarGroup(make), "group-" + make)
            context.watchWith(makeActor, CarGroupTerminated(make))
            makeActor ! trackMsg
            makeToActor += make -> makeActor
        }
        this
      }
      case trackListMsg @ RequestCarList(requestId, make, replyTo) => {
        makeToActor.get(make) match {
          case Some(makeGroup) => makeGroup ! trackListMsg
          case None => replyTo ! ReplyCarList(requestId, Set.empty[String])
        }
        this
      }
      case CarGroupTerminated(make) => {
        context.log.warn("🛑🛑 Car Make Group for {} has been terminated", make)
        makeToActor -= make
        this
      }
      case _ =>
        Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("🛑🛑 Car Make Manager Stopped")
      this
  }
}

object CarManager {
  def apply(): Behavior[CarManager.Command] =
    Behaviors.setup(new CarManager(_: ActorContext[CarManager.Command]))

  sealed trait Command

  final case class RequestTrackCar(
      make: String,
      model: String,
      vin: String,
      replyTo: ActorRef[CarRegistered]
  ) extends CarManager.Command
      with CarGroup.Command
  final case class CarRegistered(car: ActorRef[Car.Command])

  final case class RequestCarList(
      requestId: Long,
      make: String,
      replyTo: ActorRef[ReplyCarList]
  ) extends CarManager.Command
      with CarGroup.Command
  final case class ReplyCarList(requestId: Long, carModels: Set[String])

  final case class CarGroupTerminated(make: String) extends CarManager.Command

  final case class RequestAllSpeeds(
      requestId: Long,
      make: String,
      replyTo: ActorRef[RespondAllSpeeds]
  ) extends CarGroupQuery.Command
      with CarGroup.Command
      with CarManager.Command

  final case class RespondAllSpeeds(
      requestId: Long,
      speeds: Map[String, SpeedReading]
  )

  sealed trait SpeedReading

  final case class Speed(value: Double) extends SpeedReading
  case object SpeedNotAvailable extends SpeedReading
  case object CarNotAvailable extends SpeedReading
  case object CarTimedOut extends SpeedReading
}
