package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;

public class VerificationContext {
  private Idea myIdea;
  private IdeaPlugin myPlugin;

  public VerificationContext(final Idea idea, final IdeaPlugin plugin) {
    myIdea = idea;
    myPlugin = plugin;
  }

  public Idea getIdea() {
    return myIdea;
  }

  public IdeaPlugin getPlugin() {
    return myPlugin;
  }
}
