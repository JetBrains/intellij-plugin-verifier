package com.intellij.structure.domain;

import com.google.common.collect.ImmutableBiMap;
import com.intellij.structure.impl.domain.IdeVersionImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeVersion {

  public static final Comparator<IdeVersion> VERSION_COMPARATOR = new Comparator<IdeVersion>() {
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
  };
  protected static final Pattern PATTERN = Pattern.compile("(?:(IC|IU|RM|WS|PS|PY|PC|OC|MPS|AI|DB|CL)-)?(\\d{1,8})\\.(?:(\\d{1,10})(?:\\.(\\d{1,10}))?|SNAPSHOT)");

  private static final Map<String, String> PRODUCT_MAP = new HashMap<String, String>();
  private static final Map<String, String> PRODUCT_ID_TO_CODE;

  static {
    PRODUCT_MAP.put("IC", "idea_ce");
    PRODUCT_MAP.put("IU", "idea");
    PRODUCT_MAP.put("RM", "ruby");
    PRODUCT_MAP.put("WS", "webStorm");
    PRODUCT_MAP.put("PS", "phpStorm");
    PRODUCT_MAP.put("PY", "pycharm");
    PRODUCT_MAP.put("PC", "pycharm_ce");
    PRODUCT_MAP.put("OC", "objc");
    PRODUCT_MAP.put("MPS", "mps");
    PRODUCT_MAP.put("AI", "androidstudio");
    PRODUCT_MAP.put("DB", "dbe");
    PRODUCT_MAP.put("CL", "clion");

    PRODUCT_ID_TO_CODE = ImmutableBiMap.copyOf(PRODUCT_MAP).inverse();
  }

  protected IdeVersion() {
  }

  public static String getProductIdByCode(String code) {
    return PRODUCT_MAP.get(code);
  }

  public static String getCodeByProductId(String productId) {
    return PRODUCT_ID_TO_CODE.get(productId);
  }

  @NotNull
  public static IdeVersion createIdeVersion(@Nullable String text) {
    return new IdeVersionImpl(text);
  }

  public abstract String getProductCode();

  public abstract String getProductName();

  public abstract int getBranch();

  public abstract int getBuild();

  public abstract int getAttempt();

  public abstract boolean isOk();

  public abstract boolean isSnapshot();
}
