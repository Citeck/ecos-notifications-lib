package ru.citeck.ecos.notifications.lib.service

import org.apache.commons.lang3.StringUtils
import ru.citeck.ecos.commands.CommandsService
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.command.SendNotificationCommand
import ru.citeck.ecos.records2.RecordsService
import ru.citeck.ecos.records2.meta.RecordsMetaService
import java.time.Duration
import java.util.*
import java.util.function.BiConsumer

private const val TARGET_APP = "notifications"
private val INTERNAL_DEFAULT_LOCALE: Locale = Locale.ENGLISH

class NotificationService(
        private val commandsService: CommandsService,
        private val recordsService: RecordsService,
        private val recordsMetaService: RecordsMetaService,
        private val notificationTemplateService: NotificationTemplateService
) {

    var defaultLocale: Locale? = null

    fun send(notification: Notification) {
        val filledModel = fillModel(notification)
        val locale = notification.lang ?: defaultLocale ?: INTERNAL_DEFAULT_LOCALE

        val command = SendNotificationCommand(
                notification.templateRef,
                notification.type,
                locale.toString(),
                notification.recipients,
                notification.from,
                filledModel
        )

        commandsService.execute {
            this.ttl = Duration.ZERO
            this.targetApp = TARGET_APP
            this.body = command
        }
    }

    private fun fillModel(notification: Notification): Map<String, Any> {

        val requiredModel = notificationTemplateService.getTemplateModel(notification.templateRef)


        val recordModel = mutableMapOf<String, String>()
        val additionalModel = mutableMapOf<String, String>()

        requiredModel.forEach { (key, attr) ->
            if (StringUtils.startsWithAny(attr, "$", ".att(n:\"$", ".atts(n:\"$")) {
                additionalModel[key] = attr.replaceFirst("\$", "")
            } else {
                recordModel[key] = attr
            }
        }

        val filledModel = mutableMapOf<String, Any>()

        recordsService.getAttributes(notification.record, recordModel).forEach { key, attr -> filledModel[key] = attr }

        if (notification.additionalMeta.isNotEmpty()) {
            recordsMetaService.getMeta(notification.additionalMeta, additionalModel)
                    .attributes
                    .forEach(BiConsumer { key, attr ->
                        filledModel[key] = attr
                    })
        }

        return filledModel
    }

}