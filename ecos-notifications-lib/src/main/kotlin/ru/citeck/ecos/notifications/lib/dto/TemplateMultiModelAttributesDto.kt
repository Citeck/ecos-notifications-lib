package ru.citeck.ecos.notifications.lib.dto

import ru.citeck.ecos.records3.record.op.atts.service.schema.annotation.AttName

class TemplateMultiModelAttributesDto {

    @AttName("multiModelAttributes[]")
    var attributes: Set<String>? = emptySet()
}
