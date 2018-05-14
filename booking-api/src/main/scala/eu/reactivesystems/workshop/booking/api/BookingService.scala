package eu.reactivesystems.workshop.booking.api

import java.util.UUID

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.transport.Method
import com.lightbend.lagom.scaladsl.api.{Service, ServiceCall}

/**
  * The booking service.
  */
trait BookingService extends Service {

  def healthCheck(): ServiceCall[NotUsed, String]

  def requestBooking(roomId: UUID): ServiceCall[BookingRequest, UUID]

  def cancelBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def confirmBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def rejectBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def withdrawBooking(roomId: UUID, bookingId: UUID): ServiceCall[NotUsed, Done]

  def listRoom(roomId: UUID): ServiceCall[NotUsed, Done]

  def unlistRoom(roomId: UUID): ServiceCall[NotUsed, Done]

  final override def descriptor = {
    import Service._

    named("booking")
      .withCalls(
        restCall(Method.GET, "/api/bookings/healthCheck", healthCheck _),
        restCall(Method.GET, "/api/room/:roomId/bookings/:bookingId/confirm", confirmBooking _),
        restCall(Method.POST, "/api/room/:roomId/request", requestBooking _),
        restCall(Method.DELETE, "/api/room/:roomId/bookings/:bookingId/cancel", cancelBooking _),
        restCall(Method.GET, "/api/room/:roomId/bookings/:bookingId/reject", rejectBooking _),
        restCall(Method.GET, "/api/room/:roomId/bookings/:bookingId/withdraw", withdrawBooking _),
        restCall(Method.GET, "/api/room/:roomId/list", listRoom _),
        restCall(Method.DELETE, "/api/room/:roomId/unlist", unlistRoom _)
      )
      .withAutoAcl(true)
  }
}
