import actors._
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestFSMRef, TestKit}
import models._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * Created by bernardw on 9/16/16.
  */
class BidderActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem("auction"))

  val alice = TestFSMRef(new Bidder(Profile("Alice", 50, 90, 2)))
  val david = TestFSMRef(new Bidder(Profile("David", 60, 90, 4)))

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "bidder actor" must {
    "idle on initiation" in {
      assert(alice.stateName == Idle)
    }
    "send starting bid after admission" in {
      alice ! Joined
      assert(alice.stateName == Active)
      expectMsg(Bid(alice, 50))
    }
    "send next bid when outbid" in {
      alice ! Bid(david, 60)
      expectMsg(Bid(alice, 52))
    }
    "send contending bid when tied" in {
      alice ! Bid(david, 52)
      expectMsg(Bid(alice, 54))
    }
  }

}