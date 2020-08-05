package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.notifications.lib.dto.TemplateMultiModelAttributesDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDao

class NotificationTemplateService(
        private val remoteSyncTemplateMultiModelAttributesRecordsDao: RemoteSyncRecordsDao<TemplateMultiModelAttributesDto>
) {

    fun getMultiModelAttributes(template: RecordRef): Set<String> {
        val dto = remoteSyncTemplateMultiModelAttributesRecordsDao.getRecord(template)
        if (!dto.isPresent) {
            return emptySet()
        }

        return dto.get().attributes.orEmpty()
    }

}
