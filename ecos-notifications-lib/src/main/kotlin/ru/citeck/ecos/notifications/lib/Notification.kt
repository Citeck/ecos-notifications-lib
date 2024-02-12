package ru.citeck.ecos.notifications.lib

import org.apache.commons.lang3.LocaleUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

data class Notification(
    val id: String,
    val record: Any?,
    val title: String = "",
    val body: String = "",
    val templateRef: EntityRef,
    val type: NotificationType,
    val recipients: Set<String>,
    val from: String? = null,
    val cc: Set<String>,
    val bcc: Set<String>,
    val lang: Locale? = null,
    val additionalMeta: Map<String, Any>
) {

    // We use builder pattern in kotlin, because this builder may be invoked from java code
    class Builder {
        private var id: String? = null
        private var record: Any? = null
        private var title: String = ""
        private var body: String = ""
        private var templateRef: EntityRef = EntityRef.EMPTY
        private var type: NotificationType = NotificationType.EMAIL_NOTIFICATION
        private var recipients: MutableSet<String> = mutableSetOf()
        private var from: String? = null
        private var cc: MutableSet<String> = mutableSetOf()
        private var bcc: MutableSet<String> = mutableSetOf()
        private var lang: Locale? = null
        private var additionalMeta: MutableMap<String, Any> = mutableMapOf()

        fun id(id: String) = apply { this.id = id }
        fun record(record: Any?) = apply { this.record = record }

        fun title(title: String) = apply { this.title = title }
        fun body(body: String) = apply { this.body = body }
        fun templateRef(templateRef: EntityRef) = apply { this.templateRef = templateRef }

        fun notificationType(type: NotificationType) = apply { this.type = type }

        fun recipients(recipients: Collection<String>) = apply { this.recipients = recipients.toMutableSet() }
        fun addRecipient(recipient: String) = apply { this.recipients.add(recipient) }

        fun cc(cc: Collection<String>) = apply { this.cc = cc.toMutableSet() }
        fun addCc(cc: String) = apply { this.cc.add(cc) }

        fun bcc(bcc: Collection<String>) = apply { this.bcc = bcc.toMutableSet() }
        fun addBcc(bcc: String) = apply { this.bcc.add(bcc) }

        fun from(from: String?) = apply { this.from = from }

        fun lang(lang: Locale?) = apply { this.lang = lang }
        fun lang(lang: String?) = apply {
            lang?.let {
                this.lang = LocaleUtils.toLocale(lang)
            }
        }

        fun additionalMeta(model: Map<String, Any>) = apply { this.additionalMeta = model.toMutableMap() }
        fun addToAdditionalMeta(key: String, data: Any) = apply { this.additionalMeta[key] = data }

        fun build() = let {

            if (body.isBlank() && EntityRef.EMPTY == templateRef) {
                throw BuildNotificationException("TemplateRef is mandatory parameter with empty body")
            }
            if (id.isNullOrBlank()) id = UUID.randomUUID().toString()

            Notification(
                id!!,
                record,
                title,
                body,
                templateRef,
                type,
                recipients,
                from,
                cc,
                bcc,
                lang,
                additionalMeta
            )
        }
    }
}
