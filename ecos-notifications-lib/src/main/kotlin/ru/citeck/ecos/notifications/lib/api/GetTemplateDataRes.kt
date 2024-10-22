package ru.citeck.ecos.notifications.lib.api

import ecos.com.fasterxml.jackson210.annotation.JsonSubTypes
import ecos.com.fasterxml.jackson210.annotation.JsonTypeInfo
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.webapp.api.entity.EntityRef

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "_type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = GetTemplateDataTemplateVariants::class, name = "template-variants"),
    JsonSubTypes.Type(value = GetTemplateDataExactTemplate::class, name = "exact-template"),
    JsonSubTypes.Type(value = GetTemplateDataNoRes::class, name = "no-res")
)
sealed class GetTemplateDataRes

object GetTemplateDataNoRes : GetTemplateDataRes()

data class GetTemplateDataTemplateVariants(
    /**
     * Required atts for base template
     */
    val requiredAtts: Set<String>,
    val variants: List<Variant>,
    val contextData: DataValue
) : GetTemplateDataRes() {

    data class Variant(
        val templateRef: EntityRef,
        val predicate: Predicate
    )
}

data class GetTemplateDataExactTemplate(
    val templateRef: EntityRef,
    val requiredAtts: Set<String>
) : GetTemplateDataRes()
