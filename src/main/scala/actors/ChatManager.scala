package actors

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Terminated}
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object ChatManager {
	case class UserJoin(name: String)
	case class ChatMessage(msg: String)
	case object ChatTerminated
}
class ChatManager(a: ActorRef, b: ActorRef) extends Actor {
	
	import ChatManager._
	implicit val timeout: Timeout = Timeout(1 second)
	implicit val executionContext: ExecutionContext = context.dispatcher
	context.watch(a)
	context.watch(b)
	
	override def receive: Receive = {
		case Terminated(_) => stop()
		case User.Quit => stop()
		case msg: ChatMessage =>
			a ! msg
			b ! msg
	}
	
	private def stop(): Unit = {
		println(s"Chat terminated: $self")
		a ! ChatTerminated
		b ! ChatTerminated
		self ! PoisonPill
	}
}
