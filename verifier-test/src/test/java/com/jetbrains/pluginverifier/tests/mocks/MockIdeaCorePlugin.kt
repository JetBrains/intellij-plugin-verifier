package com.jetbrains.pluginverifier.tests.mocks

import java.io.File

/**
 * Mock version of the "IDEA CORE" plugin defined
 * in the `/lib/resources.jar`.
 */
fun createMockIdeaCorePlugin(tempFolder: File) = MockIdePlugin(
    pluginId = "com.intellij",
    pluginName = "IDEA CORE",
    originalFile = tempFolder,
    definedModules = setOf(
        "com.intellij.modules.platform",
        "com.intellij.modules.lang",
        "com.intellij.modules.vcs",
        "com.intellij.modules.xml",
        "com.intellij.modules.xdebugger",
        "com.intellij.modules.java",
        "com.intellij.modules.ultimate",
        "com.intellij.modules.all"
    )
)