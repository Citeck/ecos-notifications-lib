package ru.citeck.ecos.notifications.lib.dto

import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt

class TemplateMultiModelAttributesDto {

    @MetaAtt("multiModelAttributes[]")
    var attributes: Set<String>? = emptySet()

}
