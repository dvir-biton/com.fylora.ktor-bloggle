package com.fylora.session.model

import io.ktor.websocket.*

data class ActiveUser(
    val username: String,
    val userId: String,
    val session: WebSocketSession,
)
