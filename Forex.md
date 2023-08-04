# A local proxy for Forex rates

Build a local proxy for getting Currency Exchange Rates

__Requirements__

[Forex](../forex) is a simple application that acts as a local proxy for getting exchange rates. It's a service that can be consumed by other internal services to get the exchange rate between a set of currencies, so they don't have to care about the specifics of third-party providers.

We provide you with an initial scaffold for the application with some dummy interpretations/implementations. For starters, we would like you to try and understand the structure of the application, so you can use this as the base to address the following use case:

> An internal user of the application should be able to ask for an exchange rate between 2 given currencies, and get back a rate that is not older than 5 minutes.
The application should at least support 10.000 requests per day.

In practice, this should require the following 2 points:

1. Create a `live` interpreter for the `OneForge` service. This should consume the [1forge API](https://1forge.com/api), and do so using the [trial of the Starter package](https://1forge.com/pricing). Please use the `p` property from their response.

2. Adapt the `Rates` processes (if necessary) to make sure you cover the requirements of the use case, and work around possible limitations of the third-party provider.

3. Make sure the service's own API gets updated to reflect the changes you made in point 1 & 2.

The task is written using [ZIO](https://zio.dev/) library. Even if you are unfamiliar with it, the existing examples
should be enough for you to understand the basic usage and follow the patterns. In case you want to read a bit more about that,
please check this [summary](https://zio.dev/guides/), [effect creation](https://zio.dev/reference/core/zio/#creation) and about [layers](https://zio.dev/reference/di/dependency-injection-in-zio) that are a form of Dependency Injection.

The `ZIO` type is an IO-like effect type, to run/execute it manually if needed, use the [runtime](https://zio.dev/reference/core/runtime#running-a-zio-effect).

Some notes:
- Don't feel limited by the existing library dependencies; you can include others or drop something
- The interfaces provided act as an example/starting point. Feel free to add to improve or built on it when needed.
- The `Rates` service currently only use a single dependency. Don't feel limited, and do add others if you see fit.
- It's great for downstream users of the service (your colleagues) if the api returns descriptive errors in case something goes wrong.
- Feel free to update existing code in case you find it useful or needed.

Some traits/specifics we are looking for using this exercise:

- How can you navigate through an existing codebase;
- How easily do you pick up concepts, techniques and/or libraries you might not have encountered/used before;
- How do you work with third-party APIs that might not be (as) complete (as we would wish them to be);
- How do you work around restrictions;
- What design choices do you make;
- How do you think beyond the happy path.
