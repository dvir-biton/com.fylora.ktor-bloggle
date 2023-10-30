package com.fylora.session.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val username: String,
    val userId: String,
    val posts: MutableList<Post> = mutableListOf(),
    val totalLikes: Int = 0,
    val followers: MutableList<String> = mutableListOf()
) {
    companion object {
        fun ActiveUser.toAccount(): Account {
            return Account(
                username = this.username,
                userId = userId
            )
        }
    }
}