package com.fylora.session.notifications

data class NotifyUser(
    val notifications: MutableList<Notification>,
    val userId: String,
)
