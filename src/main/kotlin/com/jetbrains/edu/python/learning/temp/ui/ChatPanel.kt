package com.jetbrains.edu.python.learning.temp.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.python.learning.temp.model.AgentMessage
import com.jetbrains.edu.python.learning.temp.model.LLMMessage
import com.jetbrains.edu.python.learning.temp.model.Message
import com.jetbrains.edu.python.learning.temp.model.MessageType
import com.jetbrains.edu.python.learning.temp.model.UserMessage
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.Box
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

/**
 * A panel that displays a chat conversation with messages from both the user and the agent.
 * Messages are displayed in a vertical list with user messages aligned to the right and agent messages aligned to the left.
 */
class ChatPanel : JPanel(BorderLayout()) {
    private val messagesPanel = JPanel(GridBagLayout())
    private val scrollPane: JBScrollPane

    // Store all messages to allow refreshing the view
    private val allMessages = mutableListOf<Pair<String, MessageType>>()

    /**
     * When true, only shows user messages and assistant messages intended for the user.
     * Other message types like logs, tool calls, and errors will be hidden.
     */
    var showOnlyUserAndAssistantForUser: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                refreshMessages()
            }
        }

    init {
        messagesPanel.border = JBUI.Borders.empty(10)

        scrollPane = JBScrollPane(messagesPanel).apply {
            horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
            verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
            border = JBUI.Borders.empty()
        }

        add(scrollPane, BorderLayout.CENTER)
    }

    /**
     * Adds a message to the chat panel.
     * 
     * @param message The Message object to add
     */
    fun addMessage(message: Message) {
        val messageText = when (message) {
            is UserMessage -> message.message
            is AgentMessage -> message.message
            else -> "Unknown message type"
        }

        addMessageInternal(messageText, message.messageType)
    }

    /**
     * Adds a user message to the chat panel.
     * 
     * @param message The message text to add
     */
    fun addUserMessage(message: String) {
        addMessage(UserMessage(message))
    }

    /**
     * Adds an agent log message to the chat panel.
     * This is the default type of message from the LLM.
     * 
     * @param message The message text to add
     */
    fun addAgentMessage(message: String) {
        addMessage(LLMMessage(message))
    }

    /**
     * Adds an agent message intended for the user to the chat panel.
     * 
     * @param message The message text to add
     */
    fun addAgentForUserMessage(message: String) {
        addMessage(LLMMessage(message, MessageType.ASSISTANT_FOR_USER))
    }

    /**
     * Adds a message to the chat panel.
     * 
     * @param message The message text to add
     * @param messageType The type of the message
     */
    private fun addMessageInternal(message: String, messageType: MessageType) {
        // Store the message in our list
        allMessages.add(Pair(message, messageType))

        // If showOnlyUserAndAssistantForUser is true, only show USER and ASSISTANT_FOR_USER messages
        if (showOnlyUserAndAssistantForUser && 
            messageType != MessageType.USER && 
            messageType != MessageType.ASSISTANT_FOR_USER) {
            return
        }

        addMessageToPanel(message, messageType)
    }

    /**
     * Adds a message to the panel without filtering.
     */
    private fun addMessageToPanel(message: String, messageType: MessageType) {
        val isUserMessage = messageType == MessageType.USER
        val messagePanel = ChatMessagePanel(message, messageType)

        val constraints = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
            anchor = if (isUserMessage) GridBagConstraints.NORTHEAST else GridBagConstraints.NORTHWEST
            insets = JBUI.insets(5)
        }

        messagesPanel.add(messagePanel, constraints)

        // Add vertical spacing between messages
        val spacer = Box.createVerticalStrut(JBUI.scale(5))
        val spacerConstraints = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            weightx = 1.0
            fill = GridBagConstraints.HORIZONTAL
        }
        messagesPanel.add(spacer, spacerConstraints)

        // Revalidate and repaint to update the UI
        messagesPanel.revalidate()
        messagesPanel.repaint()

        // Scroll to the bottom to show the new message
        SwingUtilities.invokeLater {
            val verticalBar = scrollPane.verticalScrollBar
            verticalBar.value = verticalBar.maximum
        }
    }

    /**
     * Refreshes the messages panel based on the current value of showOnlyUserAndAssistantForUser.
     */
    private fun refreshMessages() {
        // Clear the messages panel
        messagesPanel.removeAll()

        // Add messages based on the current filter setting
        for ((message, messageType) in allMessages) {
            if (!showOnlyUserAndAssistantForUser || 
                messageType == MessageType.USER || 
                messageType == MessageType.ASSISTANT_FOR_USER) {
                addMessageToPanel(message, messageType)
            }
        }

        // Revalidate and repaint to update the UI
        messagesPanel.revalidate()
        messagesPanel.repaint()

        // Scroll to the bottom
        SwingUtilities.invokeLater {
            val verticalBar = scrollPane.verticalScrollBar
            verticalBar.value = verticalBar.maximum
        }
    }
}
