package com.github.blemale.fuse

import java.time.temporal.ChronoUnit.SECONDS
import java.time.{ Duration, Instant, ZoneId }
import java.util.Optional
import java.util.concurrent.{ CompletableFuture, CountDownLatch, TimeUnit }
import java.util.function.Supplier

import com.github.blemale.fuse.CircuitBreaker.CircuitBreakerOpenException
import com.github.blemale.fuse.TestKit.TestClock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ ShouldMatchers, WordSpec }

import scala.compat.java8.FunctionConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.Future
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

class CircuitBreakerSpec extends WordSpec with ShouldMatchers with ScalaFutures {
  import CircuitBreakerSpec._

  val clock = TestClock(Instant.now(), ZoneId.systemDefault())

  "CircuitBreaker" when {
    "using sync API" should {
      "allow execution when condition true" in {
        val latch = new CountDownLatch(2)

        val circuitBreaker =
          new CircuitBreaker(
            new Condition.FailureCount(2),
            Duration.of(1, SECONDS),
            clock,
            Optional.of(latch)
          )

        Try(circuitBreaker.execute(asJava(() => throw new RuntimeException)))
        Try(circuitBreaker.execute(asJava(() => "foo")))

        latch.await(1, TimeUnit.SECONDS) shouldEqual true

        circuitBreaker.execute(asJava(() => "bar")) shouldEqual "bar"
      }

      "trip when condition became false" in {
        val latch = new CountDownLatch(2)

        val circuitBreaker =
          new CircuitBreaker(
            new Condition.FailureCount(2),
            Duration.of(1, SECONDS),
            clock,
            Optional.of(latch)
          )

        Try(circuitBreaker.execute(asJava(() => throw new RuntimeException)))
        Try(circuitBreaker.execute(asJava(() => throw new RuntimeException)))

        latch.await(1, TimeUnit.SECONDS) shouldEqual true

        an[CircuitBreakerOpenException] should be thrownBy circuitBreaker.execute(asJava(() => "foo"))
      }
    }
    "using async API" should {
      "allow execution when condition true" in {
        val latch = new CountDownLatch(2)

        val circuitBreaker =
          new CircuitBreaker(
            new Condition.FailureCount(2),
            Duration.of(1, SECONDS),
            clock,
            Optional.of(latch)
          )

        circuitBreaker.executeAsync(asJavaAsync(() => Future.failed(new RuntimeException)))
        circuitBreaker.executeAsync(asJavaAsync(() => Future("foo")))

        latch.await(1, TimeUnit.SECONDS) shouldEqual true

        circuitBreaker.execute(asJavaAsync(() => Future("bar"))).toScala.futureValue shouldEqual "bar"
      }

      "trip when condition became false" in {
        val latch = new CountDownLatch(2)

        val circuitBreaker =
          new CircuitBreaker(
            new Condition.FailureCount(2),
            Duration.of(1, SECONDS),
            clock,
            Optional.of(latch)
          )

        circuitBreaker.executeAsync(asJavaAsync(() => Future.failed(new RuntimeException)))
        circuitBreaker.executeAsync(asJavaAsync(() => Future.failed(new RuntimeException)))

        latch.await(1, TimeUnit.SECONDS) shouldEqual true

        circuitBreaker.executeAsync(asJavaAsync(() => Future("foo"))).toScala.failed.futureValue shouldBe a[CircuitBreakerOpenException]
      }
    }
  }
}

object CircuitBreakerSpec {
  def asJava[T](f: () => T): Supplier[T] = asJavaSupplier(f)
  def asJavaAsync[T](f: () => Future[T]): Supplier[CompletableFuture[T]] = asJavaSupplier(() => f().toJava.toCompletableFuture)
}
