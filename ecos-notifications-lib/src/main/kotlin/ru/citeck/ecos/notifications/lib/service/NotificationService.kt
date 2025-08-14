package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.command.SendNotificationResult

interface NotificationService {

    fun send(notification: Notification)

    fun sendSync(notification: Notification): SendNotificationResult
}
