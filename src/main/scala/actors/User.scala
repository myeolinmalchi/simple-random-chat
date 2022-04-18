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
class User(name: String, matchManager: ActorRef) extends Actor{
	import User._
	
	override def receive: Receive = awaitConnection
	
	def awaitConnection: Receive = {
		case Connected(outgoing) =>
			println(s"[$name] Connected with server.")
			context.become(awaitPartner(outgoing))
			matchManager ! Waiting(name)
	}
	
	def awaitPartner(outgoing: ActorRef): Receive = {
		case Matched(chatManager, partner) =>
			outgoing ! OutgoingMessage(s"${partner}님과 연결되었습니다.")
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
			chatManager ! Quit
			context.stop(self)
		case IncomingMessage(msg) =>
			println(s"[${name}] Sent a message: $msg")
			chatManager ! ChatManager.ChatMessage(s"$name: $msg")
		case ChatManager.ChatMessage(msg) =>
			println(s"[${name}] Got a message: $msg")
			outgoing ! OutgoingMessage(msg)
		case ChatManager.ChatTerminated =>
			context.become(awaitPartner(outgoing))
			outgoing ! OutgoingMessage("/terminated")
			matchManager ! Waiting(name)
	}
}
