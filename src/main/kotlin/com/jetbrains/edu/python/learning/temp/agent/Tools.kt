package com.jetbrains.edu.python.learning.temp.agent

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.SystemInfo
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

@LLMDescription("Tools for setting up the environment")
class EnvironmentSetupTools(val project: Project) : ToolSet {

    private fun askUserFor(command: String, purpose: String): Boolean {
        val result = AtomicBoolean(false)
        ApplicationManager.getApplication().invokeAndWait {
            val dialogResult = Messages.showYesNoDialog(
                "Execute command:\n$command\n\nPurpose: $purpose",
                "Command Execution Confirmation",
                Messages.getQuestionIcon()
            )
            result.set(dialogResult == Messages.YES)
        }
        return result.get()
    }

    /**
     * Executes a command using IntelliJ IDEA's API instead of direct bash command execution.
     * This method is a replacement for the executeBashCommand method.
     */
    private fun executeCommandUnsafe(command: String): String {
        try {
            val commandLine = if (SystemInfo.isWindows) {
                GeneralCommandLine("cmd.exe", "/c", command)
            } else {
                GeneralCommandLine("bash", "-c", command)
            }

            val processHandler = CapturingProcessHandler(commandLine)
            val processOutput = processHandler.runProcess(60000) // 60 seconds timeout

            return """
                Command execution exit code: ${processOutput.exitCode}
                Std output:
                ```
                ${processOutput.stdout}
                ```
                Err output: 
                ```
                ${processOutput.stderr}
                ```
            """.trimIndent()
        } catch (e: Exception) {
            return """
                Command execution failed:
                ```
                ${e.message}
                ```
            """.trimIndent()
        }
    }

    @Tool
    @LLMDescription("Gets the list of installed Python versions on the system.")
    fun getInstalledPythonVersions(): String {
        // Check for Python 3 installations
        val python3Result = executeCommandUnsafe(
            "command -v python3 && python3 --version 2>&1 || echo 'Python 3 not found'",
        )

        // Check for Python 2 installations (if needed)
        val python2Result = executeCommandUnsafe(
            "command -v python2 && python2 --version 2>&1 || echo 'Python 2 not found'",
        )

        // Check for 'python' command which could be either version
        val pythonResult = executeCommandUnsafe(
            "command -v python && python --version 2>&1 || echo 'Python command not found'",
        )

        // Check for pyenv installations
        val pyenvResult = executeCommandUnsafe(
            "command -v pyenv && pyenv versions || echo 'pyenv not found'",
        )

        return """
            Installed Python versions:

            Python 3:
            $python3Result

            Python 2:
            $python2Result

            Default Python:
            $pythonResult

            Pyenv versions:
            $pyenvResult
        """.trimIndent()
    }

    @Tool
    @LLMDescription("Installs Python using the appropriate method for the current operating system.")
    fun installPython(
        @LLMDescription("The Python version to install (e.g., '3.9', '3.10', '3.11')")
        version: String,
        @LLMDescription("The purpose or reason of installing python.")
        purpose: String,
    ): String {
        if (!askUserFor("Install python $version", purpose)) {
            return "User permitted installation"
        }

        // Detect OS
        val osName = System.getProperty("os.name").lowercase()

        return when {
            osName.contains("linux") -> {
                // For Ubuntu/Debian-based systems
                val aptResult = executeCommandUnsafe(
                    "apt-get update && apt-get install -y python$version python$version-venv python$version-dev",
                )

                if (aptResult.contains("E: Unable to locate package")) {
                    // Try with deadsnakes PPA if the package is not found
                    val ppaResult = executeCommandUnsafe(
                        "add-apt-repository -y ppa:deadsnakes/ppa && apt-get update && apt-get install -y python$version python$version-venv python$version-dev",
                    )
                    ppaResult
                } else {
                    aptResult
                }
            }

            osName.contains("mac") || osName.contains("darwin") -> {
                // For macOS using Homebrew
                val brewResult = executeCommandUnsafe(
                    "brew update && brew install python@$version",
                )
                brewResult
            }

            osName.contains("windows") -> {
                // For Windows, suggest using the installer or winget
                """
                    To install Python $version on Windows, please:

                    1. Download the installer from https://www.python.org/downloads/
                    2. Run the installer and make sure to check "Add Python to PATH"

                    Alternatively, if you have winget installed, you can run:
                    winget install Python.Python.$version

                    Please run this command in a Windows command prompt or PowerShell.
                """.trimIndent()
            }

            else -> {
                // Try using pyenv as a fallback for any OS
                val pyenvResult = executeCommandUnsafe(
                    "command -v pyenv || curl https://pyenv.run | bash && pyenv install $version",
                )
                pyenvResult
            }
        }
    }

