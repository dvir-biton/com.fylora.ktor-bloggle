package com.fylora.session

import com.fylora.bloggle
import com.fylora.logging.Logging
import com.fylora.session.model.Request
import com.fylora.session.model.Resource
import com.fylora.session.model.Response
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.Json

fun Route.session() {
    authenticate {
        webSocket("/connect") {
            val principal = call.principal<JWTPrincipal>()

            val userId = principal?.getClaim("userId", String::class)
            val username = principal?.getClaim("username", String::class)

            if(userId == null || username == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Authentication required"))
                return@webSocket
            }

            val result = bloggle.addUser(
                userId = userId,
                username = username,
                session = this
            )

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
                        try {
                            val requestBody = frame.readText()

                            when(val request = Json.decodeFromString<Request>(requestBody)) {
                                is Request.MakeComment -> bloggle.comment(
                                    activeUser = activeUser,
                                    postId = request.postId,
                                    body = request.body
                                )
                                is Request.MakePost -> bloggle.post(
                                    activeUser = activeUser,
                                    body = request.body
                                )
                                is Request.MakeLikeComment -> bloggle.likeComment(
                                    activeUser = activeUser,
                                    commentId = request.commentId
                                )
                                is Request.MakeLikePost -> bloggle.likePost(
                                    activeUser = activeUser,
                                    postId = request.postId
                                )
                                is Request.MakeFollow -> bloggle.follow(
                                    follower = activeUser,
                                    followUserId = request.userId
                                )
                                is Request.GetAccount -> bloggle.getAccount(
                                    activeUser = activeUser,
                                    userId = request.userId
                                )
                                is Request.GetPost -> bloggle.getPost(
                                    activeUser = activeUser,
                                    postId = request.postId
                                )
                                is Request.SearchAccounts -> bloggle.searchAccounts(
                                    activeUser = activeUser,
                                    query = request.query
                                )
                                Request.GetNotifications -> bloggle.getNotifications(
                                    activeUser = activeUser,
                                )
                                Request.GetPosts -> bloggle.getPosts(
                                    activeUser = activeUser
                                )
                            }
                        } catch (e: IllegalArgumentException) {
                            val message = e.message ?: "Invalid request"
                            bloggle.logErrorSendAndReturn<String>(
                                message = message,
                                logging = Logging.Fail(e.message ?: "Invalid request"),
                                session = activeUser.session,
                                response = Response.ErrorResponse(
                                    e.message ?: "Invalid request"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bloggle.disconnectUser(activeUser)
            }
        }
    }
}
