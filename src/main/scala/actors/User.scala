package actors

import akka.actor.{Actor, ActorLogging, ActorRef}

object User {
	case class Connected(outActor: ActorRef)
	case class ConnectionFailed(ex: Exception)
	case class Matched(chatManager: ActorRef, name: String)
	case class IncomingMessage(msg: String)
	case class OutgoingMessage(msg: String)
	case class Waiting(name: String)
	case object Quit
	case object Terminate
	case class PartnerName(name: String)
}
class User(name: String, matchManager: ActorRef) extends Actor with ActorLogging{
	import User._
	
	override def receive: Receive = awaitConnection
	
	def awaitConnection: Receive = {
		case Connected(outgoing) =>
			println(s"[$name] Connected with server.")
			matchManager ! Waiting(name)
			context.become(awaitPartner(outgoing))
	}
	
	def awaitPartner(outgoing: ActorRef): Receive = {
		case Matched(chatManager, partner) =>
			outgoing ! OutgoingMessage(partner)
			context.become(withPartner(chatManager, outgoing))
		case IncomingMessage("/terminate") =>
			println(s"User $name terminate.")
			context.stop(self)
	}
	
	def withPartner(chatManager: ActorRef, outgoing: ActorRef): Receive = {
		case IncomingMessage("/quit") =>
			chatManager ! Quit
		case IncomingMessage("/terminate") =>
			println(s"User $name terminate.")
			context.stop(self)
		case IncomingMessage(msg) =>
			println(s"[${name}] Sent a message: $msg")
			chatManager ! ChatManager.ChatMessage(s"$name: $msg")
		case ChatManager.ChatMessage(msg) => outgoing ! OutgoingMessage(msg)
		case ChatManager.ChatTerminated =>
			outgoing ! OutgoingMessage("/terminated")
			matchManager ! Waiting(name)
			context.become(awaitPartner(outgoing))
	}
}
