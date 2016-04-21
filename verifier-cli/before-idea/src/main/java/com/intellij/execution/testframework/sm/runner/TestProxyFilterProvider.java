package com.intellij.execution.testframework.sm.runner;

import com.intellij.execution.filters.Filter;

public interface TestProxyFilterProvider {
  Filter getFilter(String nodeType, String nodeName, String nodeArguments);
}
