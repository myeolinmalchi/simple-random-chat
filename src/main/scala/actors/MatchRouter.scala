package actors

import akka.actor.{Actor, ActorLogging, Props, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

class MatchRouter extends Actor with ActorLogging{
	
	private val matchManagers = for(i <- 1 to 2) yield {
		val manager = context.actorOf(Props[MatchManager], s"matchManager_${i}")
		context.watch(manager)
		ActorRefRoutee(manager)
	}
	private val router = Router(RoundRobinRoutingLogic(), matchManagers)
	override def receive: Receive = receiver(router)
	
	def receiver(router: Router): Receive = {
		case Terminated(ref) =>
			val newManager = context.actorOf(Props[MatchManager])
			val newRouter = router.removeRoutee(ref).addRoutee(newManager)
			context.watch(newManager)
			context.become(receiver(newRouter))
		case message =>
			router.route(message, sender())
	}
}
