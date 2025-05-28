package com.jetbrains.edu.python.learning.temp.model

/**
* Represents the different types of messages that can be handled within the system.
*
* @property event The string representation of the event associated with the message type.
*/
enum class MessageType(val event: String) {
    ASSISTANT("assistant"),
    ASSISTANT_LOG("assistantLog"),
    ASSISTANT_FOR_USER("assistantForUser"),
    TOOL_CALL("toolCall"),
    ASSISTANT_ERROR("assistantError"),
    USER("user"),
}
