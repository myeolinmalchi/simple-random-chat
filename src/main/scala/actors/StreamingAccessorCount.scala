package actors

import akka.actor.Actor
import akka.stream.scaladsl.SourceQueueWithComplete
import scala.concurrent.duration.DurationInt

object StreamingAccessorCount {
	case class UpdateAccessorCount(count: Int)
	case object OfferAccessorCount
}
class StreamingAccessorCount(source: SourceQueueWithComplete[String]) extends Actor{
	import StreamingAccessorCount._
	
	implicit val ec = context.dispatcher
	
	override def receive: Receive = receiveHandler(0)
	
	def receiveHandler(count: Int): Receive = {
		case UpdateAccessorCount(count) =>
			context.become(receiveHandler(count))
		case OfferAccessorCount =>
			context.system.scheduler.scheduleOnce(1.seconds) {
				self ! OfferAccessorCount
			}
			source.offer(count.toString)
	}
}
