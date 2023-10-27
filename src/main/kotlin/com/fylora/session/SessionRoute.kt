package com.fylora.session

import com.fylora.auth.data.user.User
import com.fylora.bloggle
import com.fylora.session.model.Resource
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach

fun Route.session() {
    webSocket("/") {
        val user = call.authentication.principal<User>()

        if(user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
            return@webSocket
        }

        val result = bloggle.addUser(user, this)
        if(result is Resource.Error) {
            close(
                CloseReason(
                    CloseReason.Codes.INTERNAL_ERROR,
                    message = result.message ?: "Unknown error"
                )
            )
            return@webSocket
        }
        val activeUser = result.data ?: return@webSocket

        try {
            incoming.consumeEach { frame ->
                if(frame is Frame.Text) {
                    val request = frame.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            bloggle.disconnectUser(activeUser)
        }
    }
}