package ru.citeck.ecos.notifications.lib.utils

import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.DateTime
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

object CalendarEventUtils {

    private const val FILE_NAME = "invite.ics"
    private const val FILE_EXT = "ics"
    private const val FILE_MIME_TYPE = "text/calendar"
    private const val DEFAULT_PROD_ID = "Citeck"

    fun createCalendarEventAttachment(
        summary: String,
        description: String,
        start: Instant,
        durationInMillis: Long,
        organizer: String,
        attendees: List<String>,
        uid: String? = null,
        sequence: Int? = null,
        prodId: String? = null,
    ): CalendarEventAttachment {
        val timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry()
        val timeZone = timeZoneRegistry.getTimeZone(TimeZones.UTC_ID)

        val end = start.plusMillis(durationInMillis)

        val startDateTime = DateTime(start.toEpochMilli())
        startDateTime.timeZone = timeZone

        val endDateTime = DateTime(end.toEpochMilli())
        endDateTime.timeZone = timeZone

        val event = VEvent(startDateTime, endDateTime, summary)
        event.withProperty(Description(description))

        val propertyUid = if (uid == null) {
            Uid(UUID.randomUUID().toString())
        } else {
            Uid(uid)
        }
        event.withProperty(propertyUid)

        val propertySequence = if (sequence == null) {
            Sequence()
        } else {
            Sequence(sequence)
        }
        event.withProperty(propertySequence)

        event.withProperty(Organizer(URI.create("mailto:$organizer")))
        event.withProperty(
            Attendee(URI.create("mailto:$organizer"))
                .withParameter(PartStat.ACCEPTED)
                .withParameter(Cn(organizer))
                .withParameter(Role.REQ_PARTICIPANT)
                .getFluentTarget()
        )

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
            .withProdId(prodId ?: DEFAULT_PROD_ID)
            .withDefaults()
            .withComponent(timeZone.vTimeZone)
            .withComponent(event)
            .fluentTarget

        val calendarEventStr = calendarEvent.toString()
        val base64CalendarEvent = Base64.getMimeEncoder().encode(calendarEventStr.encodeToByteArray())
        return CalendarEventAttachment(bytes = base64CalendarEvent.decodeToString())
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
