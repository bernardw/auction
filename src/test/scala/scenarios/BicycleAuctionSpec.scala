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
class BicycleAuctionSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("auction"))

  def bidder(profile: Profile) =
    (profile.name, system.actorOf(Props(classOf[Bidder], profile)))

  val item = Item("Bicycle", 0)
  val alice = bidder(Profile("Alice", 50, 80, 3))
  val aaron = bidder(Profile("Aaron", 60, 82, 2))
  val amanda = bidder(Profile("Amanda", 55, 85, 5))
  val auction = TestFSMRef(new Auction(item))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "bicycle auction" must {
    "amanda wins" in {
      for ((name, bidder) <- Array(alice, aaron, amanda))
        auction ! Join(bidder, name)
      awaitCond(auction.stateName == Closed, 5 minute, 2 second)
      val (_, ref) = amanda
      assert(auction.stateData == Bid(ref, 85))
    }
  }

}