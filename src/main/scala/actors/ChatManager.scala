package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}
import akka.util.Timeout
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object ChatManager {
	case class UserJoin(name: String)
	case class ChatMessage(msg: String)
	case object ChatTerminated
}
class ChatManager(a: ActorRef, b: ActorRef) extends Actor with ActorLogging {
	
	println("New Chatroom has been opened.")
	import ChatManager._
	implicit val timeout: Timeout = Timeout(1 second)
	implicit val executionContext: ExecutionContext = context.dispatcher
	context.watch(a)
	context.watch(b)
	
	override def receive: Receive = {
		case Terminated(_) =>
			a ! ChatTerminated
			b ! ChatTerminated
		case User.Quit =>
			a ! ChatTerminated
			b ! ChatTerminated
		case msg: ChatMessage =>
			a ! msg
			b ! msg
	}
}
