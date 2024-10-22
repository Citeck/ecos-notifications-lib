package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.commands.CommandsService
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.NotificationsProperties
import ru.citeck.ecos.notifications.lib.api.*
import ru.citeck.ecos.notifications.lib.command.SendNotificationCommand
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.element.Element
import ru.citeck.ecos.records2.predicate.element.elematts.ElementAttributes
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Duration

private const val TARGET_APP = "notifications"
private const val MODEL_DATA = "_data"
private const val MODEL_ATTACHMENTS = "_attachments"

class NotificationServiceImpl(
    private val commandsService: CommandsService,
    private val recordsServiceFactory: RecordsServiceFactory,
    private val notificationsAppApi: NotificationsAppApi,
    private val properties: NotificationsProperties
) : NotificationService {

    companion object {
        private const val TYPE_ATT = "_type?id"
        private const val LEGACY_TYPE_ATT = "_etype?id"
        private val NEW_TO_LEGACY_ATTS_MAPPING = mapOf(
            TYPE_ATT to LEGACY_TYPE_ATT
        )

        private val log = KotlinLogging.logger {}
    }

    private val recordsService = recordsServiceFactory.recordsServiceV1
    private val predicateService = recordsServiceFactory.predicateService
    private val notificationsAppFallbackApi = NotificationsAppRecordsApi(recordsService)

    override fun send(notification: Notification) {

        val recordRef = if (notification.record is EntityRef) {
            notification.record
        } else {
            EntityRef.valueOf(recordsService.getAtt(notification.record, ScalarType.ID_SCHEMA).asText())
        }

        val templateWithModel = fillModel(recordRef, notification)
        val locale = notification.lang ?: properties.defaultLocale
        val from = notification.from ?: properties.defaultFrom

        val command = SendNotificationCommand(
            id = notification.id,
            record = recordRef,
            title = notification.title,
            body = notification.body,
            templateRef = templateWithModel.templateRef,
            templatesPath = templateWithModel.templatesPath,
            isTemplateRefFinal = templateWithModel.isTemplateRefFinal,
            type = notification.type,
            lang = locale.toString(),
            recipients = notification.recipients,
            from = from,
            cc = notification.cc,
            bcc = notification.bcc,
            model = templateWithModel.modelData
        )

        commandsService.execute {
            this.ttl = Duration.ZERO
            this.targetApp = TARGET_APP
            this.body = command
        }
    }

    private fun fillModel(recordRef: EntityRef, notification: Notification): TemplateWithModelData {

        if (notification.templateRef == EntityRef.EMPTY) {
            return TemplateWithModelData(EntityRef.EMPTY, false, emptyMap(), emptyList())
        }

        log.debug { "Fill model for record $recordRef and template ${notification.templateRef}" }

        val defaultRequiredAtts = getDefaultRequiredAtts()

        val allLoadedAtts = HashMap<String, DataValue>()

        var notificationsApi: NotificationsAppApi = notificationsAppApi
        var selectedTemplate: EntityRef = notification.templateRef

        fun loadAtts(attributes: Set<String>): Map<String, Any?> {
            val result = HashMap<String, Any?>()
            val attsToLoad = ArrayList<String>()
            if (allLoadedAtts.isNotEmpty()) {
                for (reqAtt in attributes) {
                    if (allLoadedAtts.containsKey(reqAtt)) {
                        result[reqAtt] = allLoadedAtts[reqAtt]
                    } else {
                        attsToLoad.add(reqAtt)
                    }
                }
            } else {
                attsToLoad.addAll(attributes)
            }
            if (attsToLoad.isNotEmpty()) {
                RequestContext.doWithCtx(
                    recordsServiceFactory,
                    { it.withCtxAtts(notification.additionalMeta) }
                ) {
                    recordsService.getAtts(notification.record, attsToLoad).forEach { key, value ->
                        allLoadedAtts[key] = value
                        result[key] = value
                    }
                }
            }
            return result
        }

        val defaultAttsObj = ObjectData.create(loadAtts(defaultRequiredAtts))
        var templateData = notificationsApi.getTemplateData(
            selectedTemplate,
            defaultAttsObj,
            DataValue.createObj()
        )
        if (templateData is GetTemplateDataNoRes) {
            log.debug { "New api is not supported. Switch to legacy" }
            notificationsApi = notificationsAppFallbackApi
            templateData = notificationsAppFallbackApi.getTemplateData(
                selectedTemplate,
                defaultAttsObj,
                DataValue.createObj()
            )
        }
        fun getContextDataForErrorMsg(): String {
            return "Data: $templateData selected template: $selectedTemplate " +
                "base template ${notification.templateRef} record: $recordRef"
        }
        val templatesPath = LinkedHashSet<EntityRef>()
        if (templateData is GetTemplateDataExactTemplate && templateData.templateRef != selectedTemplate) {
            templatesPath.add(selectedTemplate)
        }

        val templateSelectionStartedAt = System.currentTimeMillis()
        val requiredAtts = HashSet<String>()
        var iterations = 0
        while (templateData is GetTemplateDataTemplateVariants) {

            if (iterations++ > 100) {
                error("Infinite loop detected. ${getContextDataForErrorMsg()}")
            }
            templatesPath.add(selectedTemplate)
            requiredAtts.addAll(templateData.requiredAtts)

            val attributesForPredicates = HashSet<String>()
            templateData.variants.forEach { variant ->
                if (!PredicateUtils.isAlwaysTrue(variant.predicate)) {
                    attributesForPredicates.addAll(PredicateUtils.getAllPredicateAttributes(variant.predicate))
                }
            }
            val element = PredicateAttsMapElement(loadAtts(attributesForPredicates))
            selectedTemplate = templateData.variants.find {
                !templatesPath.contains(it.templateRef) &&
                    (PredicateUtils.isAlwaysTrue(it.predicate) || predicateService.isMatch(element, it.predicate))
            }?.templateRef ?: error(
                "None of the provided template variants are applicable. ${getContextDataForErrorMsg()}"
            )
            templateData = notificationsApi.getTemplateData(
                selectedTemplate,
                defaultAttsObj,
                templateData.contextData
            )
        }

        if (templateData !is GetTemplateDataExactTemplate) {
            error("Invalid template data value. ${getContextDataForErrorMsg()}")
        }

        if (iterations == 0) {
            log.debug {
                "Template selection completed without additional iterations. " +
                    "Base template: ${notification.templateRef} " +
                    "record: $recordRef"
            }
        } else {
            log.debug {
                "Template selection completed " +
                    "with $iterations iterations " +
                    "during ${System.currentTimeMillis() - templateSelectionStartedAt}ms. " +
                    "Base template: ${notification.templateRef} " +
                    "record: $recordRef"
            }
        }
        if (log.isDebugEnabled() && templateData.templateRef != notification.templateRef) {
            log.debug {
                "Notification template was changed for recordRef $recordRef " +
                    "from ${notification.templateRef} to ${templateData.templateRef}"
            }
        }

        requiredAtts.addAll(templateData.requiredAtts)
        val requiredAttsValues = HashMap<String, Any?>()
        requiredAttsValues.putAll(loadAtts(requiredAtts))

        notification.additionalMeta[MODEL_DATA]?.let {
            requiredAttsValues.putIfAbsent(MODEL_DATA, it)
        }
        notification.additionalMeta[MODEL_ATTACHMENTS]?.let {
            requiredAttsValues.putIfAbsent(MODEL_ATTACHMENTS, it)
        }

        var fixedTemplateRef = true
        if (notificationsApi === notificationsAppFallbackApi) {
            // legacy api
            fixedTemplateRef = false
            defaultRequiredAtts.forEach {
                val key = NEW_TO_LEGACY_ATTS_MAPPING.getOrDefault(it, it)
                requiredAttsValues.putIfAbsent(key, allLoadedAtts[it])
            }
        } else {
            defaultRequiredAtts.forEach {
                requiredAttsValues.putIfAbsent(it, allLoadedAtts[it])
            }
        }
        return TemplateWithModelData(
            templateData.templateRef,
            fixedTemplateRef,
            requiredAttsValues,
            templatesPath.toList()
        )
    }

    private fun getDefaultRequiredAtts(): Set<String> {
        return setOf(TYPE_ATT)
    }

    private class TemplateWithModelData(
        val templateRef: EntityRef,
        val isTemplateRefFinal: Boolean,
        val modelData: Map<String, Any?>,
        val templatesPath: List<EntityRef>
    )

    private class PredicateAttsMapElement(val data: Map<String, Any?>) : Element, ElementAttributes {
        override fun getAttribute(name: String): Any? {
            return data[name]
        }

        override fun getAttributes(attributes: List<String>): ElementAttributes {
            return this
        }
    }
}
