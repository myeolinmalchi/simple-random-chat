package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

class MatchManager extends Actor {
	
	override def receive: Receive = noWaiting
	
	def noWaiting: Receive = {
		case User.Waiting =>
			val user = sender()
			println(s"${user.path.name} is waiting.")
			context.watch(sender())
			context.become(withWaiting(user))
	}
	
	def withWaiting(waitingUser: ActorRef): Receive = {
		case User.Waiting =>
			val newUser = sender()
			println(s"${waitingUser.path.name} and ${newUser.path.name} are matched.")
			val newChat = context.actorOf(Props(new ChatManager(waitingUser, newUser)))
			waitingUser ! User.Matched(newChat)
			newUser ! User.Matched(newChat)
			context.unwatch(waitingUser)
			context.become(noWaiting)
		case Terminated(_) => context.become(noWaiting)
	}
}
