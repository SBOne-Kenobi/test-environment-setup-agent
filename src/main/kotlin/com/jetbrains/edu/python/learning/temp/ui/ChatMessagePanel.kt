package com.jetbrains.edu.python.learning.temp.ui

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.python.learning.temp.model.MessageType
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.JEditorPane
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet

/**
 * A panel that displays a single chat message.
 * The message is displayed in a bubble with different colors and alignment based on the message type.
 * Different message types have different titles and colors.
 */
class ChatMessagePanel(private val message: String, private val messageType: MessageType) : JPanel(BorderLayout()) {
    private var bubblePanel: JPanel? = null

    init {
        val isUserMessage = messageType == MessageType.USER
        val flowLayout = FlowLayout(if (isUserMessage) FlowLayout.RIGHT else FlowLayout.LEFT)
        flowLayout.hgap = 0
        flowLayout.vgap = 0
        layout = flowLayout

        bubblePanel = createMessageBubble(message, messageType)
        add(bubblePanel)
    }

    /**
     * Converts markdown text to HTML.
     * This is a simple implementation that handles basic markdown features:
     * - Headers (# Header)
     * - Bold (**bold**)
     * - Italic (*italic*)
     * - Code blocks (```code```)
     * - Inline code (`code`)
     * - Links ([text](url))
     * - Lists (- item or * item)
     */
    private fun markdownToHtml(markdown: String): String {
        var html = markdown
            // Escape HTML special characters
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

            // Headers
            .replace(Regex("^# (.+)$", RegexOption.MULTILINE), "<h1>$1</h1>")
            .replace(Regex("^## (.+)$", RegexOption.MULTILINE), "<h2>$1</h2>")
            .replace(Regex("^### (.+)$", RegexOption.MULTILINE), "<h3>$1</h3>")

            // Bold
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")

            // Italic
            .replace(Regex("\\*(.+?)\\*"), "<i>$1</i>")

            // Code blocks
            .replace(Regex("```([\\s\\S]*?)```"), "<pre><code>$1</code></pre>")

            // Inline code
            .replace(Regex("`([^`]+?)`"), "<code>$1</code>")

            // Links
            .replace(Regex("\\[(.+?)\\]\\((.+?)\\)"), "<a href=\"$2\">$1</a>")

            // Lists
            .replace(Regex("^- (.+)$", RegexOption.MULTILINE), "<li>$1</li>")
            .replace(Regex("^\\* (.+)$", RegexOption.MULTILINE), "<li>$1</li>")

            // Paragraphs
            .replace(Regex("\n\n"), "<br/><br/>")

            // Line breaks
            .replace(Regex("\n"), "<br/>")

        // Wrap lists in <ul> tags
        html = html.replace(Regex("(<li>.+</li>)+")) { 
            "<ul>${it.value}</ul>" 
        }

        return "<html><body style='font-family: ${JBUI.Fonts.label().family}; font-size: ${JBUI.Fonts.label().size}pt;'>$html</body></html>"
    }

