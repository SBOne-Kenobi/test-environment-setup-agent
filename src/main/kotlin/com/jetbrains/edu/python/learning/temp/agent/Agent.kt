package com.jetbrains.edu.python.learning.temp.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.grazie.JetBrainsAIModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.python.learning.temp.model.Message
import io.github.oshai.kotlinlogging.KotlinLogging


class EnvironmentSetupAgent(val project: Project) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val token: String by lazy {
        System.getenv("GRAZIE_TOKEN") ?: error("GRAZIE_TOKEN environment variable is not set")
    }

    private val openAiToken: String by lazy {
        System.getenv("OPENAI_TOKEN") ?: error("OPENAI_TOKEN environment variable is not set")
    }

    suspend fun execute(issueDescription: String, onAgentEvent: suspend (Message) -> Unit): String? {
        val agentConfig = AIAgentConfig(
            prompt = prompt("environment_setup_agent_system_prompt") {
                system(EnvironmentSetupPrompts.fixIssueSystemPrompt)
            },
//            model = JetBrainsAIModels.OpenAI.GPT4o,
            model = OpenAIModels.Chat.GPT4o,
            maxAgentIterations = 100,
        )

        val executor =
            simpleOpenAIExecutor(openAiToken)
//            EnvironmentSetupExecutor(token).getLLMExecutor()

        val agentToolSet = EnvironmentSetupTools(project)

        val toolRegistry = ToolRegistry {
            tools(agentToolSet.asTools())
        }

        val strategy = EnvironmentSetupStrategies.fixIssueStrategy(project, agentToolSet)

        val agent = AIAgent(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry,
            installFeatures = { configureFeatures(onAgentEvent) },
        )

        val agentResult = agent.runAndGetResult(issueDescription)

        logger.info { "Agent finished with result: $agentResult" }

        return agentResult
    }
}
