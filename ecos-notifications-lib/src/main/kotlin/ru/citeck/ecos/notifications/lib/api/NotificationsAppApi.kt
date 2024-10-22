package ru.citeck.ecos.notifications.lib.api

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.webapp.api.entity.EntityRef

interface NotificationsAppApi {

    /**
     * Get required attributes for notification command
     * Method may return exact template ref or template variants to chose from.
     */
    fun getTemplateData(
        templateRef: EntityRef,
        attributes: ObjectData,
        contextData: DataValue
    ): GetTemplateDataRes
}
