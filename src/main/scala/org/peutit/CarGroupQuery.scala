package org.peutit

import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.scaladsl.TimerScheduler
import org.apache.pekko.actor
import scala.concurrent.duration.FiniteDuration

class CarGroupQuery(context: ActorContext[CarGroupQuery.Command])(
    carModelToActor: Map[String, ActorRef[Car.Command]],
    requestId: Long,
    requester: ActorRef[CarManager.RespondAllSpeeds],
    timeout: FiniteDuration,
    timers: TimerScheduler[CarGroupQuery.Command]
) extends AbstractBehavior[CarGroupQuery.Command](context) {

  import CarGroupQuery.{CarTerminated, CollectionTimeout, WrappedRespondSpeed}
  import CarManager.{
    CarNotAvailable,
    CarTimedOut,
    RespondAllSpeeds,
    Speed,
    SpeedNotAvailable,
    SpeedReading
  }
  import Car.ReadSpeed

  timers.startSingleTimer(CollectionTimeout, CollectionTimeout, timeout)

  private val respondSpeedAdapter: ActorRef[Car.RespondSpeed] =
    context.messageAdapter(WrappedRespondSpeed.apply)

  carModelToActor.foreach { case (model, car) =>
    context.watchWith(car, CarTerminated(model))
    car ! ReadSpeed(0, respondSpeedAdapter)
  }

  private var repliesSoFar = Map.empty[String, SpeedReading]
  private var stillWaiting = carModelToActor.keySet

  override def onMessage(
      msg: CarGroupQuery.Command
  ): Behavior[CarGroupQuery.Command] = msg match {
    case WrappedRespondSpeed(response) => onRespondSpeed(response)
    case CarTerminated(model)          => onCarTerminated(model)
    case CollectionTimeout             => onCollectionTimeout()
  }

  private def onRespondSpeed(
      response: Car.RespondSpeed
  ): Behavior[CarGroupQuery.Command] = {
    val reading = response.value match {
      case Some(value) => Speed(value)
      case None        => SpeedNotAvailable
    }

    val model = response.model
    repliesSoFar += (model -> reading)
    stillWaiting -= model

    respondWhenAllCollected()
  }

  private def onCarTerminated(
      model: String
  ): Behavior[CarGroupQuery.Command] = {
    if (stillWaiting(model)) {
      repliesSoFar += (model -> CarNotAvailable)
      stillWaiting -= model
    }

    respondWhenAllCollected()
  }

  private def onCollectionTimeout(): Behavior[CarGroupQuery.Command] = {
    repliesSoFar ++= stillWaiting.map(_ -> CarTimedOut)
    stillWaiting = Set.empty

    respondWhenAllCollected()
  }

  private def respondWhenAllCollected(): Behavior[CarGroupQuery.Command] = {
    stillWaiting.isEmpty match {
      case true => {
        requester ! RespondAllSpeeds(requestId, repliesSoFar)
        Behaviors.stopped
      }
      case false => this
    }
  }
}

object CarGroupQuery {
  def apply(
      carModelToActor: Map[String, ActorRef[Car.Command]],
      requestId: Long,
      requester: ActorRef[CarManager.RespondAllSpeeds],
      timeout: FiniteDuration
  ): Behavior[CarGroupQuery.Command] =
    Behaviors.setup { (context: ActorContext[Command]) =>
      context.log.info(
        "✅✅ Create CarGroupeQuery with carModelToActor: {}, requestId: {}, requester: {}, timeout: {}",
        carModelToActor,
        requestId,
        requester,
        timeout
      )
      Behaviors.withTimers { (timers: TimerScheduler[Command]) =>
        new CarGroupQuery(context)(
          carModelToActor,
          requestId,
          requester,
          timeout,
          timers
        )
      }
    }

  trait Command

  private case object CollectionTimeout extends Command

  final case class WrappedRespondSpeed(response: Car.RespondSpeed)
      extends Command

  private final case class CarTerminated(model: String) extends Command
}
