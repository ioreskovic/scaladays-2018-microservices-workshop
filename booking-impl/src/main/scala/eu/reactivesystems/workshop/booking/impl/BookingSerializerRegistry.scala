package eu.reactivesystems.workshop.booking.impl

import java.util.UUID

import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}

import scala.collection.immutable.Seq

object BookingSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    // State
    JsonSerializer[BookingRegisterState],
    // Commands and replies
    JsonSerializer[RequestBooking],
    // Events
    JsonSerializer[BookingRequested],
    // Other
    JsonSerializer[UUID]
  )
}
