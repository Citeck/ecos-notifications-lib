package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.commands.CommandsService
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.command.SendNotificationCommand
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext
import java.time.Duration
import java.util.*

private const val TARGET_APP = "notifications"

private val INTERNAL_DEFAULT_LOCALE: Locale = Locale.ENGLISH
private const val INTERNAL_DEFAULT_FROM = "ecos.notification@citeck.ru"

class NotificationServiceImpl(
    private val commandsService: CommandsService,
    private val recordsServiceFactory: RecordsServiceFactory,
    private val notificationTemplateService: NotificationTemplateService
) : NotificationService {

    private val recordsService = recordsServiceFactory.recordsServiceV1

    var defaultLocale: Locale? = null
    var defaultFrom: String? = null

    override fun send(notification: Notification) {

        val filledModel = fillModel(notification)
        val locale = notification.lang ?: defaultLocale ?: INTERNAL_DEFAULT_LOCALE
        val from = notification.from ?: defaultFrom ?: INTERNAL_DEFAULT_FROM

        val recordRef = if (notification.record is RecordRef) {
            notification.record
        } else {
            RecordRef.valueOf(recordsService.getAtt(notification.record, "?id").asText())
        }

        val command = SendNotificationCommand(
            id = notification.id,
            record = recordRef,
            title = notification.title,
            body = notification.body,
            templateRef = notification.templateRef,
            type = notification.type,
            lang = locale.toString(),
            recipients = notification.recipients,
            from = from,
            cc = notification.cc,
            bcc = notification.bcc,
            model = filledModel
        )

        commandsService.execute {
            this.ttl = Duration.ZERO
            this.targetApp = TARGET_APP
            this.body = command
        }
    }

    private fun fillModel(notification: Notification): Map<String, Any> {
        if (notification.templateRef == RecordRef.EMPTY) {
            return emptyMap()
        }

        val requiredModel = notificationTemplateService.getMultiModelAttributes(notification.templateRef)
        val attsToRequest = getPrefilledModel()
        attsToRequest.addAll(requiredModel)

        val filledModel = mutableMapOf<String, Any>()

        RequestContext.doWithCtx(
            recordsServiceFactory,
            { data ->
                data.withCtxAtts(notification.additionalMeta)
            }
        ) {
            recordsService.getAtts(notification.record, attsToRequest).forEach { key, attr ->
                filledModel[key] = attr
            }
        }

        return filledModel
    }

    private fun getPrefilledModel(): MutableSet<String> {
        return mutableSetOf("_etype?id")
    }
}
