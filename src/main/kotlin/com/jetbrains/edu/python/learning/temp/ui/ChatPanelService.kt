package com.jetbrains.edu.python.learning.temp.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.python.learning.temp.agent.EnvironmentSetupAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

val Project.chatPanel: ChatPanel
    get() = service<ChatPanelService>().chatPanel

@Service(Service.Level.PROJECT)
class ChatPanelService(val project: Project, val coroutineScope: CoroutineScope) {
    val chatPanel = ChatPanel()

    fun startAgent(inputMessage: String) {
        coroutineScope.launch {
            val agent = EnvironmentSetupAgent(project)
            agent.execute(inputMessage) { chatPanel.addMessage(it) }
        }
    }
}
