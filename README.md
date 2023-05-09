# STTP ZIO Redis Request Caching

Example of building a http client request caching backed by Redis using ZIO stack with the Sttp client.

* [STTP w/ ZIO](https://sttp.softwaremill.com/en/stable/backends/zio.html)
* [ZIO](https://github.com/zio/zio)
* [ZIO-Redis](https://github.com/zio/zio-redis)
* [ZIO-JSON](https://github.com/zio/zio-json)
* [ZIO-Schema](https://github.com/zio/zio-schema)

This is straightforward example ZIO service that takes in an STTP request and caches the body using the uri as cache key.

[The service code.](https://github.com/ianlintner/sttp-zio-redis/blob/main/src/main/scala/SttpRequestCaching.scala#L18)

## Example Dependencies
To run the example you will need to use the `docker-compose up` to bring up the redis server.


## Run the example
```
sbt run
```

Output should show the same message for 3 runs even though they are delayed by a 1 second.
```
timestamp=2023-05-09T18:54:14.336568Z level=INFO thread=#zio-fiber-15 message="Connected to the redis server with address localhost/127.0.0.1:6379." location=zio.redis.RedisConnectionLive.openChannel file=RedisConnectionLive.scala line=126
timestamp=2023-05-09T18:54:14.790617Z level=INFO thread=#zio-fiber-4 message="2023-05-09T13:01:44.881126-05:00" location=<empty>.Main.run file=Main.scala line=26
timestamp=2023-05-09T18:54:15.803653Z level=INFO thread=#zio-fiber-4 message="2023-05-09T13:01:44.881126-05:00" location=<empty>.Main.run file=Main.scala line=28
timestamp=2023-05-09T18:54:16.814158Z level=INFO thread=#zio-fiber-4 message="2023-05-09T13:01:44.881126-05:00" location=<empty>.Main.run file=Main.scala line=30
```
