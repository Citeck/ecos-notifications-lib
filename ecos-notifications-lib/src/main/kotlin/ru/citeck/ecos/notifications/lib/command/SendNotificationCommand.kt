package ru.citeck.ecos.notifications.lib.command

import ru.citeck.ecos.commands.annotation.CommandType
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.records2.RecordRef

@CommandType("ecos.notifications.send")
data class SendNotificationCommand(
        val templateRef: RecordRef,
        val type: NotificationType,
        val lang: String,
        val recipients: Set<String>,
        val from: String,
        val model: Map<String, Any>
)