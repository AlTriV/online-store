# Online Store v2

## Introduction

This project is a simple reactive web application based on Spring Boot using reactive stack (Spring WebFlux, Spring Data R2DBC)
, that implements a small online store.


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

To add products to the showcase, you can use the admin panel at http://localhost:8080/store/admin