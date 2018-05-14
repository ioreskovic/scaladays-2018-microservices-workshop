package eu.reactivesystems.workshop.booking.impl

import java.util.UUID

import akka.Done
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, AggregateEventTagger, PersistentEntity}
import eu.reactivesystems.workshop.booking.api.{BookingRequest}
import eu.reactivesystems.workshop.jsonformats.JsonFormats._
import play.api.libs.json.{Format, Json}

/**
  */
class BookingRegister extends PersistentEntity {

  override type State = BookingRegisterState
  override type Command = BookingRegisterCommand
  override type Event = BookingRegisterEvent

  override def initialState: BookingRegisterState = BookingRegisterState(BookingRegisterStatus.NotCreated)


  override def behavior: Behavior = {
    case BookingRegisterState(BookingRegisterStatus.NotCreated) => unlisted
  }

  /**
    * Behavior for the not created state.
    */
  private def unlisted = Actions().onCommand[ListRoom.type, Done] {
    case (ListRoom, ctx, state) => {
      ctx.thenPersist(RoomListed)(event => ctx.reply(Done))
    }
  }

}


/**
  * The state.
  */
case class BookingRegisterState(status: BookingRegisterStatus.Status)

object BookingRegisterState {
  implicit val format: Format[BookingRegisterState] = Json.format
}

/**
  * Status.
  */
object BookingRegisterStatus extends Enumeration {
  type Status = Value
  val NotCreated = Value

  implicit val format: Format[Status] = enumFormat(BookingRegisterStatus)
}

/**
  * A command.
  */
sealed trait BookingRegisterCommand

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

object BookingRegisterEvent {
  val Tag = AggregateEventTag[BookingRegisterEvent]
}
