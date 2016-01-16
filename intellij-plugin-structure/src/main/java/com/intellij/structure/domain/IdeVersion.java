package com.intellij.structure.domain;

import com.google.common.collect.ImmutableBiMap;
import com.intellij.structure.impl.domain.IdeVersionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeVersion {

  public static final Comparator<IdeVersion> VERSION_COMPARATOR = new IdeVersionComparator();

  protected static final Pattern PATTERN = Pattern.compile("(?:([A-Z]{1,10})-)?(\\d{1,8})\\.(?:(\\d{1,10})(?:\\.(\\d{1,10}))?|SNAPSHOT)");

  private static final Map<String, String> PRODUCT_MAP;

  static {
    PRODUCT_MAP = ImmutableBiMap.<String, String>builder()
        .put("IC", "idea_ce")
        .put("IU", "idea")
        .put("RM", "ruby")
        .put("WS", "webStorm")
        .put("PS", "phpStorm")
        .put("PY", "pycharm")
        .put("PC", "pycharm_ce")
        .put("OC", "objc")
        .put("MPS", "mps")
        .put("AI", "androidstudio")
        .put("DB", "dbe")
        .put("CL", "clion")
        .build();
  }

  protected IdeVersion() {
  }

  @Nullable
  protected static String getProductIdByCode(@NotNull String code) {
    return PRODUCT_MAP.get(code);
  }

  /**
   * @throws IllegalArgumentException if specified {@code text} doesn't represent correct {@code IdeVersion}
   */
  @NotNull
  public static IdeVersion createIdeVersion(@NotNull String text) throws IllegalArgumentException {
    return new IdeVersionImpl(text);
  }

  @Override
  public String toString() {
    if (!isSnapshot() && getProductCode().isEmpty() && getBuild() == 0 && getAttempt() == 0) {
      //it's a baseline (e.g. "134")
      return Integer.toString(getBranch());
    }
    String code = getProductCode().isEmpty() ? "" : getProductCode() + "-";
    String notSnapshot = getBuild() + (getAttempt() == 0 ? "" : "." + getAttempt());
    return code + getBranch() + "." + (isSnapshot() ? "SNAPSHOT" : notSnapshot);
  }


  @Override
  public int hashCode() {
    int hc = getProductCode().hashCode();
    hc += getBranch();
    hc += getBuild();
    hc += getAttempt();
    hc *= isSnapshot() ? 1 : -1;
    return hc;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof IdeVersion)) {
      return false;
    }
    IdeVersion o = (IdeVersion) obj;
    return getProductCode().equals(o.getProductCode())
        && getBranch() == o.getBranch()
        && getBuild() == o.getBuild()
        && getAttempt() == o.getAttempt()
        && isSnapshot() == o.isSnapshot();
  }

  @NotNull
  public abstract String getProductCode();

  @Nullable
  public abstract String getProductName();

  public abstract int getBranch();

  public abstract int getBuild();

  public abstract int getAttempt();

  public abstract boolean isSnapshot();

  private static class IdeVersionComparator implements Comparator<IdeVersion> {
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
}
