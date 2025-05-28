package com.jetbrains.edu.python.learning.temp.agent

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.ext.agent.subgraphWithTask
import com.intellij.openapi.project.Project
import com.jetbrains.edu.python.learning.temp.ui.chatPanel

object EnvironmentSetupStrategies {
    fun fixIssueStrategy(project: Project, tools: EnvironmentSetupTools): AIAgentStrategy =
        strategy("simple-environment-setup-agent") {
            val subgraphIssueInformationCollection by subgraphWithTask<String>(
                tools = tools.asTools(),
                shouldTLDRHistory = false,
            ) { input ->
                "Start investigate issue without resolving. Do not try to fix the problem for now only collect information about it."
            }
            val nodeHypothesisMaking by nodeLLMRequest(allowToolCalls = false)
            val nodePlanning by nodeLLMRequest(allowToolCalls = false)

            val subgraphIssueResolution by subgraphWithTask<String>(
                tools = tools.asTools(),
                shouldTLDRHistory = false,
            ) { input ->
                "Resolve issue aligning with the plan"
            }

            val subgraphCheckIssueResolved by subgraphWithTask<String>(
                tools = tools.asTools(),
                shouldTLDRHistory = false,
            ) { input ->
                "Check and verify if issue resolved by the provided solution"
            }

            edge(nodeStart forwardTo subgraphIssueInformationCollection)
            edge(subgraphIssueInformationCollection forwardTo nodeHypothesisMaking transformed {
                """
                    Make a hypothesis. The hypothesis should provide description what is wrong.
                    Do not resolve issue. Reflect on what might go wrong to explain it and teach how to you discovered it.
                """.trimIndent()
            })
            edge(nodeHypothesisMaking forwardTo nodePlanning onAssistantMessage { true } transformed {
                project.chatPanel.addAgentForUserMessage(it)
                """
                    Plan how to resolve the issue. Describe the reason and purpose of each step.
                """.trimIndent()
            })
            edge(nodePlanning forwardTo subgraphIssueResolution onAssistantMessage { true } transformed {
                project.chatPanel.addAgentForUserMessage(it)
                it
            })
            edge(subgraphIssueResolution forwardTo subgraphCheckIssueResolved transformed { it.result })
            edge(subgraphCheckIssueResolved forwardTo nodeFinish transformed {
                project.chatPanel.addAgentForUserMessage(it.result)
                it.result
            })
        }
}