    private fun createMessageBubble(message: String, messageType: MessageType): JPanel {
        // Create a custom panel that dynamically calculates its preferred size
        val bubblePanel = object : JPanel(BorderLayout()) {
            override fun getPreferredSize(): java.awt.Dimension {
                // Get the parent width (this is the ChatMessagePanel)
                val parentWidth = parent?.width ?: 400

                // Calculate maximum width as 70% of parent width
                val maxWidth = (parentWidth * 0.7).toInt()

                // Get the preferred size based on content
                val baseSize = super.getPreferredSize()

                // Constrain the width while maintaining the aspect ratio
                val constrainedWidth = maxWidth.coerceAtMost(baseSize.width)

                return java.awt.Dimension(constrainedWidth, baseSize.height)
            }
        }

        bubblePanel.border = LineBorder(getBubbleBorderColor(messageType), 1, true)
        bubblePanel.background = getBubbleBackgroundColor(messageType)

        // Add title if it's not a user message
        if (messageType != MessageType.USER) {
            val titleLabel = JLabel(getMessageTitle(messageType))
            titleLabel.font = JBUI.Fonts.label().deriveFont(JBUI.Fonts.label().style or java.awt.Font.BOLD)
            titleLabel.foreground = getTitleColor(messageType)
            titleLabel.border = EmptyBorder(JBUI.insets(8, 8, 0, 8))
            bubblePanel.add(titleLabel, BorderLayout.NORTH)
        }

        // Convert markdown to HTML
        val htmlContent = markdownToHtml(message)

        // Create an editor pane to display HTML content
        val editorPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            border = EmptyBorder(JBUI.insets(8))

            // Set up HTML editor kit with custom styles
            val editorKit = HTMLEditorKit()
            setEditorKit(editorKit)

            // Apply custom styles
            val styleSheet = StyleSheet()
            val isDarkTheme = JBColor.isBright().not()

            // Base styles
            styleSheet.addRule("body { font-family: ${JBUI.Fonts.label().family}; font-size: ${JBUI.Fonts.label().size}pt; }")

            // Code styling with theme-specific colors
            if (isDarkTheme) {
                // Dark theme styles
                styleSheet.addRule("code { font-family: monospace; background-color: #2b2d30; color: #a9b7c6; padding: 2px 5px; border: 1px solid #3c3f41; }")
                styleSheet.addRule("pre { background-color: #2b2d30; padding: 10px; margin: 10px 0; border: 1px solid #3c3f41; }")
                styleSheet.addRule("pre code { background-color: transparent; color: #a9b7c6; padding: 0; border: none; }")
                styleSheet.addRule("a { color: #589df6; text-decoration: none; }")
                styleSheet.addRule("h1, h2, h3 { margin-top: 10px; margin-bottom: 10px; color: #e8e8e8; }")
            } else {
                // Light theme styles
                styleSheet.addRule("code { font-family: monospace; background-color: #f3f3f3; color: #0033b3; padding: 2px 5px; border: 1px solid #e0e0e0; }")
                styleSheet.addRule("pre { background-color: #f8f8f8; padding: 10px; margin: 10px 0; border: 1px solid #e0e0e0; }")
                styleSheet.addRule("pre code { background-color: transparent; color: #000000; padding: 0; border: none; }")
                styleSheet.addRule("a { color: #2b8dd6; text-decoration: none; }")
                styleSheet.addRule("h1, h2, h3 { margin-top: 10px; margin-bottom: 10px; color: #2c2c2c; }")
            }

            // Common styles for both themes
            styleSheet.addRule("a:hover { text-decoration: underline; }")
            styleSheet.addRule("ul { margin-top: 6px; margin-bottom: 6px; padding-left: 20px; }")
            styleSheet.addRule("li { margin-bottom: 4px; }")

            editorKit.styleSheet = styleSheet

            // Set the HTML content
            text = htmlContent

            // Make sure hyperlinks are clickable
            addHyperlinkListener { e ->
                if (e.eventType == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        java.awt.Desktop.getDesktop().browse(e.url.toURI())
                    } catch (ex: Exception) {
                        // Handle exception if needed
                    }
                }
            }
        }

        bubblePanel.add(editorPane, BorderLayout.CENTER)

        // Calculate the initial size for the editor pane
        editorPane.size = editorPane.preferredSize

        // We don't need to set a fixed preferred size for the bubble panel
        // as our overridden getPreferredSize method will handle dynamic sizing

        // However, we still need to set the initial size of the editor pane
        // to ensure proper text wrapping
        val initialWidth = ((parent?.width ?: 400) * 0.7).toInt().coerceAtMost(editorPane.preferredSize.width + 16) - 16
        editorPane.setSize(initialWidth, Int.MAX_VALUE)

        return bubblePanel
    }

    private fun getMessageTitle(messageType: MessageType): String {
        return when (messageType) {
            MessageType.ASSISTANT_LOG -> "Log"
            MessageType.ASSISTANT_FOR_USER -> "Assistant"
            MessageType.ASSISTANT -> "Assistant"
            MessageType.TOOL_CALL -> "Tool Call"
            MessageType.ASSISTANT_ERROR -> "Error"
            else -> ""
        }
    }

    private fun getBubbleBackgroundColor(messageType: MessageType): Color {
        return when (messageType) {
            MessageType.USER -> JBColor(Color(220, 242, 255), Color(45, 95, 140))
            MessageType.ASSISTANT_LOG -> JBColor(Color(245, 245, 245), Color(55, 58, 60))
            MessageType.ASSISTANT_FOR_USER -> JBColor(Color(230, 250, 230), Color(45, 70, 45))
            MessageType.ASSISTANT -> JBColor(Color(250, 250, 250), Color(65, 68, 70))
            MessageType.TOOL_CALL -> JBColor(Color(250, 250, 250), Color(65, 68, 70))
            MessageType.ASSISTANT_ERROR -> JBColor(Color(255, 230, 230), Color(90, 45, 45))
        }
    }

    private fun getBubbleBorderColor(messageType: MessageType): Color {
        return when (messageType) {
            MessageType.USER -> JBColor(Color(180, 220, 250), Color(40, 85, 125))
            MessageType.ASSISTANT_LOG -> JBColor(Color(220, 220, 220), Color(60, 63, 65))
            MessageType.ASSISTANT_FOR_USER -> JBColor(Color(190, 235, 190), Color(40, 65, 40))
            MessageType.ASSISTANT -> JBColor(Color(230, 230, 230), Color(70, 73, 75))
            MessageType.TOOL_CALL -> JBColor(Color(230, 230, 230), Color(70, 73, 75))
            MessageType.ASSISTANT_ERROR -> JBColor(Color(245, 190, 190), Color(80, 40, 40))
        }
    }

    private fun getTitleColor(messageType: MessageType): Color {
        return when (messageType) {
            MessageType.ASSISTANT_LOG -> JBColor(Color(100, 100, 100), Color(180, 180, 180))
            MessageType.ASSISTANT_FOR_USER -> JBColor(Color(0, 100, 0), Color(100, 180, 100))
            MessageType.ASSISTANT -> JBColor(Color(70, 70, 70), Color(200, 200, 200))
            MessageType.TOOL_CALL -> JBColor(Color(70, 70, 70), Color(200, 200, 200))
            MessageType.ASSISTANT_ERROR -> JBColor(Color(180, 0, 0), Color(220, 100, 100))
            else -> JBColor.foreground()
        }
    }
}
