package com.github.blemale.fuse

import com.github.blemale.fuse.Condition.FailureRate
import org.scalatest.{ ShouldMatchers, WordSpec }

class FailureRateSpec extends WordSpec with ShouldMatchers {

  "FailureCount" should {
    "be true" when {
      "failure rate is below threshold" in {
        val condition = new FailureRate(0.5, 3)

        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.FAILURE)

        condition.isTrue shouldEqual true
      }
    }
    "be false" when {
      "failure rate is equal to threshold" in {
        val condition = new FailureRate(0.5, 2)

        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.FAILURE)

        condition.isTrue shouldEqual false
      }
      "failure count is above threshold" in {
        val condition = new FailureRate(0.5, 3)

        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.FAILURE)
        condition.update(CallStatus.FAILURE)

        condition.isTrue shouldEqual false
      }
    }

    "count call status open as failure" in {
      val condition = new FailureRate(0.5, 2)

      condition.update(CallStatus.OPEN)
      condition.update(CallStatus.OPEN)

      condition.isTrue shouldEqual false
    }

    "reset the condition" in {
      val condition = new FailureRate(0.5, 3)

      condition.update(CallStatus.FAILURE)
      condition.update(CallStatus.FAILURE)
      condition.update(CallStatus.FAILURE)

      condition.reset()

      condition.update(CallStatus.FAILURE)

      condition.isTrue shouldEqual true
    }

    "take into account only values in window" in {
      val condition = new FailureRate(0.5, 3)

      condition.update(CallStatus.FAILURE)
      condition.update(CallStatus.FAILURE)
      condition.update(CallStatus.FAILURE)

      condition.isTrue shouldEqual false

      condition.update(CallStatus.SUCCESS)
      condition.update(CallStatus.SUCCESS)

      condition.isTrue shouldEqual true
    }
  }

}
