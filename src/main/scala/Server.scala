import actors.{MatchRouter, User}
import akka.NotUsed
import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.io.StdIn
import scala.language.postfixOps

object Server extends App{
	implicit val system = ActorSystem("chatServer")
	implicit val materializer = ActorMaterializer
	
	val matchRouter = system.actorOf(Props[MatchRouter], "matchRouter")
	
	def newUser(name: String): Flow[Message, Message, NotUsed] = {
		val userActor = system.actorOf(Props(new User(name, matchRouter)))
		println(s"actors.User $name has been connected.")
		
		val incomingMessages: Sink[Message, NotUsed] = {
			Flow[Message].map {
				case TextMessage.Strict(text) => User.IncomingMessage(text)
			}.to(Sink.actorRef[User.IncomingMessage](userActor, PoisonPill))
		}
		
		val outgoingMessages: Source[Message, NotUsed] =
			Source.actorRef[User.OutgoingMessage](10, OverflowStrategy.fail)
					.mapMaterializedValue { outActor =>
						userActor ! User.Connected(outActor)
						NotUsed
					}.map { outMsg: User.OutgoingMessage => TextMessage(outMsg.msg) }
		
		Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
	}
	
	val route =
		(get & pathSingleSlash) {
			getFromFile("public/htmls/chat.html")
		} ~ (path("chat") & parameter("name")) { name =>
			handleWebSocketMessages(newUser(name))
		} ~ pathPrefix("assets") {
			getFromDirectory("public")
		}
	
	val (host, port) = ("0.0.0.0", 8000)
	val binding = Await.result(Http().newServerAt(host, port).bind(route), 3 seconds)
	
	println(s"Server started at ${host}:${port}, press enter to kill server")
	StdIn.readLine()
	system.terminate()
	
}
