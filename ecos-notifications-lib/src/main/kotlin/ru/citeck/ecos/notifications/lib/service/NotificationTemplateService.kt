package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.RecordsService

class NotificationTemplateService(
    private val recordsService: RecordsService
) {

    fun getMultiModelAttributes(template: RecordRef): Set<String> {
        val attributes = recordsService.getAttribute(template, "multiModelAttributes[]")
        if (attributes.isNull()) {
            return emptySet()
        }

        return attributes.asList(String::class.java).toSet()
    }

}
