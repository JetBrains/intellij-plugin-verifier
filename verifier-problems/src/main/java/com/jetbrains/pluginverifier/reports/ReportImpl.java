package com.jetbrains.pluginverifier.reports;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Sergey Patrikeev
 */
public class ReportImpl implements Report {

  private final List<Details> myDetails;

  public ReportImpl(@NotNull List<Details> details) {
    myDetails = new ArrayList<Details>(details);
  }

  @NotNull
  @Override
  public List<Details> details() {
    return Collections.unmodifiableList(myDetails);
  }

}
