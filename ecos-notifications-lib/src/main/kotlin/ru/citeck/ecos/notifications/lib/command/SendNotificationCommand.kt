package ru.citeck.ecos.notifications.lib.command

import ru.citeck.ecos.commands.annotation.CommandType
import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.webapp.api.entity.EntityRef

@CommandType("ecos.notifications.send")
data class SendNotificationCommand(
    val id: String,
    val record: EntityRef,
    val title: String = "",
    val body: String = "",
    val templateRef: EntityRef,
    val type: NotificationType,
    val lang: String,
    val recipients: Set<String>,
    val from: String,
    val cc: Set<String> = emptySet(),
    val bcc: Set<String> = emptySet(),
    val model: Map<String, Any>,
    val createdFrom: EntityRef = EntityRef.EMPTY
)
