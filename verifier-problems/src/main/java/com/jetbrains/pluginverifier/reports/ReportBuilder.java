package com.jetbrains.pluginverifier.reports;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class ReportBuilder {

  private final List<Details> myDetails = new ArrayList<Details>();

  public ReportBuilder add(@NotNull Details details) {
    myDetails.add(details);
    return this;
  }

  public Report build() {
    return new ReportImpl(myDetails);
  }
}
