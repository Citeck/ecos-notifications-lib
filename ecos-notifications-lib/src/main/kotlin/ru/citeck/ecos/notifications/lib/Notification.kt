package ru.citeck.ecos.notifications.lib

import org.apache.commons.lang3.LocaleUtils
import ru.citeck.ecos.records2.RecordRef
import java.util.*

data class Notification(
        val record: RecordRef,
        val templateRef: RecordRef,
        val type: NotificationType,
        val recipients: Set<String>,
        val from: String,
        val lang: Locale? = null,
        val additionalMeta: Map<String, Any>
) {

    //We use builder pattern in kotlin, because this builder may be invoked from java code
    class Builder {
        private var record: RecordRef = RecordRef.EMPTY
        private var templateRef: RecordRef = RecordRef.EMPTY
        private var type: NotificationType = NotificationType.EMAIL_NOTIFICATION
        private var recipients: MutableSet<String> = mutableSetOf()
        private var from: String = ""
        private var lang: Locale? = null
        private var additionalMeta: MutableMap<String, Any> = mutableMapOf()

        fun record(record: RecordRef) = apply { this.record = record }
        fun templateRef(templateRef: RecordRef) = apply { this.templateRef = templateRef }
        fun notificationType(type: NotificationType) = apply { this.type = type }
        fun recipients(recipients: List<String>) = apply { this.recipients = recipients.toMutableSet() }
        fun addRecipient(recipient: String) = apply { this.recipients.add(recipient) }
        fun from(from: String) = apply { this.from = from }
        fun lang(lang: Locale) = apply { this.lang = lang }
        fun lang(lang: String) = apply { this.lang = LocaleUtils.toLocale(lang) }
        fun additionalMeta(model: Map<String, Any>) = apply { this.additionalMeta = model.toMutableMap() }
        fun addToAdditionalMeta(key: String, data: Any) = apply { this.additionalMeta[key] = data }

        fun build() = let {

            if (RecordRef.EMPTY == templateRef) throw BuildNotificationException("TemplateRef is mandatory parameter")

            Notification(
                    record,
                    templateRef,
                    type,
                    recipients,
                    from,
                    lang,
                    additionalMeta
            )
        }
    }
}
