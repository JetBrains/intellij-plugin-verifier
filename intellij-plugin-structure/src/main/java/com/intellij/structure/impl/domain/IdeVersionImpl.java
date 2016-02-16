package com.intellij.structure.impl.domain;

import com.google.common.collect.ImmutableBiMap;
import com.intellij.structure.domain.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergey Evdokimov
 */
public class IdeVersionImpl extends IdeVersion {

  private static final Map<String, String> PRODUCT_MAP;
  private static final Pattern PATTERN = Pattern.compile("(?:([A-Z]{1,10})-)?(\\d{1,8})\\.(?:(\\d{1,10})(?:\\.(\\d{1,10}))?|SNAPSHOT)");

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
  private String productCode;
  private int branch;
  private int build;
  private int attempt;
  private boolean isSnapshot;
  private String productName;
  private Integer baselineNumber;


  public IdeVersionImpl(@NotNull String text) throws IllegalArgumentException {
    text = text.trim();

    if (text.isEmpty()) {
      throw new IllegalArgumentException("Empty string is not a correct IdeVersion");
    }

    Matcher matcher = PATTERN.matcher(text);
    if (matcher.matches()) {
      baselineNumber = null;

      productCode = matcher.group(1);
      if (productCode != null) {
        productName = getProductIdByCode(productCode);
        if (productName == null) {
          throw new IllegalArgumentException("Unknown product code " + text);
        }
      }


      branch = Integer.parseInt(matcher.group(2));
      if (matcher.group(3) != null) {
        try {
          build = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException e) {
          build = 0;
          throw new IllegalArgumentException("Couldn't parse build number " + text);
        }

        if (build < 0) {
          throw new IllegalArgumentException("Version build number should be non-negative " + text);
        }
      } else {
        build = Integer.MAX_VALUE;
        isSnapshot = true;
      }
      if (matcher.group(4) != null) {
        try {
          attempt = Integer.parseInt(matcher.group(4));
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Couldn't parse attempt number " + text);
        }
      }

      return;
    }

    try {
      int x = Integer.parseInt(text);
      baselineNumber = x;

      // it's probably a baseline, not a build number
      if (x <= 2000) {
        branch = x;
        build = 0;
        return;
      }

      if (x >= 10000) {
        branch = 90;
      } else if (x >= 9500) {
        branch = 85;
      } else if (x >= 9100) {
        branch = 81;
      } else if (x >= 8000) {
        branch = 80;
      } else if (x >= 7500) {
        branch = 75;
      } else if (x >= 7200) {
        branch = 72;
      } else if (x >= 6900) {
        branch = 69;
      } else if (x >= 6000) {
        branch = 60;
      } else if (x >= 5000) {
        branch = 55;
      } else if (x >= 4000) {
        branch = 50;
      } else {
        branch = 40;
      }

      build = x;

      if (build < 0) {
        build = 0;
        throw new IllegalArgumentException("Version build number should be non-negative " + text);
      }
    } catch (NumberFormatException ignored) {
      throw new IllegalArgumentException("Couldn't parse version " + text);
    }
  }


  @Nullable
  private static String getProductIdByCode(@NotNull String code) {
    return PRODUCT_MAP.get(code);
  }

  @Override
  public String getFullPresentation() {
    if (baselineNumber != null) {
      return baselineNumber.toString();
    }
    String code = getProductCode().isEmpty() ? "" : getProductCode() + "-";
    String notSnapshot = getBuild() + (getAttempt() == 0 ? "" : "." + getAttempt());
    return code + getBranch() + "." + (isSnapshot() ? "SNAPSHOT" : notSnapshot);
  }

  @Override
  public String toString() {
    return getFullPresentation();
  }

  @Override
  @NotNull
  public String getProductCode() {
    return productCode == null ? "" : productCode;
  }

  @NotNull
  @Override
  public String getProductName() {
    return productName == null ? "" : productName;
  }

  @Override
  public int getBranch() {
    return branch;
  }

  @Override
  public int getBuild() {
    return build;
  }

  @Override
  public int getAttempt() {
    return attempt;
  }

  @Override
  public boolean isSnapshot() {
    return isSnapshot;
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

}
