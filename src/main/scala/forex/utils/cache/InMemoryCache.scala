package forex.utils.cache

import zio._
import java.time.OffsetDateTime
import scala.reflect.ClassTag

/**
 *  A concurrency-safe in-memory cache with a fixed capacity, optional time-to-live and the affordance of out-of-bands cache updates.
 *  Eviction happens on a least-recently-used basis.
 * 
 *  Note: ZIO-cache would be a good solution if we didn't want to support an optimization which requires us
 *  to cache values explicitly and directly instead of as a side-effect of a `get` operation.
 *  Since this is not supported by ZIO-cache, we provide a custom implementation.
 */
class InMemoryCache[Key, Error <: Throwable, Value](
  lookup: (Seq[Key]) => IO[Error, Seq[Option[Value]]],
  capacity: Int,
  timeToLive: Option[Duration],
  ref: Ref[Map[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])]]
)(implicit et: ClassTag[Error]) {

  private def getStalenessDateTime(timeToLive: Option[Duration], lastFetched: OffsetDateTime): Option[OffsetDateTime] =
    timeToLive.map(ttl => lastFetched.plusNanos(ttl.toNanos))

  private def cleanupStaleEntries(now: OffsetDateTime): ZIO[Any, Nothing, Unit] =
    ref.update { cache =>
      withoutStaleEntries(now, cache)
    }.unit
  
  private def withoutStaleEntries(now: OffsetDateTime, map: Map[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])]): Map[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])] =
    map.filter { case (_, (_, _, staleDateTime)) => staleDateTime.forall(now.isBefore) }

  // Takes min of `capacity` and `n`, cleans up stale entries, then evicts the least recently accessed entries
  // (or random entries if none are accessed) until the min of `capacity` and `n` is free.
  // Attempts to preserve the entries with the keys in `tryToPreserve`, but will evict them 
  // if we cannot create the required capacity otherwise.
  private def createCapacity(n: Int, tryToPreserve: Seq[Key]): ZIO[Any, Nothing, Map[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])]] = {
    var keysBeingPreserved: Seq[Key]  = Seq.empty
    (for {
      now <- Clock.currentDateTime
      newMap   <-  ref.updateAndGet { cache => withFreedCapacity(n, tryToPreserve, now, cache) }
    } yield (newMap)).uninterruptible
  }

  private def withFreedCapacity(n: Int, tryToPreserve: Seq[Key], now: OffsetDateTime, map: Map[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])]): Map[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])] = {
    val mapWithoutStaleEntries = withoutStaleEntries(now, map)
    val numberCurrentlyFilled = mapWithoutStaleEntries.size
    val numberCurrentlyFree  = capacity - numberCurrentlyFilled
    val numberToFree = Math.min(capacity, n) - numberCurrentlyFree
    val numberToPreserve = Math.min(numberCurrentlyFilled - numberToFree, tryToPreserve.size)
    val keysToPreverse = tryToPreserve.take(numberToPreserve)
    val filteredEntries = 
      map
      .filter(entry => !keysToPreverse.contains(entry._1))
      .toSeq
      // This ensures we evict the least recently used entries
      .sortBy { case (_, (_, lastAccessTime, _)) => lastAccessTime }
    val entriesToEvict = filteredEntries.takeRight(numberToFree)
    map -- entriesToEvict.map(_._1)
  }

  private def remove(keys: Seq[Key]): ZIO[Any, Nothing, Unit] =
    ref.update { cache =>
      cache -- keys
    }.unit

  private def catchLookupThrowable[T <: Throwable](keys: Seq[Key], t: T): ZIO[Any, Error, Seq[Option[Value]]] = t match {
    // If the error is of the configured type, fail the operation
    case e: Error => ZIO.fail(e)
    // Otherwise, just ignore the error and return None
    case _        => ZIO.succeed(Seq.fill(keys.size)(None))
  }

  /**
   * Will maximally cache `capacity` entries, and will evict the least recently used entries first.
   */
  def cache(map: Map[Key, Value]): ZIO[Any, Nothing, Unit] =
    {
      val mapToCache = map.take(capacity)
      (for {
        now                  <- Clock.currentDateTime
        alreadyCached        <- ref.get.map(_.filter { case (key, _) => mapToCache.contains(key) })
        (toUpdate, toInsert)  = mapToCache.partition { case (key, _) => alreadyCached.contains(key) }
                                // Will never evict already cached entries, since we can gaurantee that
                                // the entire list of keys to preverve can be preserved
                                // (because `capacity`-`toInsert.size` - `alreadyCached.size` >= 0, 
                                // which is guaranteed by restricting `mapToCache` to have at most `capacity` entries).
        _                    <- createCapacity(toInsert.size, toUpdate.keys.toSeq).flatMap(newMap => {
                                  ref.update { cache =>
                                    val mapWithUpdate = toUpdate.foldLeft(newMap) { (localCache1, tuple) =>
                                      val (key, value)           = tuple
                                      val (_, lastAccessTime, _) = localCache1.getOrElse(key, (value, None, None))
                                      val newStaleDateTime       = getStalenessDateTime(timeToLive, now)
                                      val newEntry               = (value, lastAccessTime, newStaleDateTime)
                                      localCache1 + (key -> newEntry)
                                    }

                                    val mapWithUpdateAndInsert = toInsert.foldLeft(mapWithUpdate) { (localCache2, tuple) =>
                                      val (key, value)     = tuple
                                      val newStaleDateTime = getStalenessDateTime(timeToLive, now)
                                      val newEntry         = (value, None, newStaleDateTime)
                                      localCache2 + (key -> newEntry)
                                    }
                                    cache ++ mapWithUpdateAndInsert
                                  }
                                })
      } yield ()).uninterruptible
    }

  def cacheWithLookup(keys: Seq[Key]): ZIO[Any, Error, Unit] =
    for {
      now     <- Clock.currentDateTime
      result  <- lookup(keys).catchSome(e => catchLookupThrowable(keys, e))
      zipped   = keys.zip(result)
      found    = zipped.filter(_._2.isDefined).map(tuple => (tuple._1, tuple._2.get))
      asMap    = found.toMap
      _       <- if (asMap.nonEmpty) this.cache(asMap) else ZIO.unit
    } yield ()

  def recacheStale(): ZIO[Any, Error, Unit] =
    for {
      now    <- Clock.currentDateTime
      result <- ref.get.map(_.filter { case (_, (_, _, staleDateTime)) => staleDateTime.exists(_.isBefore(now)) })
      _      <- cacheWithLookup(result.keys.toSeq)
    } yield ()

  def recacheAll(): ZIO[Any, Error, Unit] =
    for {
      now <- Clock.currentDateTime
      map <- ref.get
      _   <- cacheWithLookup(map.keys.toSeq)
    } yield ()

  def get(key: Key): ZIO[Any, Error, Option[Value]] =
    for {
      now    <- Clock.currentDateTime
      // Try from cache. If not found, try fetching (and re-caching) the value
      result <- ref.get.map(_.get(key)).flatMap {
                  case Some((value, _, staleDateTime)) if staleDateTime.exists(_.isAfter(now)) =>
                    ZIO.succeed(Some(value)).tap(
                      // Update with new last access time
                      _ =>
                        ref.update { cache =>
                          val newEntry = (value, Some(now), staleDateTime)
                          cache + (key -> newEntry)
                        }
                    )

                  case _ =>
                    lookup(Seq(key))
                      .catchSome(e => catchLookupThrowable(Seq(key), e))
                      .map(_.head)
                      .tap(value =>
                        if (value.isDefined)
                          ref.update { cache =>
                            val newEntry = (value.get, Some(now), getStalenessDateTime(timeToLive, now))
                            cache + (key -> newEntry)
                          }
                        else
                          ZIO.unit
                      )
                }
    } yield result

  def get(keys: Seq[Key]): ZIO[Any, Error, Seq[Option[Value]]] =
    for {
      now                       <- Clock.currentDateTime
      cachedAndFresh            <- ref.get.map(_.filter {
                                     case (key, (_, _, staleDateTime)) =>
                                       staleDateTime.forall(_.isAfter(now)) && keys.contains(key)
                                   })
      (cachedKeys, uncachedKeys) = keys.partition(cachedAndFresh.contains)
      fetched                   <- lookup(uncachedKeys).catchSome(e => catchLookupThrowable(keys, e))
      keyValuePairs              = uncachedKeys.zip(fetched)
      foundPairs                 = keyValuePairs.filter(_._2.isDefined)
      asMap                      = foundPairs.map(tuple => (tuple._1, tuple._2.get)).toMap
      
      _                         <- cache(asMap)
      // Update last access time for cached entries
      _                         <- ref.update { cache =>
                                     cachedAndFresh.foldLeft(cache) { (localCache, tuple) =>
                                       val (key, (value, _, staleDateTime)) = tuple
                                       val newEntry                         = (value, Some(now), staleDateTime)
                                       localCache + (key -> newEntry)
                                     }
                                   }
      // Map the original seq of keys to a seq of optional values
      result                     = keys.map(key => cachedAndFresh.get(key).map(_._1) orElse asMap.get(key))
    } yield result

}

object InMemoryCache {

  type Lookup[Key, Error <: Throwable, Value] = (Seq[Key]) => ZIO[Any, Error, Seq[Option[Value]]]

  def make[Key, Error <: Throwable, Value](
    capacity: Int,
    timeToLive: Option[Duration],
    lookup: Lookup[Key, Error, Value]
  )(implicit t: ClassTag[Error]): ZIO[Any, Nothing, InMemoryCache[Key, Error, Value]] =
    for {
      ref <- Ref.make(Map.empty[Key, (Value, Option[OffsetDateTime], Option[OffsetDateTime])])
    } yield new InMemoryCache[Key, Error, Value](lookup, capacity, timeToLive, ref)
}
