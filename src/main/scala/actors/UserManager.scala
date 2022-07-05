package actors

import akka.NotUsed
import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, PoisonPill, Props, Terminated}
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.BroadcastHub
import akka.stream.{ActorMaterializer, Attributes, DelayOverflowStrategy, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object UserManager {
	case object NewUser
	case class CheckUser(name: String)
	case class OutgoingMessage(msg: String)
	case class NewConnection(outActor: ActorRef)
}
class UserManager(matchRouter: ActorRef, eventSource: ActorRef) extends Actor with ActorLogging{
	import UserManager._
	override def receive: Receive = withUsers(Map(), 1)
	
	def withUsers(users: Map[String, ActorRef], userCounter: Int): Receive = {
		case NewUser =>
			val newUserName = s"User$userCounter"
			val newUser = context.actorOf(Props(new User(newUserName, matchRouter)), newUserName)
			val newUsers = users + (newUserName -> newUser)
			context.watch(newUser)
			context.become(withUsers(newUsers, userCounter + 1))
			eventSource ! StreamingAccessorCount.UpdateAccessorCount(newUsers.size)
			sender() ! userMsgFlow(newUser)
			
		case Terminated(user) =>
			val newUsers = users.filterNot(_._2.path.equals(user.path))
			println(s"${user.path.name} terminated.")
			eventSource ! StreamingAccessorCount.UpdateAccessorCount(newUsers.size)
			context.become(withUsers(newUsers, userCounter))
	}
	
	def userMsgFlow(userActor: ActorRef): Flow[Message, Message, NotUsed] = {
		val incomingMessages: Sink[Message, NotUsed] =
			Flow[Message].map {
				case TextMessage.Strict(text) => User.IncomingMessage(text)
			}.to(Sink.actorRef[User.IncomingMessage](
				ref = userActor,
				onCompleteMessage = PoisonPill,
				onFailureMessage = { _: Throwable => PoisonPill}
			))
		val outgoingMessages: Source[Message, NotUsed] =
			Source.actorRef[User.OutgoingMessage](1024, OverflowStrategy.dropHead)
					.mapMaterializedValue { outActor =>
						userActor ! User.Connected(outActor)
						NotUsed
					}.map { outMsg: User.OutgoingMessage => TextMessage(outMsg.msg) }
		
		
		Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
				.addAttributes(Attributes.inputBuffer(initial=1, max=1024))
	}
}
