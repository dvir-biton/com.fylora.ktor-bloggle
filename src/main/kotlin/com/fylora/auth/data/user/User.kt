package com.fylora.auth.data.user

import com.fylora.session.model.Account
import io.ktor.server.auth.*
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class User(
    val username: String,
    val password: String,
    val salt: String,

    @BsonId
    val id: ObjectId = ObjectId()
): Principal {
    companion object {
        fun User.toAccount(): Account {
            return Account(
                username = this.username,
                userId = id.toString()
            )
        }
    }
}
