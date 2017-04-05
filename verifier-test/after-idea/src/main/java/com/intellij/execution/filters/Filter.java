package com.intellij.execution.filters;

/**
 * @author Sergey Patrikeev
 */
public interface Filter {

  Result applyFilter(String line, int entireLength);

  class Result extends ResultItem {

  }

  class ResultItem {

  }

}
