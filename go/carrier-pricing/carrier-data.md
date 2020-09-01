# Technical test

## Introduction

The purpose of this test is to ensure you have some basic knowledge of Golang in a web and REST API context.

## Deliverable

- The source code of the server, with simple build instructions.
- All necessary script to create and seed the different databases IF NECESSARY.
- The software will be compiled with the latest go version.
- The software and its ecosystem will be run and tested on a local computer.

_Note: You can use Docker and docker-compose to provide the databases._

_Note: Ideally, you should only use the standard lib, except for the database drivers. If you feel that a better library to complete the exercise is available to you, just explain why it's better suited ._



### Basic Service

Build a basic service that responds to a POST request to an endpoint called /quotes, with the following request structure:

```
{
  "pickup_postcode":   "SW1A1AA",
  "delivery_postcode": "EC2A3LT"
}
```
And responds with the following price:
```
{
  "pickup_postcode":   "SW1A1AA",
  "delivery_postcode": "EC2A3LT",
  "price":             316
}
```

The price we charge depends on the distance between two postcodes. We are not implementing postcode geocoding here, so instead we are using basic formula for working out the price for a quote between two postcodes. The process is to take the base-36 integer of each postcode, subtract the delivery postcode from the pickup postcode and then divide by some large number. If the result is negative, turn it into a positive.


`Base64("SW1A1AA", 36) - Base64("EC2A3LT", 36)`

If you have a better idea for a deterministic way of making a number from two postcodes, please feel free to use that instead. Update your service to calculate pricing based upon these rules.

The requests should be logged at least on the consoles, and we expect to have some tests (unit adn/or integration).

## Features to complete

### 1) Simple variable prices by vehicle

Our price changes based upon the vehicle. Implement a "vehicle" attribute on the request, that takes one of the following values, applying the appropriate markup:

* bicycle: 10%
* motorbike: 15%
* parcel_car: 20%
* small_van: 30%
* large_van: 40%

For example, if the base price was 100, the `small_van` price with markup will be 130.
The vehicle should also be returned in the response, and the price should be rounded to the nearest integer.

Request:
```
{
  "pickup_postcode":   "SW1A1AA",
  "delivery_postcode": "EC2A3LT",
  "vehicle": "bicycle"
}
```
Response:
```
{
  "pickup_postcode":   "SW1A1AA",
  "delivery_postcode": "EC2A3LT"
  "vehicle": "bicycle"
  "price": 348
}
```

### 2) Variable prices by carrier

Now we need the list of prices per carrier for the given `pickup_postcode`, `delivery_postcode` and `vehicle`.

Use the JSON file  `src/data` folder to fetch the carrier data and calculate the price.
Bear in mind the carrier service should support the vehicle type. When calculating the price, add the service markup as well as the vehicle markup you have implemented in the earlier exercise to the carrier base price.

The `price_list` array needs to contain JSON objects sorted by `price`. And be stored in a database of your choosing (Postgres, redis, SQLLite, etc).

Example request:
```
{
  "pickup_postcode":   "SW1A1AA",
  "delivery_postcode": "EC2A3LT",
  "vehicle": "small_van"
}
```
Example response:
```
{
  "pickup_postcode":   "SW1A1AA",
  "delivery_postcode": "EC2A3LT"
  "vehicle": "small_van"
  "price_list": [
    {"service": "RoyalPackages", "price": 300, "delivery_time": 5}
    {"service": "Hercules", "price": 500, "delivery_time": 2},
  ]
}
```

### 3) (Optional) Secure this endpoint with TLS/ Https, or at least explain how you'd do it.
