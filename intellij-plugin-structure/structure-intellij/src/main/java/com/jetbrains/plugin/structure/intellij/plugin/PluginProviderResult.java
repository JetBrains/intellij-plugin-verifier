package com.jetbrains.plugin.structure.intellij.plugin;

/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

/**
 * A plugin along with its type indicating how that plugin was resolved.
 * <p>
 * Plugin can be resolved either via its name or via its module name (plugin alias).
 */
public class PluginProviderResult {
    public enum Type {
        PLUGIN, MODULE
    }

    private final Type type;
    private final IdePlugin plugin;

    public PluginProviderResult(Type type, IdePlugin plugin) {
        this.type = type;
        this.plugin = plugin;
    }

    public Type getType() {
        return type;
    }

    public IdePlugin getPlugin() {
        return plugin;
    }
}
