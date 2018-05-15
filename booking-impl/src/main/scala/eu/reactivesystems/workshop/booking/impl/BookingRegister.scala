package eu.reactivesystems.workshop.booking.impl

import java.time.LocalDate
import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import eu.reactivesystems.workshop.booking.api.{BookingCancelled, BookingCreated, BookingRequest}
import eu.reactivesystems.workshop.jsonformats.JsonFormats._
import play.api.libs.json.{Format, Json}

/**
  */
class BookingRegister extends PersistentEntity {

  override type State = BookingRegisterState
  override type Command = BookingRegisterCommand
  override type Event = BookingRegisterEvent

  override def initialState: BookingRegisterState = BookingRegisterState(BookingRegisterStatus.Listed)


  override def behavior: Behavior = {
    case BookingRegisterState(BookingRegisterStatus.Unlisted, _) => unlisted
    case BookingRegisterState(BookingRegisterStatus.Listed, _) => listed

    case _ => listed
  }

  private def cancelAction: Actions = {
    Actions().onCommand[CancelBooking, Done] {
      case (CancelBooking(id), ctx, state) => {
        state.requestedBookings.find(_.id == id).fold {
          ctx.invalidCommand(s"No such booking: $id")
          ctx.done
        }({ booking => ctx.thenPersist(BookingCancelled(booking.id))(_ => ctx.reply(Done))})
      }
    }.onEvent {
      case (BookingCancelled(id), state) => {
        state.copy(requestedBookings = state.requestedBookings.filterNot(_.id == id))
      }
    }
  }

  /**
    * Behavior for the not created state.
    */
  private def unlisted = Actions().onCommand[ListRoom.type, Done] {
    case (ListRoom, ctx, state) => ctx.thenPersist(RoomListed)(event => ctx.reply(Done))
  }.onCommand[UnlistRoom.type, Done] {
    case (UnlistRoom, ctx, state) => {
      ctx.reply(Done)
      ctx.done
    }
  }.orElse(cancelAction)

  private def listed = Actions().onCommand[UnlistRoom.type, Done] {
    case (UnlistRoom, ctx, state) =>ctx.thenPersist(RoomUnlisted)(event => ctx.reply(Done))
  }.onEvent {
    case (RoomUnlisted, state) => BookingRegisterState(BookingRegisterStatus.Unlisted)
  }.onCommand[RequestBooking, UUID] {
    case (RequestBooking(request), ctx, state) if request.startingDate.isAfter(LocalDate.now()) => {
      ctx.thenPersist(BookingRequested(
        UUID.randomUUID(),
        request.guest,
        request.startingDate,
        request.duration,
        request.numberOfGuests
      ))(event => ctx.reply(event.bookingId))
    }
    case (_, ctx, state) => {
      ctx.invalidCommand("Booking date has to be in future")
      ctx.done
    }
  }.onEvent {
    case (BookingRequested(bId, gId, sd, d, n), state) => state.copy(
      requestedBookings = Booking(bId, sd, sd.plusDays(d)) :: state.requestedBookings
    )
  }.orElse(cancelAction)
    .onCommand[ConfirmBooking, Done] {
    case (ConfirmBooking(id), ctx, state) => ???
  }.onCommand[RejectBooking, Done] {
    case (RejectBooking(id), ctx, state) => ???
  }.onCommand[WithdrawBooking, Done] {
    case (WithdrawBooking(id), ctx, state) => ???
  }

}


/**
  * The state.
  */
case class BookingRegisterState(status: BookingRegisterStatus.Status, requestedBookings: List[Booking] = Nil)

object BookingRegisterState {
  implicit val format: Format[BookingRegisterState] = Json.format
}

object Booking {
  implicit val format: Format[Booking] = Json.format
}

case class Booking(id: UUID, startingDate: LocalDate, endDate: LocalDate) {
  def fits(other: Booking): Boolean = {
    this.endDate.isBefore(other.startingDate) || other.endDate.isBefore(this.startingDate)
  }
}

/**
  * Status.
  */
object BookingRegisterStatus extends Enumeration {
  type Status = Value
  val Listed: Status = Value
  val Unlisted: Status = Value

  implicit val format: Format[Status] = enumFormat(BookingRegisterStatus)
}

/**
  * A command.
  */
sealed trait BookingRegisterCommand

object RequestBooking {
  implicit val format: Format[RequestBooking] = Json.format
}

case class RequestBooking(request: BookingRequest) extends BookingRegisterCommand with ReplyType[UUID]
case class CancelBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class ConfirmBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class RejectBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class WithdrawBooking(bookingId: UUID) extends BookingRegisterCommand with ReplyType[Done]
case class ModifyBooking(bookingId: UUID, request: BookingRequest) extends BookingRegisterCommand with ReplyType[Done]
case object UnlistRoom extends BookingRegisterCommand with ReplyType[Done]
case object ListRoom extends BookingRegisterCommand with ReplyType[Done]

/**
  * A persisted event.
  */
trait BookingRegisterEvent extends AggregateEvent[BookingRegisterEvent] {
  override def aggregateTag: AggregateEventTagger[BookingRegisterEvent] = BookingRegisterEvent.Tag
}

case object RoomListed extends BookingRegisterEvent
case object RoomUnlisted extends BookingRegisterEvent

object BookingRequested {
  implicit val format: Format[BookingRequested] = Json.format
}

case class BookingRequested(bookingId: UUID, guestId: UUID, startingDate: LocalDate, duration: Int, numberOfGuests: Int) extends BookingRegisterEvent

object BookingRegisterEvent {
  val Tag = AggregateEventTag[BookingRegisterEvent]
}
