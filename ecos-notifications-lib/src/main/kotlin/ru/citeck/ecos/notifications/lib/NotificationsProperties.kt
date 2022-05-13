package ru.citeck.ecos.notifications.lib

import java.util.Locale

data class NotificationsProperties(
    val defaultLocale: Locale = Locale.ENGLISH,
    val defaultFrom: String = "ecos.notification@citeck.ru"
)
