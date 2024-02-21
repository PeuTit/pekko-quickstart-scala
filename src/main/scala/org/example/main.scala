package org.example

import org.apache.pekko.actor.typed.Behavior
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.actor.typed.scaladsl.ActorContext
import org.apache.pekko.actor.typed.scaladsl.AbstractBehavior
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.ActorRef
import org.apache.pekko.actor.typed.Signal
import org.apache.pekko.actor.typed.PostStop
import org.apache.pekko.actor.typed.SupervisorStrategy
import org.apache.pekko.actor.TypedActor
import org.apache.pekko.actor.typed.PreRestart

object PrintMyActorRefActor {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new PrintMyActorRefActor(context))
}

class PrintMyActorRefActor(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
  println("PrintMyActor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "printit" =>
        val secondRef: ActorRef[String] =
          context.spawn(Behaviors.empty[String], "second-actor")
        println(s"Second: $secondRef")
        this
      case "stop" =>
        Behaviors.stopped
      case _ =>
        Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("PrintMyActor stopped")
      this
  }
}

object SupervisingActor {
  def apply(): Behavior[String] =
    Behaviors.setup(new SupervisingActor(_))
}

class SupervisingActor(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
  println("SupervisingActor started")
  private val child = context.spawn(
    Behaviors
      .supervise(SupervisedActor())
      .onFailure(SupervisorStrategy.restart),
    "supervised-actor"
  )

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail-child" =>
        child ! "fail"
        this
      case _ =>
        Behaviors.unhandled
    }
}
object SupervisedActor {
  def apply(): Behavior[String] =
    Behaviors.setup(new SupervisedActor(_))
}

class SupervisedActor(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
  println("SupervisedActor started")

  override def onMessage(msg: String): Behavior[String] =
    msg match {
      case "fail" =>
        println("SupervisedActor fail now")
        throw new Exception("I failed! 🤠")
        this
      case _ =>
        Behaviors.unhandled
    }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PreRestart =>
      println("SupervisedActor restarted")
      this
    case PostStop =>
      println("SupervisedActor stopped")
      this
  }
}

object Main {
  def apply(): Behavior[String] =
    Behaviors.setup(context => new Main(context))
}

class Main(context: ActorContext[String])
    extends AbstractBehavior[String](context) {
  println("MainActor started")

  override def onMessage(msg: String): Behavior[String] = {
    msg match {
      case "start" =>
        val firstRef: ActorRef[String] =
          context.spawn(PrintMyActorRefActor(), "first-actor")
        val secondRef: ActorRef[String] =
          context.spawn(SupervisingActor(), "supervising-actor")
        println(s"First: $firstRef")
        firstRef ! "printit"
        secondRef ! "fail-child"
        firstRef ! "stop"
        this
      case "stop" =>
        Behaviors.stopped
      case _ =>
        Behaviors.unhandled
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[String]] = {
    case PostStop =>
      println("MainActor stopped")
      this
  }
}

object ActorHierarchyExperiments extends App {
  val testSystem = ActorSystem(Main(), "test-system")
  testSystem ! "start"
  testSystem ! "stop"
}
