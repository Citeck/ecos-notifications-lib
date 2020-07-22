package ru.citeck.ecos.notifications.lib.spring

import org.apache.commons.lang3.LocaleUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.commands.CommandsService
import ru.citeck.ecos.notifications.lib.dto.TemplateModelDto
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.notifications.lib.service.NotificationTemplateService
import ru.citeck.ecos.records2.RecordsService
import ru.citeck.ecos.records2.meta.RecordsMetaService
import ru.citeck.ecos.records2.source.dao.local.RemoteSyncRecordsDao

@Configuration
open class NotificationsServiceConfig {

    @Value("\${notifications.default.locale}")
    private lateinit var defaultAppNotificationLocale: String

    @Bean
    @ConditionalOnMissingBean(NotificationService::class)
    open fun ecosNotificationService(commandsService: CommandsService,
                                     recordsService: RecordsService,
                                     recordsMetaService: RecordsMetaService,
                                     notificationTemplateService: NotificationTemplateService
    ): NotificationService {
        val service = NotificationService(commandsService, recordsService, recordsMetaService,
                notificationTemplateService)
        service.defaultLocale = LocaleUtils.toLocale(defaultAppNotificationLocale)
        return service
    }

    @Bean
    @ConditionalOnMissingBean(NotificationTemplateService::class)
    open fun notificationTemplateService(
            @Qualifier("remoteSyncTemplateModelRecordsDao") syncRecordsDao: RemoteSyncRecordsDao<TemplateModelDto>
    ): NotificationTemplateService {
        return NotificationTemplateService(syncRecordsDao)
    }

    @Bean
    open fun remoteSyncTemplateModelRecordsDao(): RemoteSyncRecordsDao<TemplateModelDto> {
        return RemoteSyncRecordsDao("notifications/template", TemplateModelDto::class.java)
    }

}