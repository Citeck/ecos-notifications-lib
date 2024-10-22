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
    /**
     * The list of template references representing the path followed during the template resolution process.
     */
    val templatesPath: List<EntityRef> = emptyList(),
    /**
     * Indicates whether the provided templateRef should be used directly.
     * true: templateRef is final and will be used as is.
     * false: templateRef may be a complex template, and the final reference could be calculated later.
     */
    val isTemplateRefFinal: Boolean = false,
    val type: NotificationType,
    val lang: String,
    val webUrl: String = "",
    val recipients: Set<String>,
    val from: String,
    val cc: Set<String> = emptySet(),
    val bcc: Set<String> = emptySet(),
    val model: Map<String, Any?>,
    val createdFrom: EntityRef = EntityRef.EMPTY
)
