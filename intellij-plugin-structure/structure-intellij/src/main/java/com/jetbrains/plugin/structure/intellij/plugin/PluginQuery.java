package com.jetbrains.plugin.structure.intellij.plugin;

import org.jetbrains.annotations.NotNull;

/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
public class PluginQuery {

    private String identifier;

    private boolean searchId;

    private boolean searchName;

    private boolean searchPluginAliases;

    private boolean searchContentModuleId;

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public boolean searchId() {
        return searchId;
    }

    public boolean searchName() {
        return searchName;
    }

    public boolean searchPluginAliases() {
        return searchPluginAliases;
    }

    public boolean searchContentModuleId() {
        return searchContentModuleId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Searching for '").append(identifier).append("'");
        boolean hasField = false;
        if (searchId) {
            sb.append(" in plugin ID");
            hasField = true;
        }
        if (searchName) {
            if (hasField) {
                sb.append(",");
            }
            sb.append(" in plugin name");
            hasField = true;
        }
        if (searchPluginAliases) {
            if (hasField) {
                sb.append(",");
            }
            sb.append(" in plugin aliases");
            hasField = true;
        }

        if (searchContentModuleId()) {
            if (hasField) {
                sb.append(",");
            }
            sb.append(" in content module identifiers");
            hasField = true;
        }
        return sb.toString();
    }

    public static class Builder {
        private final PluginQuery query = new PluginQuery();

        @NotNull
        public static Builder of(@NotNull String identifier) {
            return new Builder().setIdentifier(identifier);
        }

        @NotNull
        public Builder setIdentifier(@NotNull String identifier) {
            query.identifier = identifier;
            return this;
        }

        @NotNull
        public Builder inId() {
            query.searchId = true;
            return this;
        }

        @NotNull
        public Builder inName() {
            query.searchName = true;
            return this;
        }

        @NotNull
        public Builder inPluginAliases() {
            query.searchPluginAliases = true;
            return this;
        }

        @NotNull
        public Builder inContentModuleId() {
            query.searchContentModuleId = true;
            return this;
        }

        @NotNull
        public PluginQuery build() {
            return query;
        }
    }
}
