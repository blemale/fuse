package com.github.blemale.fuse

import java.time.temporal.ChronoUnit.SECONDS
import java.time.{ Duration, Instant, ZoneId }

import com.github.blemale.fuse.TestKit._
import org.scalatest.{ ShouldMatchers, WordSpec }

class StateMachineSpec extends WordSpec with ShouldMatchers {

  val clock = TestClock(Instant.now(), ZoneId.systemDefault())

  "StateMachine" should {

    "start in close state" in {
      val stateMachine = new StateMachine(new Condition.FailureCount(1), Duration.of(1, SECONDS), clock)

      stateMachine.isExecutionAllowed shouldEqual true
    }

    "trip in open state when condition becomes false" in {
      val stateMachine = new StateMachine(new Condition.FailureCount(1), Duration.of(1, SECONDS), clock)

      stateMachine.accept(CallStatus.FAILURE)

      stateMachine.isExecutionAllowed shouldEqual false
    }

    "allow a trial call after cool down" in {
      val stateMachine = new StateMachine(new Condition.FailureCount(1), Duration.of(1, SECONDS), clock)

      stateMachine.accept(CallStatus.FAILURE)
      clock.plus(Duration.of(2, SECONDS))
      stateMachine.accept(CallStatus.OPEN)

      stateMachine.isExecutionAllowed shouldEqual true
      stateMachine.isExecutionAllowed shouldEqual false
    }

    "switch on close state when trial call succeed" in {
      val stateMachine = new StateMachine(new Condition.FailureCount(1), Duration.of(1, SECONDS), clock)

      stateMachine.accept(CallStatus.FAILURE)
      clock.plus(Duration.of(2, SECONDS))
      stateMachine.accept(CallStatus.OPEN)
      stateMachine.isExecutionAllowed
      stateMachine.accept(CallStatus.SUCCESS)

      stateMachine.isExecutionAllowed shouldEqual true
    }

    "switch off open state when trial call fail" in {
      val stateMachine = new StateMachine(new Condition.FailureCount(1), Duration.of(1, SECONDS), clock)

      stateMachine.accept(CallStatus.FAILURE)
      clock.plus(Duration.of(2, SECONDS))
      stateMachine.accept(CallStatus.OPEN)
      stateMachine.isExecutionAllowed
      stateMachine.accept(CallStatus.FAILURE)

      stateMachine.isExecutionAllowed shouldEqual false
    }

  }
}
