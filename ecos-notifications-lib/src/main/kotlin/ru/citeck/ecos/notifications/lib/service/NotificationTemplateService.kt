package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.notifications.lib.dto.TemplateModelDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDao
import java.util.HashMap
import java.util.function.BiConsumer

class NotificationTemplateService(
        private val remoteSyncTemplateModelRecordsDao: RemoteSyncRecordsDao<TemplateModelDto>
) {

    fun getTemplateModel(template: RecordRef): Map<String, String> {
        val modelDto = remoteSyncTemplateModelRecordsDao.getRecord(template)
        if (!modelDto.isPresent) {
            return emptyMap()
        }

        val model: MutableMap<String, String> = HashMap()
        modelDto.get().model?.forEach(BiConsumer { s, dataValue -> model[s] = dataValue.asText() })
        return model
    }

}