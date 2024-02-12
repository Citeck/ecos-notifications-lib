package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef

class NotificationTemplateService(
    private val recordsService: RecordsService
) {

    fun getMultiModelAttributes(template: EntityRef): Set<String> {
        val attributes = recordsService.getAtt(template, "multiModelAttributes[]")
        if (attributes.isNull()) {
            return emptySet()
        }

        return attributes.asList(String::class.java).toSet()
    }
}
