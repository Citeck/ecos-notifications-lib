package ru.citeck.ecos.notifications.lib.service

import org.apache.commons.lang3.StringUtils
import ru.citeck.ecos.commands.CommandsService
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.command.SendNotificationCommand
import ru.citeck.ecos.records2.RecordsService
import ru.citeck.ecos.records2.meta.RecordsMetaService
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils
import java.time.Duration
import java.util.*
import java.util.function.BiConsumer

private const val TARGET_APP = "notifications"

private val INTERNAL_DEFAULT_LOCALE: Locale = Locale.ENGLISH
private const val INTERNAL_DEFAULT_FROM = "ecos.notification@citeck.ru"

class NotificationService(
    private val commandsService: CommandsService,
    private val recordsService: RecordsService,
    private val recordsMetaService: RecordsMetaService,
    private val notificationTemplateService: NotificationTemplateService
) {

    var defaultLocale: Locale? = null
    var defaultFrom: String? = null

    fun send(notification: Notification) {
        val filledModel = fillModel(notification)
        val locale = notification.lang ?: defaultLocale ?: INTERNAL_DEFAULT_LOCALE
        val from = notification.from ?: defaultFrom ?: INTERNAL_DEFAULT_FROM

        val command = SendNotificationCommand(
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
        val requiredModel = notificationTemplateService.getMultiModelAttributes(notification.templateRef)

        val recordModel = getPrefilledModel()
        val additionalModel = mutableSetOf<String>()

        requiredModel.forEach { attr ->
            if (StringUtils.startsWithAny(attr, "$", ".att(n:\"$", ".atts(n:\"$")) {
                additionalModel.add(attr.replaceFirst("\$", ""))
            } else {
                recordModel.add(attr)
            }
        }

        val filledModel = mutableMapOf<String, Any>()

        RemoteRecordsUtils.runAsSystem {
            recordsService.getAttributes(notification.record, recordModel).forEach {
                key, attr -> filledModel[key] = attr
            }

            if (notification.additionalMeta.isNotEmpty()) {
                recordsMetaService.getMeta(notification.additionalMeta, additionalModel)
                    .attributes
                    .forEach(BiConsumer { key, attr ->
                        filledModel[key] = attr
                    })
            }
        }

        return filledModel
    }

    private fun getPrefilledModel(): MutableSet<String> {
        return mutableSetOf("_etype?id")
    }

}
