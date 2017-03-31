package com.intellij.structure.domain;

public interface PluginProblem {
    Level getLevel();
    String getMessage();

    enum Level {
        ERROR,
        WARNING
    }
}
