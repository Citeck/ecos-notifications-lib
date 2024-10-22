package ru.citeck.ecos.notifications.lib.api

import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.web.client.EcosWebClientApi

class NotificationsAppWebApi(
    private val webClientApi: EcosWebClientApi?
) : NotificationsAppApi {

    companion object {
        const val GET_TEMPLATE_DATA_PATH = "/notifications/get-template-data"
    }

    override fun getTemplateData(
        templateRef: EntityRef,
        attributes: ObjectData,
        contextData: DataValue
    ): GetTemplateDataRes {
        webClientApi ?: return GetTemplateDataNoRes

        val supportedVersion = webClientApi.getApiVersion(
            AppName.NOTIFICATIONS,
            GET_TEMPLATE_DATA_PATH,
            0
        )
        if (supportedVersion != 0) {
            return GetTemplateDataNoRes
        }
        return webClientApi.newRequest()
            .targetApp(AppName.NOTIFICATIONS)
            .path(GET_TEMPLATE_DATA_PATH)
            .body { it.writeDto(GetTemplateWithRequiredAttsReq(templateRef, attributes, contextData)) }
            .executeSync { it.getBodyReader().readDto(GetTemplateDataRes::class.java) }
    }

    data class GetTemplateWithRequiredAttsReq(
        val templateRef: EntityRef,
        val attributes: ObjectData,
        val contextData: DataValue
    )
}
