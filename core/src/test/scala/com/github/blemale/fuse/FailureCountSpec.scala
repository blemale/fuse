package com.github.blemale.fuse

import com.github.blemale.fuse.Condition.FailureCount
import org.scalatest.{ ShouldMatchers, WordSpec }

class FailureCountSpec extends WordSpec with ShouldMatchers {

  "FailureCount" should {
    "be true" when {
      "failure count is below threshold" in {
        val condition = new FailureCount(2)

        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.FAILURE)

        condition.isTrue shouldEqual true
      }
    }
    "be false" when {
      "failure count is equal to threshold" in {
        val condition = new FailureCount(2)

        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.FAILURE)
        condition.update(CallStatus.FAILURE)

        condition.isTrue shouldEqual false
      }
      "failure count is above threshold" in {
        val condition = new FailureCount(2)

        condition.update(CallStatus.SUCCESS)
        condition.update(CallStatus.FAILURE)
        condition.update(CallStatus.FAILURE)
        condition.update(CallStatus.FAILURE)

        condition.isTrue shouldEqual false
      }
    }

    "count call status open as failure" in {
      val condition = new FailureCount(2)

      condition.update(CallStatus.OPEN)
      condition.update(CallStatus.OPEN)

      condition.isTrue shouldEqual false
    }

    "reset the condition" in {
      val condition = new FailureCount(2)

      condition.update(CallStatus.FAILURE)
      condition.update(CallStatus.FAILURE)

      condition.reset()

      condition.update(CallStatus.FAILURE)

      condition.isTrue shouldEqual true
    }
  }
}
