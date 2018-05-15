package eu.reactivesystems.workshop.booking.impl

import java.time.LocalDate
import java.util.UUID

import scala.concurrent.duration._
import akka.actor.ActorSystem
import com.lightbend.lagom.scaladsl.playjson.JsonSerializerRegistry
import com.lightbend.lagom.scaladsl.testkit.PersistentEntityTestDriver
import eu.reactivesystems.workshop.booking.api.BookingRequest
import org.scalactic.ConversionCheckedTripleEquals
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Await

class BookingRegisterSpec extends WordSpecLike with Matchers with BeforeAndAfterAll with ConversionCheckedTripleEquals {
  val system = ActorSystem("PostSpec", JsonSerializerRegistry.actorSystemSetupFor(BookingSerializerRegistry))

  override def afterAll(): Unit = {
    Await.ready(system.terminate, 10.seconds)
  }

  "BookingRegister entity".must {
    "handle RequestBooking event in Listed state" in {
      val driver = new PersistentEntityTestDriver(system, new BookingRegister, "room-1-register")
      val bookingRequest = BookingRequest(
        UUID.randomUUID(),
        LocalDate.now().plusDays(1),
        3,
        2
      )

      val outcome = driver.run(RequestBooking(bookingRequest))

      val uuid = outcome.replies.head.asInstanceOf[UUID]

      outcome.events.head match {
        case BookingRequested(bookingId, guestId, startingDate, duration, numberOfGuests) => {
          bookingId should not be null
          guestId should ===(bookingRequest.guest)
          startingDate should ===(bookingRequest.startingDate)
          duration should ===(bookingRequest.duration)
          numberOfGuests should ===(bookingRequest.numberOfGuests)
        }
        case _ => fail
      }

      outcome.state.status should ===(BookingRegisterStatus.Listed)
      outcome.state.requestedBookings.size should ===(1)
      outcome.replies.headOption match {
        case Some(bId) => bId should be(a[UUID])
        case _ => fail
      }

      outcome.issues should be(empty)
    }
  }
}
