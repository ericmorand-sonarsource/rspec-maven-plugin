# RSPEC Maven Plugin Project

> This is not the RSPEC Maven Plugin documentation. This is its project documentation.

## Prerequisites

* [Maven 3.9](https://maven.apache.org/index.html)
* Read permissions to Sonar Maven artifact repositories

## Usage

### Build

```shell
mvn clean install
```

### Build under an arbitrary version

```shell
mvn clean install -Drevision=X.X.X
```

## Philosophy

This project honors a strict interpretation of the Domain Driven design pattern that can be summarized like this:

* The Domain layer is not allowed to import from the other layers, or from any infrastructure-related package - for example `java.io` or `java.nio`.
* The Application layer is the interface between the foreign domains and the domestic one. It is not allowed to include any business-related logic. It is not allowed to import from any infrastructure-related package - for example `java.io` or `java.nio`.
* The Infrastructure layer is the interface between the application and the infrastructure that hosts it - aka the _Host_.