package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Terminated}

object UserManager {
	case class NewUser(user: ActorRef)
}
class UserManager extends Actor with ActorLogging{
	import UserManager._
	 override def receive: Receive = withUsers(List())
	
	def withUsers(users: List[ActorRef]): Receive = {
		case NewUser(user) =>
			context.watch(user)
			val newUsers = users :+ user
			context.become(withUsers(newUsers))
		case Terminated(user) =>
			context.unwatch(user)
			val newUsers = users.filterNot(_.path == user.path)
			context.become(withUsers(newUsers))
	}
	
}
