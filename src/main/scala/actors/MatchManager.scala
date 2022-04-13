package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}

class MatchManager extends Actor with ActorLogging{
	
	override def receive: Receive = noWaiting(List())
	
	def noWaiting(chatList: List[ActorRef]): Receive = {
		case User.Waiting(name) =>
			context.become(withWaiting(chatList, sender(), name))
		case Terminated(chat) =>
			val newList = chatList filterNot chat.==
			context.become(noWaiting(newList))
	}
	
	def withWaiting(chatList: List[ActorRef], waitingUser: ActorRef, waitingUserName: String): Receive = {
		case User.Waiting(name) =>
			println(s"User $waitingUserName and $name are matched.")
			val newChat = context.actorOf(Props(new ChatManager(waitingUser, sender())))
			waitingUser ! User.Matched(newChat, name)
			sender() ! User.Matched(newChat, waitingUserName)
			context.watch(newChat)
			context.become(noWaiting(newChat::chatList))
		case Terminated(chat) =>
			val newList = chatList filterNot chat.==
			context.become(withWaiting(newList, waitingUser, waitingUserName))
	}
}
