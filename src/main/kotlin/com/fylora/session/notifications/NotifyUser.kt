package com.fylora.session.notifications

import kotlinx.serialization.Serializable

@Serializable
data class NotifyUser(
    val notifications: MutableList<Notification>,
    val userId: String,
)
