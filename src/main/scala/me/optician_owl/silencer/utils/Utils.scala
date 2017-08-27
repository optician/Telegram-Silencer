package me.optician_owl.silencer.utils

import java.time.{Instant, ZonedDateTime}
import java.util.TimeZone

object Utils {
  def zonedDateTime(epochSeconds: Int): ZonedDateTime =
    ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds),
                            TimeZone.getDefault.toZoneId)
}
