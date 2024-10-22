package ru.citeck.ecos.notifications.lib

import org.junit.jupiter.api.AfterEach
import ru.citeck.ecos.commands.CommandExecutor
import ru.citeck.ecos.commands.CommandsServiceFactory
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.io.file.std.EcosStdFile
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.notifications.lib.api.*
import ru.citeck.ecos.notifications.lib.command.SendNotificationCommand
import ru.citeck.ecos.notifications.lib.service.NotificationServiceImpl
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.test.commons.EcosWebAppApiMock
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef

abstract class NotificationServiceTestBase {

    companion object {
        val TEST_RECORD_REF = EntityRef.create(AppName.NOTIFICATIONS, "test", "test")
    }

    val receivedCommands = ArrayList<SendNotificationCommand>()
    val templates = readTemplates()
    lateinit var notificationsService: NotificationServiceImpl
    lateinit var record: TestRecordData

    fun initialize(legacyApi: Boolean) {

        record = TestRecordData()

        val api = if (legacyApi) {
            NotificationsAppWebApi(null)
        } else {
            object : NotificationsAppApi {

                fun getExactTemplateData(baseTemplate: NotificationTemplateDto?, template: NotificationTemplateDto): GetTemplateDataExactTemplate {
                    val requiredAtts = baseTemplate?.model?.values?.toMutableSet() ?: mutableSetOf()
                    template.model?.values?.forEach { requiredAtts.add(it) }
                    return GetTemplateDataExactTemplate(
                        EntityRef.create(AppName.NOTIFICATIONS, "template", template.id),
                        requiredAtts
                    )
                }

                override fun getTemplateData(
                    templateRef: EntityRef,
                    attributes: ObjectData,
                    contextData: DataValue
                ): GetTemplateDataRes {
                    val template = templates[templateRef.getLocalId()] ?: error("Template not found: $templateRef")
                    if (template.multiTemplateConfig.isNullOrEmpty()) {
                        return getExactTemplateData(null, template)
                    }
                    val type = attributes["_type?id"].asText().toEntityRef()
                    val variants = ArrayList<GetTemplateDataTemplateVariants.Variant>()
                    for (multiTemplate in template.multiTemplateConfig) {
                        if (multiTemplate.type == type) {
                            if (variants.isEmpty() &&
                                PredicateUtils.isAlwaysTrue(multiTemplate.condition ?: Predicates.alwaysTrue())
                            ) {
                                return getExactTemplateData(template, templates[multiTemplate.template.getLocalId()]!!)
                            } else {
                                variants.add(
                                    GetTemplateDataTemplateVariants.Variant(
                                        multiTemplate.template,
                                        multiTemplate.condition ?: Predicates.alwaysTrue()
                                    )
                                )
                            }
                        }
                    }
                    if (variants.isEmpty()) {
                        return getExactTemplateData(null, template)
                    }
                    variants.add(GetTemplateDataTemplateVariants.Variant(templateRef, Predicates.alwaysTrue()))
                    return GetTemplateDataTemplateVariants(
                        template.model?.values?.toSet() ?: emptySet(),
                        variants,
                        DataValue.createObj()
                    )
                }
            }
        }
        val webApi = EcosWebAppApiMock("notifications")

        val commandsServices = object : CommandsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return webApi
            }
        }
        commandsServices.commandsService.addExecutor(object : CommandExecutor<SendNotificationCommand> {
            override fun execute(command: SendNotificationCommand): Any {
                receivedCommands.add(command)
                return DataValue.createObj()
            }
        })

        val records = object : RecordsServiceFactory() {
            override fun getEcosWebAppApi(): EcosWebAppApi {
                return webApi
            }
        }

        if (legacyApi) {
            records.recordsService.register(object : RecordAttsDao {
                override fun getId(): String {
                    return "template"
                }

                override fun getRecordAtts(recordId: String): Any {
                    return TemplateRecord(templates[recordId]!!)
                }
            })
        }

        records.recordsService.register(object : RecordAttsDao {
            override fun getId(): String {
                return "test"
            }
            override fun getRecordAtts(recordId: String): Any? {
                if (recordId == "test") {
                    return record
                }
                return null
            }
        })

        notificationsService = NotificationServiceImpl(
            commandsServices.commandsService,
            records,
            api,
            NotificationsProperties()
        )
    }

    fun createTemplateRef(id: String): EntityRef {
        return EntityRef.create(AppName.NOTIFICATIONS, "template", id)
    }

    private fun readTemplates(): Map<String, NotificationTemplateDto> {
        val root = ResourceUtils.getFile("${ResourceUtils.CLASSPATH_URL_PREFIX}test/templates")
        val result = HashMap<String, NotificationTemplateDto>()
        EcosStdFile(root).findFiles("**.json").forEach {
            val value = Json.mapper.read(it, NotificationTemplateDto::class.java)!!
            result[value.id] = value
        }
        return result
    }

    @AfterEach
    fun baseAfterEach() {
        receivedCommands.clear()
    }

    inner class TemplateRecord(
        private val dto: NotificationTemplateDto
    ) {
        fun getMultiModelAttributes(): Set<String> {
            val result = HashSet<String>()
            dto.model?.values?.forEach { result.add(it) }
            processMultiTemplate(dto.multiTemplateConfig, result)
            return result
        }

        private fun processMultiTemplate(config: List<SubTemplateDto>?, result: MutableSet<String>) {
            config?.forEach { subTemplate ->
                val template = templates[subTemplate.template.getLocalId()]!!
                template.model?.values?.forEach { result.add(it) }
                processMultiTemplate(template.multiTemplateConfig, result)
            }
        }
    }

    class TestRecordData(
        var text: String = "txt",
        var typeId: String = "test-type"
    ) {
        fun getId(): String {
            return TEST_RECORD_REF.getLocalId()
        }
        fun getEcosType(): String {
            return typeId
        }
    }

    class NotificationTemplateDto(
        val id: String,
        val model: Map<String, String>?,
        val multiTemplateConfig: List<SubTemplateDto>?
    )

    class SubTemplateDto(
        val template: EntityRef,
        val type: EntityRef,
        val condition: Predicate?
    )
}
