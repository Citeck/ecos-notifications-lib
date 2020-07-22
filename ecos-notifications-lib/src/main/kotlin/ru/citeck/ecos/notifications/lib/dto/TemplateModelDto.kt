package ru.citeck.ecos.notifications.lib.dto

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt

class TemplateModelDto {

    @MetaAtt("model")
    var model: ObjectData? = ObjectData.create()

}