package ru.citeck.ecos.notifications.lib.icalendar

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.Property
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Cn
import net.fortuna.ical4j.model.parameter.PartStat
import net.fortuna.ical4j.model.parameter.Role
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.TimeZones
import java.net.URI
import java.time.Instant
import java.util.*

class CalendarEvent(
    private val uid: String,
    private val sequence: Int,
    private val prodId: String,
    private val method: Method,
    private val summary: String,
    private val description: String?,
    private val startDate: Instant,
    private val durationInMillis: Long,
    private val organizer: String?,
    private val attendees: Set<String>,
    private val createDate: Instant?,
    private val timeZone: TimeZone
) {
    companion object {
        private const val FILE_NAME = "invite.ics"
        private const val FILE_EXT = "ics"
        private const val FILE_MIME_TYPE = "text/calendar"
        private const val DEFAULT_PROD_ID = "Citeck"
    }

    fun createAttachment(): CalendarEventAttachment {

        val end = startDate.plusMillis(durationInMillis)

        val startDateTime = DateTime(startDate.toEpochMilli())
        startDateTime.timeZone = timeZone

        val endDateTime = DateTime(end.toEpochMilli())
        endDateTime.timeZone = timeZone

        val event = VEvent(startDateTime, endDateTime, summary)
        createDate?.let {
            val dtStamp = event.getProperty<DtStamp>(Property.DTSTAMP)
            dtStamp.dateTime = DateTime(it.toEpochMilli())
        }
        description?.let {
            event.withProperty(Description(it))
        }

        event.withProperty(Uid(uid))
        event.withProperty(Sequence(sequence))

        if (method == Method.CANCEL) {
            event.withProperty(Status.VEVENT_CANCELLED)
        } else {
            event.withProperty(Status.VEVENT_CONFIRMED)
        }

        organizer?.let {
            event.withProperty(Organizer(URI.create("mailto:$organizer")))
            event.withProperty(
                Attendee(URI.create("mailto:$organizer"))
                    .withParameter(PartStat.ACCEPTED)
                    .withParameter(Cn(organizer))
                    .withParameter(Role.REQ_PARTICIPANT)
                    .getFluentTarget()
            )
        }

        for (attendee in attendees) {
            event.withProperty(
                Attendee(URI.create("mailto:$attendee"))
                    .withParameter(PartStat.NEEDS_ACTION)
                    .withParameter(Cn(attendee))
                    .withParameter(Role.REQ_PARTICIPANT)
                    .getFluentTarget()
            )
        }

        val calendarEvent = Calendar()
            .withProdId(prodId)
            .withDefaults()
            .withProperty(method)
            .withComponent(timeZone.vTimeZone)
            .withComponent(event)
            .fluentTarget

        val calendarEventStr = calendarEvent.toString()
        val base64CalendarEvent = Base64.getMimeEncoder().encode(calendarEventStr.encodeToByteArray())
        return CalendarEventAttachment(bytes = base64CalendarEvent.decodeToString())
    }

    class Builder(
        private val summary: String,
        private val startDate: Instant
    ) {
        private var uid: String? = null
        private var sequence: Int = 0
        private var prodId: String = DEFAULT_PROD_ID
        private var method: Method = Method.REQUEST
        private var description: String? = null
        private var durationInMillis: Long = 0L
        private var organizer: String? = null
        private var attendees: MutableSet<String> = mutableSetOf()
        private var createDate: Instant? = null
        private var timeZone: TimeZone? = null

        fun uid(uid: String) = apply { this.uid = uid }
        fun sequence(sequence: Int) = apply { this.sequence = sequence }
        fun prodId(prodId: String) = apply { this.prodId = prodId }
        fun method(method: Method) = apply { this.method = method }
        fun description(description: String) = apply { this.description = description }
        fun durationInMillis(durationInMillis: Long) = apply { this.durationInMillis = durationInMillis }
        fun organizer(organizer: String) = apply { this.organizer = organizer }

        fun attendees(attendees: Collection<String>) = apply { this.attendees = attendees.toMutableSet() }
        fun addAttendee(attendee: String) = apply { this.attendees.add(attendee) }

        fun createDate(createDate: Instant) = apply { this.createDate = createDate }
        fun timeZone(timeZone: TimeZone) = apply { this.timeZone = timeZone }

        private fun initDefaultTimeZone(): TimeZone {
            val timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry()
            return timeZoneRegistry.getTimeZone(TimeZones.UTC_ID)
        }

        fun build() = let {
            if (uid.isNullOrBlank()) uid = UUID.randomUUID().toString()

            CalendarEvent(
                uid!!,
                sequence,
                prodId,
                method,
                summary,
                description,
                startDate,
                durationInMillis,
                organizer,
                attendees,
                createDate,
                timeZone ?: initDefaultTimeZone()
            )
        }
    }

    data class CalendarEventAttachment(
        val bytes: String,
        val meta: MetaData = MetaData()
    )

    data class MetaData(
        val name: String = FILE_NAME,
        val ext: String = FILE_EXT,
        val mimeType: String = FILE_MIME_TYPE
    )
}
