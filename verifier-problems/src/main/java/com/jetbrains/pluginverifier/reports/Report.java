package com.jetbrains.pluginverifier.reports;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public interface Report {

  @NotNull
  List<Details> details();


}
