# Todos

## Unit Tests

Add a full UTs coverage.

Prerequisites:
* Encapsulate dependencies to Disruptor in order to provide a sync implementation for testing.

## Benchmarks

Add jmh benchmarks against baseline, akka circuit-breaker, ...

Prerequisites:
* Separate build in core and benchmark sbt modules

## Add state listeners

Add possibility to listen to circuit breaker state changes

Async or Synchronous calls ? Reuse ring buffer ?

## Add scala API

## Share Disruptor

Share disruptor between multiple circuit breakers ?

## Reviews

Ask peers for review (Brice D., Olivier B., ...)

