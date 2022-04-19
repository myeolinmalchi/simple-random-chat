package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

class MatchManager extends Actor {
	
	override def receive: Receive = noWaiting
	
	def noWaiting: Receive = {
		case User.Waiting(name) =>
			println(s"$name is waiting.")
			context.watch(sender())
			context.become(withWaiting(sender(), name))
	}
	
	def withWaiting(waitingUser: ActorRef, waitingUserName: String): Receive = {
		case User.Waiting(name) =>
			println(s"User $waitingUserName and $name are matched.")
			val newUser = sender()
			val newChat = context.actorOf(Props(new ChatManager(waitingUser, newUser)))
			waitingUser ! User.Matched(newChat, name)
			newUser ! User.Matched(newChat, waitingUserName)
			context.unwatch(waitingUser)
			context.become(noWaiting)
		case Terminated(_) => context.become(noWaiting)
	}
}
