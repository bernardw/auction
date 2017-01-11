package actors

import akka.actor._
import models._

sealed trait BidderState

case object Idle extends BidderState

case object Active extends BidderState

case object Inactive extends BidderState

/**
  * Absentee bidder actor
  *
  * state transitions
  *
  * Idle -> Active (when admitted to auction)
  * action -> submit starting bid
  *
  * Active -> Active (when outbid)
  * action -> submit next bid
  *
  * Active -> Inactive (when auction closed)
  * action -> report to bidder
  *
  * Created by bernardw on 9/16/16.
  */
class Bidder(profile: Profile) extends FSM[BidderState, Bid] with ActorLogging {

  startWith(Idle, Bid(self, profile.startingBid))

  when(Idle) {
    case Event(Joined, b: Bid) => goto(Active) using b
  }

  when(Active) {
    case Event(Bid(bidder, price), b: Bid) => {
      if (bidder != self && b.price <= price && nextBid <= profile.maxBid) {
        goto(Active) using b.copy(price = nextBid)
      } else {
        stay
      }
    }
    case Event(Sold, _) => goto(Inactive) replying CheckStatus
  }

  when(Inactive)(FSM.NullFunction)

  whenUnhandled {
    case Event(AuctionStatus(state, winner, price), _) =>
      log.info("{} knows {} won with ${}", profile.name, winner, price)
      // TODO: report to bidder
      stay
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case _ -> Active => sender ! nextStateData
  }

  def nextBid = stateData.price + profile.increment

}