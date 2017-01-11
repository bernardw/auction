import actors.{Auction, Bidder}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import models._
import spray.json._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val itemFormat = jsonFormat2(Item)
  implicit val profileFormat = jsonFormat4(Profile)
  implicit val statusFormat = jsonFormat3(AuctionStatus)
}

object AuctionService extends App with Directives with JsonSupport {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout(100 seconds)

  val route =
    path("auction") {
      post {
        entity(as[Item]) { item =>
          val id = getAuctionId(item.name)
          val actor: Try[ActorRef] = Try {
            system.actorOf(Props(classOf[Auction], item, 10 minutes), name = id)
          }
          actor match {
            case Success(auction) => {
              onComplete((auction ? CheckStatus).mapTo[AuctionStatus]) {
                complete(_)
              }
            }
            case Failure(e) => {
              complete(StatusCodes.BadRequest, e.getMessage)
            }
          }
        }
      }
    } ~
      path("auction" / Remaining) { auctionName =>
        val auctionId = getAuctionId(auctionName)
        post {
          entity(as[Profile]) { profile =>
            val bidderId = getBidderId(auctionId, profile.name)
            for {
              bidder <- Try(system.actorOf(Props(classOf[Bidder], profile), name = bidderId))
              auction <- Try(system.actorSelection(getActorId(auctionId)))
            } yield {
              auction ? Join(bidder, profile.name)
            }
            complete(profile)
          }
        } ~
          get {
            val auction = system.actorSelection(getActorId(auctionId))
            onComplete((auction ? CheckStatus).mapTo[AuctionStatus]) {
              complete(_)
            }
          }
      }

  val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

  def getBidderId(auctionId: String, name: String) =
    "bid-%s-%s".format(auctionId, name.replace(' ', '_').toLowerCase)

  def getAuctionId(itemName: String) =
    "auc-%s".format(itemName.replace(' ', '_').toLowerCase)

  def getActorId(id: String) = "akka://default/user/%s".format(id)

}