import actors._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import models._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
  * Created by bernardw on 9/16/16.
  */
class AuctionActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("auction"))

  val item = Item("Rocket Launcher", 40)
  val alice = TestFSMRef(new Bidder(Profile("Alice", 50, 90, 2)))
  val peter = TestFSMRef(new Bidder(Profile("Peter", 20, 90, 4)))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "Auction actor" must {
    "accept new bidder" in {
      val auction = TestFSMRef(new Auction(item))
      assert(peter.stateName == Idle)
      auction ! Join(peter, "Peter")
      assert(peter.stateName == Active)
      expectMsg(Joined)
    }
    "accept higher bid" in {
      val auction = TestFSMRef(new Auction(item))
      auction ! Bid(peter, 30) // current bid
      assert(auction.stateData == Bid(peter, 30))

      auction ! Bid(alice, 50)
      assert(auction.stateData == Bid(alice, 50))
    }
    "reject low bid and echo current bid" in {
      val auction = TestFSMRef(new Auction(item))
      auction ! Bid(alice, 50)
      auction ! Bid(peter, 40)
      assert(auction.stateData == Bid(alice, 50))
      expectMsg(Bid(alice, 50))
    }
    "take first bid when tied" in {
      val auction = TestFSMRef(new Auction(item))
      auction ! Bid(alice, 60)
      auction ! Bid(peter, 60)
      assert(auction.stateData == Bid(alice, 60))
    }
    "close auction after timeout" in {
      val auction = TestFSMRef(new Auction(item))
      auction ! Bid(alice, 50)
      awaitCond(auction.stateName == Closed, 5 minute, 2 second)
      assert(auction.stateName == Closed)
      assert(auction.stateData == Bid(alice, 50))
    }
    "cancel auction" in {
      val auction = TestFSMRef(new Auction(item))
      assert(auction.stateName == Bidding)
      auction ! Cancel
      assert(auction.stateName == Closed)
    }
  }

}