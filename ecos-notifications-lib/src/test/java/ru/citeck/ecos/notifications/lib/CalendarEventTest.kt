package ru.citeck.ecos.notifications.lib

import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.model.property.Method
import org.junit.jupiter.api.Test
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.notifications.lib.icalendar.CalendarEvent
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals

class CalendarEventTest {

    @Test
    fun test() {
        val eventSummary = "Summary"
        val eventDescription = "Description"
        val eventStartDate = Instant.parse("2024-07-30T00:00:00Z")
        val eventDuration = 7200000L
        val eventOrganizer = "organizer@test.ru"
        val eventAttendees = listOf(
            "attendee1@test.ru",
            "attendee2@test.ru"
        )
        val eventUid = "dd7908c2-489b-4cc6-a173-e30cce84844e"
        var sequence = 0
        val createDate = Instant.parse("2024-07-29T00:00:00Z")

        val calendarEvent = CalendarEvent.Builder(eventSummary, eventStartDate)
            .uid(eventUid)
            .sequence(sequence)
            .createDate(createDate)
            .description(eventDescription)
            .durationInMillis(eventDuration)
            .organizer(eventOrganizer)
            .attendees(eventAttendees)
            .build()
        var calendarEventAttachment = calendarEvent.createAttachment()

        var resource = "test/icalendar/invite.ics"
        var inviteText = ResourceUtils.getFile("${ResourceUtils.CLASSPATH_URL_PREFIX}$resource")
            .readText(StandardCharsets.UTF_8)
        var decodeCalendarEvent = Base64.getMimeDecoder().decode(calendarEventAttachment.bytes)
        assertEquals(inviteText, decodeCalendarEvent.decodeToString())

        sequence += 1
        val cancelCalendarEvent = CalendarEvent.Builder(eventSummary, eventStartDate)
            .uid(eventUid)
            .sequence(sequence)
            .createDate(createDate)
            .description(eventDescription)
            .durationInMillis(eventDuration)
            .organizer(eventOrganizer)
            .attendees(eventAttendees)
            .method(Method.CANCEL)
            .build()
        calendarEventAttachment = cancelCalendarEvent.createAttachment()

        resource = "test/icalendar/cancel_invite.ics"
        inviteText = ResourceUtils.getFile("${ResourceUtils.CLASSPATH_URL_PREFIX}$resource")
            .readText(StandardCharsets.UTF_8)
        decodeCalendarEvent = Base64.getMimeDecoder().decode(calendarEventAttachment.bytes)
        assertEquals(inviteText, decodeCalendarEvent.decodeToString())

        sequence = 2
        val createDateGMT4 = Instant.parse("2024-07-29T00:00:00Z")

        val timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry()
        val timeZone = timeZoneRegistry.getTimeZone("Etc/GMT+7")

        val calendarEventGMT4 = CalendarEvent.Builder(eventSummary, eventStartDate)
            .uid(eventUid)
            .sequence(sequence)
            .createDate(createDateGMT4)
            .timeZone(timeZone)
            .description(eventDescription)
            .durationInMillis(eventDuration)
            .organizer(eventOrganizer)
            .attendees(eventAttendees)
            .build()
        calendarEventAttachment = calendarEventGMT4.createAttachment()

        resource = "test/icalendar/invite_GMT_7.ics"
        inviteText = ResourceUtils.getFile("${ResourceUtils.CLASSPATH_URL_PREFIX}$resource")
            .readText(StandardCharsets.UTF_8)
        decodeCalendarEvent = Base64.getMimeDecoder().decode(calendarEventAttachment.bytes)
        assertEquals(inviteText, decodeCalendarEvent.decodeToString())
    }
}
