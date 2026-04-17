/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginLookupResult {
  private static final PluginLookupResult NOT_FOUND = new PluginLookupResult(State.NOT_FOUND, null);
  private static final PluginLookupResult UNSUPPORTED = new PluginLookupResult(State.UNSUPPORTED, null);

  private final @NotNull State state;
  private final @Nullable IdePlugin plugin;

  private PluginLookupResult(@NotNull State state, @Nullable IdePlugin plugin) {
    this.state = state;
    this.plugin = plugin;
  }

  public static @NotNull PluginLookupResult found(@NotNull IdePlugin plugin) {
    return new PluginLookupResult(State.FOUND, plugin);
  }

  public static @NotNull PluginLookupResult notFound() {
    return NOT_FOUND;
  }

  public static @NotNull PluginLookupResult unsupported() {
    return UNSUPPORTED;
  }

  public boolean isFound() {
    return state == State.FOUND;
  }

  public boolean isNotFound() {
    return state == State.NOT_FOUND;
  }

  public boolean isUnsupported() {
    return state == State.UNSUPPORTED;
  }

  public @Nullable IdePlugin getPlugin() {
    return plugin;
  }

  private enum State {
    FOUND,
    NOT_FOUND,
    UNSUPPORTED
  }
}
