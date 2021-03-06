import actors.{MatchRouter, StreamingAccessorCount, UserManager}
import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, Source}
import akka.stream.{ActorMaterializer, DelayOverflowStrategy, OverflowStrategy}
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Server extends App{
	implicit val system = ActorSystem("chatServer")
	implicit val materializer = ActorMaterializer
	implicit val timeout = Timeout(1 seconds)
	implicit lazy val ec = system.dispatcher
	
	import akka.http.scaladsl.marshalling.sse.EventStreamMarshalling._
	
	lazy val (sourceQueue, eventsSource) = Source.queue[String](1024, OverflowStrategy.dropHead)
			.delay(1.seconds, DelayOverflowStrategy.backpressure)
			.map(message => ServerSentEvent(message))
			.keepAlive(1.seconds, () => ServerSentEvent.heartbeat)
			.toMat(BroadcastHub.sink[ServerSentEvent])(Keep.both)
			.run()
	
	lazy val streamingActor = system.actorOf(
		Props(new StreamingAccessorCount(sourceQueue))
	)
	
	val matchRouter = system.actorOf(Props[MatchRouter], "matchRouter")
	val userManager = system.actorOf(
		Props(new UserManager(matchRouter, streamingActor)),
		"userManager"
	)
	
	lazy val route =
		path("chat") {
			onComplete(userManager ? UserManager.NewUser) {
				case Success(flow: Flow[Message, Message, NotUsed]) =>
					handleWebSocketMessages(flow)
				case Failure(_) => complete(StatusCodes.BadRequest)
			}
		} ~ pathPrefix("assets") {
			getFromDirectory("public")
		} ~ path("visitor") {
			respondWithHeaders(RawHeader("Access-Control-Allow-Origin", "*")) {
				complete{
					system.scheduler.scheduleOnce(2.second) {
						streamingActor ! StreamingAccessorCount.OfferAccessorCount
					}
					eventsSource
				}
			}
		}
	
	val (host, port) = ("0.0.0.0", 5000)
	val binding = Await.result(Http().newServerAt(host, port).bind(route), 3 seconds)
	
	println(s"Server started at ${host}:${port}, press enter to kill server")
	StdIn.readLine()
	system.terminate()
	
}
