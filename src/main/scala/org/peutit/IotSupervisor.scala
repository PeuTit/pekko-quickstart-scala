package org.peutit

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.TypedActor
import org.apache.pekko.actor.typed.PostStop

class IotSupervisor(context: ActorContext[Nothing])
    extends AbstractBehavior[Nothing](context) {
  context.log.info("🦸🦸 IotSupervisor Started")

  override def onMessage(msg: Nothing): Behavior[Nothing] =
    Behaviors.unhandled

  override def onSignal: PartialFunction[Signal, Behavior[Nothing]] = {
    case PostStop => {
      context.log.info("🦸🦸 IotSupervisor Stopped")
      this
    }
  }
}

object IotSupervisor {
  def apply(): Behavior[Nothing] =
    Behaviors.setup[Nothing](new IotSupervisor(_))
}
