package com.github.blemale.fuse

import java.time.{ Clock, Duration, Instant, ZoneId }

object TestKit {

  class TestClock(startInstant: Instant, zoneId: ZoneId) extends Clock {
    private var _instant = startInstant

    override def instant(): Instant = _instant
    override def getZone: ZoneId = zoneId
    override def withZone(zone: ZoneId): Clock = new TestClock(_instant, zone)

    def plus(duration: Duration): Unit = {
      _instant = _instant.plus(duration)
    }

    def minus(duration: Duration): Unit = {
      _instant = _instant.minus(duration)
    }

    override def toString = s"MoveableClock($instant, $getZone)"
  }

  object TestClock {
    def apply(startInstant: Instant, zoneId: ZoneId): TestClock = new TestClock(startInstant, zoneId)
  }
}
