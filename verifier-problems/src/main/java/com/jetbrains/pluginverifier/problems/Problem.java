package com.jetbrains.pluginverifier.problems;

import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Sergey Evdokimov
 */
public abstract class Problem {

  private String myUid;

  @XmlTransient
  public abstract String getDescription();

  @NotNull
  protected final String evaluateUID(String... params) {
    StringBuilder res = new StringBuilder();

    res.append(getClass().getSimpleName());

    for (String param : params) {
      res.append('|');
      res.append(param.replace("|", "||"));
    }

    return res.toString();
  }

  @NotNull
  protected abstract String evaluateUID();

  protected void cleanUid() {
    myUid = null;
  }

  @NotNull
  @XmlTransient
  public final String getUID() {
    if (myUid == null) {
      myUid = evaluateUID();
    }
    return myUid;
  }

  @Override
  public final boolean equals(Object obj) {
    return obj instanceof Problem && getUID().equals(((Problem)obj).getUID());
  }

  @Override
  public final int hashCode() {
    return getUID().hashCode();
  }
}
