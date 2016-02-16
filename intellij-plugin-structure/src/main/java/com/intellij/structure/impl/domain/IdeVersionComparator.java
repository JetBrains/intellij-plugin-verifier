package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;

import java.util.Comparator;

/**
 * @author Sergey Patrikeev
 */
public class IdeVersionComparator implements Comparator<IdeVersion> {
  @Override
  public int compare(IdeVersion o1, IdeVersion o2) {
    if (o1.getBranch() > o2.getBranch()) return 1;
    if (o1.getBranch() < o2.getBranch()) return -1;

    if (o1.getBuild() > o2.getBuild()) return 1;
    if (o1.getBuild() < o2.getBuild()) return -1;

    if (o1.getAttempt() > o2.getAttempt()) return 1;
    if (o1.getAttempt() < o2.getAttempt()) return -1;
    return 0;
  }
}
