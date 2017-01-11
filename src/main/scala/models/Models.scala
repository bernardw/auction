package models

import akka.actor.ActorRef

/**
  * Instance identifiers
  */
final case class Item(name: String, reservePrice: Int)

final case class Profile(name: String, startingBid: Int, maxBid: Int, increment: Int)

/**
  * Shared events for Auction and Bidder
  */
case object Joined

case class Join(bidder: ActorRef, name: String)

case class Bid(bidder: ActorRef, price: Int)

case object Sold

case object Cancel

case object CheckStatus

case class AuctionStatus(state: String, winner: String, price: Int)