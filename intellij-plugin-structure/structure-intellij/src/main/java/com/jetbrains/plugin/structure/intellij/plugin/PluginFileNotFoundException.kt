package com.jetbrains.plugin.structure.intellij.plugin

import java.nio.file.Path

class PluginFileNotFoundException(val file: Path) : IllegalArgumentException("Plugin file $file is not found or does not exist")