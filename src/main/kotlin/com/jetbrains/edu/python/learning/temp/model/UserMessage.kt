package com.jetbrains.edu.python.learning.temp.model

import kotlinx.serialization.Serializable

/**
 * Represents a message from the user in the chat.
 *
 * @property message The content of the message provided by the user.
 */
@Serializable
data class UserMessage(
    val message: String
) : Message {
    override val messageType: MessageType = MessageType.USER
}