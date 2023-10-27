package com.fylora.session

import com.fylora.auth.data.user.User
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

fun Route.session() {
    webSocket("/") {
        val user = call.authentication.principal<User>()

        if(user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
            return@webSocket
        }
    }
}