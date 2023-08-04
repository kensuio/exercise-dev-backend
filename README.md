# KENSU Challenge Solution (Forex) - Michael Bauer

This repository contains code representing my proposed solution to the challenge of implementing a service to fetch currency exchange rates from the 1forge API and provide them via an API which serves data that is stale by no more than 5 minutes and enables a higher number of requests per 24h cycle than the 1forge API under the free plan.

## Solution

The service to fetch rates from the 1forge API was implemented using ZIO, ZIO-http, and ZIO-json. The response of the external endpoint is modeled in `src/main/scala/forex/services/oneforge/OneForgeResponse.scala`. 

To enable the required number of requests per 24h-cycle, three techniques were used:

1. **Caching**
Here, ZIO-cache would have been the initial choice. But a further optimization required an in-memory cache where out-of-band lookups/recaching can be triggered, which ZIO-cache does not support. Thus, a similar in-memory cache with extended functionality was built in `src/main/scala/forex/utils/cache/InMemoryCache.scala`. This has a definable capacity, the ability to trigger out-of-band recaching, supports TTL and evicts the least-recently-used elements first. Based on this, utility-methods for creating and managing rates-cache have been defined in `src/main/scala/forex/services/oneforge/OneForgeRatesCache.scala`. The TTL is configurable via the `cacheTTL` value in the `OptimizationsConfig`.

2. **Iterated out-of-band recaching**
Since this proxy-service only supports a handful of currencies, a very impactful optimization is possible: As a background process scheduled to run with the `cacheTTL` interval, we generate all pairs of all handled currencies, make a batched lookup and cache the results out-of-band. Thus, all requests to our proxy service can be handled without in-band lookup at all. This optimization is configured via the `fetchAll` value in the `OptimizationsConfig`. **WARNING:** Depending on optimiztion 3, this attempts a batch-request with *n-choose-2* or *2\*(n-choose-2)* currency-pairs for *n* handled currencies.

3. **Canonicalized ordering of currency pairs**
Since this proxy service may serve data that is stale for up to 5 minutes, we already accept a certain deviation of the rate we output from the actual currently known trading rate at 1forge. Thus, we may also accept reasonably small errors from optimizations. By canonically ordering currency-pairs for lookup and caching and then inverting the known rate when users ask for inverted pairs, we can halve the sizes of backing-api responses and cache. This optimization is configured via the `canonicalize` value in the `OptimizationsConfig`.

## Unhappy paths

Special care was taken to handle unhappy paths, mostly in virtue of applying two techniques:

1. Defining meaningful error-types with descriptive messages (subtypes of `OneForgeError` and `RatesError` as well as the transformation of the latter to HTTP error responses)
2. Configurable retries for in-band and out-of-band lookups (configured via the `retryLookupConfig` and `retryFetchAllConfig` values in the `OptimizationsConfig`)

## Running the service

Nothing was changed in how the application runs - `Main` remains the main class with a `run` method. It can be started via `sbt run` or in your IDE of choice.


