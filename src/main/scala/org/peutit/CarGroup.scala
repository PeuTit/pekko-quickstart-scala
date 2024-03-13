package org.peutit

import scala.concurrent.duration.DurationInt

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.typed.PostStop

class CarGroup(context: ActorContext[CarGroup.Command])(makeGroup: String)
    extends AbstractBehavior[CarGroup.Command](context) {
  import CarGroup._
  import CarManager.{
    RequestTrackCar,
    CarRegistered,
    RequestCarList,
    ReplyCarList,
    RequestAllSpeeds
  }

  context.log.info("🚥🚥 CarGroup {} Started", makeGroup)

  private var carModelToActor = Map.empty[String, ActorRef[Car.Command]]

  override def onMessage(msg: Command): Behavior[Command] = {
    msg match {
      case trackMsg @ RequestTrackCar(`makeGroup`, model, vin, replyTo) => {
        carModelToActor.get(model) match {
          case Some(carActor: ActorRef[Car.Command]) =>
            replyTo ! CarRegistered(carActor)
          case None =>
            context.log.info(
              "🚙🚙 Creating Car Actor for {} {} with vin: {}",
              trackMsg.make,
              trackMsg.model,
              trackMsg.vin
            )
            val carActor: ActorRef[Car.Command] =
              context.spawn(
                Car(makeGroup, model, vin),
                s"$makeGroup-$model-$vin"
              )
            context.watchWith(
              carActor,
              CarTerminated(carActor, makeGroup, model, vin)
            )
            carModelToActor += model -> carActor
            replyTo ! CarRegistered(carActor)
        }
        this
      }
      case RequestTrackCar(make, _, _, _) =>
        context.log.warn(
          "❌❌ Ignoring TrackCar request for {}. This actor is responsible for {}.",
          make,
          makeGroup
        )
        Behaviors.unhandled
      case RequestCarList(requestId, `makeGroup`, replyTo) =>
        context.log.info(
          "✅✅ Sending Car List Data for make group: {}",
          makeGroup
        )
        replyTo ! ReplyCarList(requestId, carModelToActor.keySet)
        this
      case RequestCarList(_, make, _) =>
        context.log.warn(
          "❌❌ Ignoring RequestCarList request for {}. This actor is responsible for {}.",
          make,
          makeGroup
        )
        Behaviors.unhandled
      case CarTerminated(_, make, model, vin) =>
        context.log.warn(
          "🛑🛑 Car Actor for {} {} with vin: {} has been terminated",
          make,
          model,
          vin
        )
        carModelToActor -= model
        this
      case RequestAllSpeeds(requestId, `makeGroup`, replyTo) =>
        context.spawnAnonymous(
          CarGroupQuery(carModelToActor, requestId, requester = replyTo, timeout = 3.seconds)
          )
        this
      case _ =>
        Behaviors.unhandled
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("🛑🛑 Car Group {} Stopped", makeGroup)
      this
  }
}

object CarGroup {
  def apply(makeGroup: String): Behavior[Command] =
    Behaviors.setup(new CarGroup(_: ActorContext[CarGroup.Command])(makeGroup))
  trait Command

  final case class CarTerminated(
      car: ActorRef[Car.Command],
      make: String,
      model: String,
      vin: String
  ) extends CarGroup.Command
}
