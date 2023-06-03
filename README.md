# AskMe Web

This is a fairly basic Spring Boot application that provides the web front end for the AskMe service.  Most of the logic is done in the `AskController`.

## Metrics

The AskMe services provide runtime metrics via the Spring Boot Actuator and [micrometer.io](https://micrometer.io), which is then scraped with [Prometheus](https://prometheus.io).  Since none of the other service have public IP addresses the AskMe web module provides HTTP endpoints via the `MetricsController` for Prometheus to scrape.

## Running

## Docker

A Docker immage can also be built and pushed to docker.lappsgrid.org.

``` 
$> docker run -p 80:8080 --name web docker.lappsgrid.org/lappsg
```