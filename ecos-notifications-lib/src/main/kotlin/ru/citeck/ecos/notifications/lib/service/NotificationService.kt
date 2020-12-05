package ru.citeck.ecos.notifications.lib.service

import ru.citeck.ecos.notifications.lib.Notification

interface NotificationService {

    fun send(notification: Notification)
}
