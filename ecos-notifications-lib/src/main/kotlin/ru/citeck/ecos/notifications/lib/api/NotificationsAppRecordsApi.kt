package ru.citeck.ecos.notifications.lib.api

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef

class NotificationsAppRecordsApi(
    private val recordsService: RecordsService
) : NotificationsAppApi {

    override fun getTemplateData(
        templateRef: EntityRef,
        attributes: ObjectData,
        contextData: DataValue
    ): GetTemplateDataExactTemplate {
        val requiredAtts = recordsService.getAtt(templateRef, "multiModelAttributes[]")
        val requiredAttsSet = if (requiredAtts.isNull()) {
            emptySet()
        } else {
            requiredAtts.asList(String::class.java).toSet()
        }
        return GetTemplateDataExactTemplate(templateRef, requiredAttsSet)
    }
}