    @Tool
    @LLMDescription("Installs a Python package using pip.")
    fun installPythonPackage(
        @LLMDescription("The name of the package to install")
        packageName: String,
        @LLMDescription("The version of the package to install")
        version: String,
        @LLMDescription("The Python executable to use (e.g., 'python', 'python3', 'python3.9')")
        pythonExecutable: String,
    ): String {
        val packageSpec = if (version.isNotEmpty()) "$packageName==$version" else packageName

        if (!askUserFor(
                "$pythonExecutable -m pip install $packageSpec",
                "Install Python package $packageName${if (version.isNotEmpty()) " version $version" else ""}"
            )
        ) {
            return "User denied package installation"
        }

        return executeCommandUnsafe(
            "$pythonExecutable -m pip install $packageSpec",
        )
    }

    @Tool
    @LLMDescription("Creates a Python virtual environment.")
    fun createVirtualEnvironment(
        @LLMDescription("The purpose or reason for creating the virtual environment")
        purpose: String,
        @LLMDescription("The path where the virtual environment should be created")
        path: String,
        @LLMDescription("The Python executable to use (e.g., 'python3', 'python3.9')")
        pythonExecutable: String = "python3",
    ): String {
        if (!askUserFor(
                "$pythonExecutable -m venv $path",
                purpose
            )
        ) {
            return "User denied virtual environment creation"
        }

        // Create the directory if it doesn't exist
        val directory = File(path)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        return executeCommandUnsafe(
            "$pythonExecutable -m venv $path",
        )
    }

    @Tool
    @LLMDescription("Lists installed Python packages in the current environment or a specified virtual environment.")
    fun listInstalledPackages(
        @LLMDescription("The Python executable to use (e.g., 'python3', 'python3.9')")
        pythonExecutable: String = "python3",
        @LLMDescription("The path to the virtual environment (optional)")
        virtualEnvPath: String = "",
    ): String {
        val command = if (virtualEnvPath.isEmpty()) {
            "$pythonExecutable -m pip list"
        } else {
            "source $virtualEnvPath/bin/activate && pip list && deactivate"
        }

        return executeCommandUnsafe(
            command,
        )
    }

    @Tool
    @LLMDescription("Checks if a specific Python package is installed and returns its version.")
    fun checkPackageVersion(
        @LLMDescription("The name of the package to check")
        packageName: String,
        @LLMDescription("The Python executable to use (e.g., 'python3', 'python3.9')")
        pythonExecutable: String = "python3",
    ): String {
        return executeCommandUnsafe(
            "$pythonExecutable -m pip show $packageName",
        )
    }

    @Tool
    @LLMDescription("Runs a Python script with the specified Python interpreter.")
    fun runPythonScript(
        @LLMDescription("The content of the Python script to run")
        scriptContent: String,
        @LLMDescription("The Python executable to use (e.g., 'python3', 'python3.9')")
        pythonExecutable: String = "python3",
    ): String {
        if (!askUserFor(
                "$pythonExecutable script.py",
                "Execute Python script:\n$scriptContent"
            )
        ) {
            return "User denied script execution"
        }

        // Create a temporary file for the script
        val tempFile = File.createTempFile("temp_script_", ".py")
        tempFile.writeText(scriptContent)
        tempFile.setExecutable(true)

        val result = executeCommandUnsafe(
            "$pythonExecutable ${tempFile.absolutePath}",
        )

        // Clean up the temporary file
        tempFile.delete()

        return result
    }

    @Tool
    @LLMDescription("Lists all Python SDKs configured in IntelliJ IDEA.")
    fun listConfiguredPythonSdks(): String {
        val jdkTable = ProjectJdkTable.getInstance()
        val allSdks = jdkTable.allJdks

        val pythonSdks = allSdks.filter { it.sdkType.name.contains("Python", ignoreCase = true) }

        if (pythonSdks.isEmpty()) {
            return "No Python SDKs are configured in IntelliJ IDEA."
        }

        val sdkInfo = pythonSdks.joinToString("\n\n") { sdk ->
            """
                SDK Name: ${sdk.name}
                SDK Home Path: ${sdk.homePath}
                SDK Version: ${sdk.versionString}
            """.trimIndent()
        }

        return """
            Configured Python SDKs in IntelliJ IDEA:

            $sdkInfo
        """.trimIndent()
    }
}
