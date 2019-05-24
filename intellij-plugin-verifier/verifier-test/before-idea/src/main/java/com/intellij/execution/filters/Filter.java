package com.intellij.execution.filters;

public interface Filter {

  Result applyFilter(String line, int entireLength);

  class Result extends ResultItem {

  }

  class ResultItem {

  }

}
