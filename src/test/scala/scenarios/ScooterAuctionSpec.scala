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
class ScooterAuctionSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("auction"))

  def bidder(profile: Profile) =
    (profile.name, system.actorOf(Props(classOf[Bidder], profile)))

  val item = Item("Scotter", 0)
  val alice = bidder(Profile("Alice", 700, 725, 2))
  val aaron = bidder(Profile("Aaron", 599, 725, 15))
  val amanda = bidder(Profile("Amanda", 625, 725, 8))
  val auction = TestFSMRef(new Auction(item))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "scooter auction" must {
    "alice wins" in {
      for ((name, bidder) <- Array(alice, aaron, amanda))
        auction ! Join(bidder, name)
      awaitCond(auction.stateName == Closed, 5 minute, 2 second)
      val (_, ref) = alice
      assert(auction.stateData == Bid(ref, 722))
    }
  }

}