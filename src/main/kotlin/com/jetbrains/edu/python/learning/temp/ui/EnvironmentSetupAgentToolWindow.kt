package com.jetbrains.edu.python.learning.temp.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.python.learning.temp.model.LLMMessage
import com.jetbrains.edu.python.learning.temp.model.UserMessage
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.border.EmptyBorder

/**
 * Tool window for the Environment Setup Agent chat interface.
 * This will display a chat with the environment setup agent.
 */
class EnvironmentSetupAgentToolWindow : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createChatPanel(project), null, false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createChatPanel(project: Project): JPanel {
        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(JBUI.insets(10))

        // Chat history panel
        val chatPanel = project.chatPanel

        // Create a control panel for buttons at the top
        val controlPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
        controlPanel.border = JBUI.Borders.empty(0, 0, 5, 0)

        // Add toggle button for showing/hiding logs
        val toggleLogsButton = JToggleButton("Hide Logs")
        toggleLogsButton.addActionListener {
            val showLogs = !toggleLogsButton.isSelected
            chatPanel.showOnlyUserAndAssistantForUser = !showLogs
            toggleLogsButton.text = if (!showLogs) "Show Logs" else "Hide Logs"
        }
        controlPanel.add(toggleLogsButton)

        // Add control panel to the top
        panel.add(controlPanel, BorderLayout.NORTH)

        // Add chat panel to the center
        panel.add(chatPanel, BorderLayout.CENTER)

        // Add initial welcome message
        chatPanel.addAgentForUserMessage("Welcome to the Environment Setup Agent!\nHow can I help you set up your Python environment today?")

        // Input field and send button panel
        val inputPanel = JPanel(BorderLayout())
        inputPanel.border = JBUI.Borders.empty(10, 0, 0, 0)

        val inputField = JTextField("I tried to execute python 'print(\"Hello, World!\")', but it's not working")
        inputPanel.add(inputField, BorderLayout.CENTER)

        val sendButton = JButton("Send")
        inputPanel.add(sendButton, BorderLayout.EAST)

        // Action to send a message
        val sendAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val messageText = inputField.text.trim()
                if (messageText.isNotEmpty()) {
                    chatPanel.addUserMessage(messageText)
                    inputField.text = ""

                    // Start the agent with the user's message
                    project.service<ChatPanelService>().startAgent(messageText)
                }
            }
        }

        // Connect the action to the button and Enter key
        sendButton.addActionListener(sendAction)
        inputField.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    sendAction.actionPerformed(ActionEvent(this, ActionEvent.ACTION_PERFORMED, null))
                }
            }
        })

        panel.add(inputPanel, BorderLayout.SOUTH)

        return panel
    }

    companion object {
        const val ID = "Environment Setup Agent"
    }
}
