# Junior Back-End Engineer Interview challenge

Kensu Inc. coding challenge for backend developers

**Here you go:**
* Read instructions below carefully
* Send us your submission through a Github repository
* Make sure you put a README file with detailed instructions and all assumptions to run your code
* Ideally your solution will have a Dockerfile or a Docker Compose file to make run possible with all the external dependencies
* When you think you finished with all the tasks send us an email with your submission link.

**Task 1:**
We want to write a simple service called JSON transformer.
The json transformer service is a web service  accepts a JSON payload (via an HTTP PUT or HTTP POST action) and performs an action on it based on the endpoint the payload is received on.  The JSON transformer service has three endpoints

| Endpoint        | Function           | HTTP Verb  |
| ------------- |-------------| -----|
| `/alpha`     | Alphabetize (orders alphabetically) the keys in the request JSON payload, load the request into a database and returns the resulting JSON back in the HTTP response | `PUT` |
| `/flatten`      | Flatten any JSON Arrays in the request JSON payload (comma separated) such that the resulting JSON does not contain any JSON Arrays        |   `POST` |
| `/status`      | Obtains the health status of the system and responds with the details in the HTTP response | `GET` |

## Examples
### /alpha
**input**
`HTTP PUT /alpha`
```
{
  "fruit":"apple",
  "animal":"zebra",
  "city-list":["sunnyvale","sanjose"]
}
```
**output**
```
{
  "animal":"zebra",
  "city-list":["sunnyvale","sanjose"],
  "fruit":"apple"
}
```

### /flatten
**input**
`HTTP PUT /flatten`
```
{
  "fruit":"apple",
  "animal":"zebra",
  "city-list":["sunnyvale","sanjose"]
}
```
**output**
```
{
  "fruit":"apple",
  "animal":"zebra",
  "city-list":"sunnyvale,sanjose"
}
```

### /status
**input**
`HTTP GET /status`

**output**
```
{
  "mem-used-pct" : 83.6,
  "disc-space-avail" : [
                         { "discname" : "/dev/SDA1", "availbytes" : "12345000"},
                         { "discname" : "/dev/SDA2", "availbytes" : "12345000"}
                       ],
  "cpu-used-pct" : 55.0
}
```

**Task 2:**

Our microservice architecture has a service to create and manage products. We now would like to add another microservice that offers search and filter capabilities for products. To quickly provide a working endpoint to our frontend team, we agreed on the following details.

## Read Data from a database, of your choice. Use Docker to set up the database

The service should connect to database store, where it can read products with the following structure:

```JSON
{
	"id": 1,
	"name": "Product A",
	"price": 12.99,
	"brand": "Brand A",
	"onSale": true
}
```
Pre-load database with at least 10 products.

Expected Response
-
Our Frontend Team expects a response matching the following requirements

- All products are returned
- Products are grouped by `brand`, sorted alphabetically
- Property `brand` should be omitted on products
- Products inside a `brand` should be sorted ascending by `price`
- Property `onSale` should be converted to a property `event` of type String with the value "ON SALE"

```JSON
{
	"Brand A" : [{
		"id": 1,
		"name": "Product A",
		"price": 12.99,
		"event": "ON SALE"
	},
	{
		"id": 2,
		"name": "Product B",
		"price": 7.99
	}],
	"Brand B" : [{
		"id": 3,
		"name": "Product C",
		"price": 14.99
	},
	{
		"id": 4,
		"name": "Product D",
		"price": 10.99
	}]
}
```

# Technical Requirement  

We expect:  
 - A Scala based solution, ideally Akka-http
 - That has a Docker container ready to run
 - with tests verifying the given requirements
 - and a versioned code base

## Freedom of Tools and Technology

You can use, if you go the Akka-http solution this template [Akka Http Project Skeleton] (https://github.com/akka/akka-http-quickstart-scala.g8),
however **we suggest you to use the tools and technologies you are comfortable with**.

You need to implement these two tasks in two separate microservices.

Required artifacts:
- Code with test cases.
- Readme.md file for installation and running.
- Dockerfile or/and Docker Compose
- API Documentation. (Swagger would be nice to have).
- A postman collection to test all implemented API Endpoints (Would be nice to have)
