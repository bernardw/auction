package actors

import akka.actor._
import models._

import scala.collection.mutable
import scala.concurrent.duration._

sealed trait AuctionState

case object Bidding extends AuctionState

case object Closed extends AuctionState

/**
  * Auction actor
  *
  * state transition
  *
  * Bidding -> Bidding (when highest bid is updated)
  * action -> broadcast new bid to participants
  *
  * Bidding -> Closed (when Cancel / StateTimeout)
  * action -> auction closed. no more bid accepted
  *
  * Created by bernardw on 9/16/16.
  */
class Auction(item: Item, timeout: FiniteDuration = 10 seconds) extends FSM[AuctionState, Bid] with ActorLogging {

  import context._

  private[this] def getName(bidder: ActorRef) = bidders.getOrElse(bidder, "consignor")

  private[this] var bidders = mutable.Map[ActorRef, String]()

  startWith(Bidding, Bid(system.deadLetters, 0))

  when(Bidding, stateTimeout = timeout) {
    case Event(Join(bidder, name), _) => {
      bidders += (bidder -> name)
      if (sender != bidder) bidder ! Joined
      stay replying Joined
    }
    case Event(c: Bid, b: Bid) => {
      // compare contending bid with current bid
      if (c.price > b.price) {
        log.info("{} => {}, ${} => ${}", getName(b.bidder), getName(c.bidder), b.price, c.price)
        goto(Bidding) using c
      } else {
        log.info("{} attempted => ${}", getName(c.bidder), c.price)
        stay replying b
      }
    }
    case Event(Cancel | StateTimeout, _) => goto(Closed)
  }

  when(Closed)(FSM.NullFunction)

  whenUnhandled {
    case Event(CheckStatus, _) => stay replying currentStatus
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case Bidding -> Bidding => {
      log.info("current bid => {} @ ${}", getName(nextStateData.bidder), nextStateData.price)
      for (bidder <- bidders.keys if bidder != nextStateData.bidder)
        bidder ! nextStateData
    }
    case Bidding -> Closed => {
      if (nextStateData.price >= item.reservePrice) {
        log.info("winner is {} for ${}", getName(nextStateData.bidder), nextStateData.price)
        for (bidder <- bidders.keys)
          bidder ! Sold
      } else {
        log.info("bid ${} failed reserve price of ${}", nextStateData.price, item.reservePrice)
      }
    }
  }

  def currentStatus = AuctionStatus(stateName.toString, getName(stateData.bidder), stateData.price)

}