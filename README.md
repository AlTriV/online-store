# Online Store v3

## Introduction

This project is a simple reactive web application based on Spring Boot using reactive stack (Spring WebFlux, Spring Data R2DBC),
that implements a small online store.
The project consist of 3 modules:
* [payment-service](payment-service) - 
small service with simple logic for purchasing orders, implementing [payment-service-spec.yaml](api/payment-service-spec.yaml)
* [payment-client-starter](payment-client-starter) - simple client to simplify interaction with the payment-service, generated
using OpenAPI generator gradle plugin and based on [payment-service-spec.yaml](api/payment-service-spec.yaml)
* [store-service](store-service) - main store logic service


## Features

* Add/delete items to the shopping cart
* View item details
* Browse the shopping cart
* View the order history and each order details

## App build requirements

* JDK 21
* Docker
* Gradle 8.13 (optional)

## App start requirements

First of all build executable jar file with gradle:

``` 
./gradlew clean test bootJar
```

From the project folder '/docker' run the command:

``` dockerfile
docker compose up
```

Online store application will start on http://localhost:8080/store address.

Payment service will start on http://localhost:8081/payment.

To add products to the showcase, you can use the admin panel at http://localhost:8080/store/admin

To add credits on wallet POST request to http://localhost:8081/payment/add. Body example:
```json
{
	"creditsAmount": 12500
}
```