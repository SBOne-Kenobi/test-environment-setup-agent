package com.jetbrains.edu.python.learning.temp.agent

import ai.koog.agents.core.agent.AIAgent.FeatureContext
import ai.koog.agents.core.tools.reflect.ToolFromCallable.VarArgs
import ai.koog.agents.local.features.eventHandler.feature.EventHandler
import ai.koog.agents.local.features.tracing.feature.Tracing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import com.jetbrains.edu.python.learning.temp.model.LLMErrorMessage
import com.jetbrains.edu.python.learning.temp.model.LLMMessage
import com.jetbrains.edu.python.learning.temp.model.LLMToolCallMessage
import com.jetbrains.edu.python.learning.temp.model.Message

fun FeatureContext.configureFeatures(onAgentEvent: suspend (Message) -> Unit) {
    install(Tracing)

    install(EventHandler) {
        onToolCallResult = { tool, toolArgs, result ->
            val argsMessage = (toolArgs as? VarArgs)?.asNamedValues()?.toMap()?.toString() ?: toolArgs.toString()
            val resultMessage = result?.toStringDefault() ?: "UNKNOWN TOOL CALL RESULT"
            val escapedResultMessage = try {
                Json.decodeFromString<JsonPrimitive>(resultMessage).jsonPrimitive.content
            } catch (_: Exception) {
                resultMessage
            }
            val message = LLMToolCallMessage(
                toolName = tool.name,
                toolArgs = argsMessage,
                result = escapedResultMessage
            )
            onAgentEvent(message)
        }

        onAfterLLMWithToolsCall = { response, tools ->
            val messageBuilder = StringBuilder()
            messageBuilder.appendLine("LLM Responses:")
            response.forEach { responseMessage ->
                val text = when (responseMessage) {
                    is ai.koog.prompt.message.Message.Assistant -> "message: ${responseMessage.content}"
                    is ai.koog.prompt.message.Message.Tool.Call -> "call: ${responseMessage.tool}, message: ${responseMessage.content}"
                }
                messageBuilder.appendLine("  - $text")
            }

            if (tools.isNotEmpty()) {
                messageBuilder
                    .append("Tools: ")
                    .append("[")
                    .append(tools.joinToString { it.name })
                    .append("]")
                    .appendLine()
            }

            val message = LLMMessage(message = messageBuilder.toString())
            onAgentEvent(message)
        }

        onAgentFinished = { strategyName: String, result: String? ->
            val message = LLMMessage(
                message = "Agent finished with result: $result"
            )
            onAgentEvent(message)
        }

        onAgentRunError = { strategyName, throwable ->
            val message = LLMErrorMessage(
                message = "Agent execution error: ${throwable.message}"
            )
            onAgentEvent(message)
        }
    }
}
