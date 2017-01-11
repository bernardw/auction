package scenarios

import actors._
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import models._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by bernardw on 9/16/16.
  */
class BoatAuctionSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("auction"))

  def bidder(profile: Profile) =
    (profile.name, system.actorOf(Props(classOf[Bidder], profile)))

  val item = Item("Boat", 0)
  val alice = bidder(Profile("Alice", 2500, 3000, 500))
  val aaron = bidder(Profile("Aaron", 2800, 3100, 201))
  val amanda = bidder(Profile("Amanda", 2501, 3200, 247))
  val auction = TestFSMRef(new Auction(item))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "boat auction" must {
    "aaron wins" in {
      for ((name, bidder) <- Array(alice, aaron, amanda))
        auction ! Join(bidder, name)
      awaitCond(auction.stateName == Closed, 5 minute, 2 second)
      val (_, ref) = aaron
      assert(auction.stateData == Bid(ref, 3001))
    }
  }

}