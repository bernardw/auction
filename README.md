Auction
=======
A library project for creating a simulated auction house using actors.

It contains two types of actors, Auction and Bidder, both are FSM.

Below are events and their corresponding actions in their primary state.

Start
------
```bash
sbt run
```

API
------
### POST &nbsp;&nbsp;&nbsp;&nbsp; /auction
Request
```json
{
  "name": "Playstation 4 Pro",
  "reservePrice": 10
}
```
Response
```json
{
   "state":"Bidding",
   "winner":"consignor",
   "price":0
}
```

### GET &nbsp;&nbsp;&nbsp;&nbsp; /auction/@itemId &nbsp;&nbsp;&nbsp;&nbsp; (e.g. /auction/playstation_4_pro)
Response
```json
{
   "state":"Bidding",
   "winner":"consignor",
   "price":0
}
```
### POST &nbsp;&nbsp;&nbsp;&nbsp; /auction/@itemId
Request
```json
{
  "name": "John",
  "startingBid": 30,
  "maxBid": 1000,
  "increment": 3
}
```
Response
```json
{
  "name": "John",
  "startingBid": 30,
  "maxBid": 1000,
  "increment": 3
}
```

Actors
------

**Auction.scala**

|Event           | Actions                                 |
|:---------------|:----------------------------------------|
|Join            | Register new bidder.                    |
|Bid             | Receive new bid, compare and broadcast. |
|CheckStatus     | Send current state and current bid.     |
|StateTimeout    | Close auction.                          |
|Cancel          | Close auction.                          |

**Bidder.scala**

|Event           | Actions                                 |
|:---------------|:----------------------------------------|
|Joined          | Send starting bid.                      |
|Bid             | Receive contending bid, send counter.   |
|Sold            | Auction closed, request information.    |

Scenarios
-------------
```bash
sbt
> test-only scenarios.BicycleAuctionSpec
> test-only scenarios.ScooterAuctionSpec
> test-only scenarios.BoatAuctionSpec
```