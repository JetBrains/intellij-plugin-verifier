package com.intellij.structure.utils;

import com.google.common.collect.ImmutableBiMap;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Sergey Evdokimov
 */
public class ProductUpdateBuild {

  public static final Pattern PATTERN = Pattern.compile("(?:(IC|IU|RM|WS|PS|PY|PC|OC|MPS|AI|DB|CL)-)?(\\d{1,8})\\.(?:(\\d{1,10})(?:\\.(\\d{1,10}))?|SNAPSHOT)");
  public static final Comparator<ProductUpdateBuild> VERSION_COMPARATOR = new Comparator<ProductUpdateBuild>() {
    @Override
    public int compare(ProductUpdateBuild o1, ProductUpdateBuild o2) {
      if (o1.getBranch() > o2.getBranch()) return 1;
      if (o1.getBranch() < o2.getBranch()) return -1;

      if (o1.getBuild() > o2.getBuild()) return 1;
      if (o1.getBuild() < o2.getBuild()) return -1;

      if (o1.getAttempt() > o2.getAttempt()) return 1;
      if (o1.getAttempt() < o2.getAttempt()) return -1;
      return 0;
    }
  };
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

  private String productCode;
  private int branch;
  private int build;
  private int attempt;
  private boolean isOk;
  private boolean isSnapshot;

  public ProductUpdateBuild(@Nullable String text) {
    if (text == null) return;

    text = text.trim();

    if (text.length() == 0) return;

    isOk = true;

    Matcher matcher = PATTERN.matcher(text);
    if (matcher.matches()) {
      productCode = matcher.group(1);
      branch = Integer.parseInt(matcher.group(2));
      if (matcher.group(3) != null) {
        try {
          build = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException e) {
          build = 0;
          isOk = false;
        }

        assert build >= 0;
      } else {
        build = Integer.MAX_VALUE;
        isSnapshot = true;
      }
      if (matcher.group(4) != null) {
        try {
          attempt = Integer.parseInt(matcher.group(4));
        } catch (NumberFormatException e) {
          isOk = false;
        }
      }

      return;
    }

    try {
      int x = Integer.parseInt(text);

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
        isOk = false;
      }
    } catch (NumberFormatException ignored) {
      isOk = false;
    }
  }

  public static String getProductIdByCode(String code) {
    return PRODUCT_MAP.get(code);
  }

  public static String getCodeByProductId(String productId) {
    return PRODUCT_ID_TO_CODE.get(productId);
  }

  public String getProductCode() {
    return productCode == null ? "" : productCode;
  }

  public String getProductName() {
    return getProductIdByCode(productCode);
  }

  public int getBranch() {
    return branch;
  }

  public int getBuild() {
    return build;
  }

  public int getAttempt() {
    return attempt;
  }

  public boolean isOk() {
    return isOk;
  }

  public boolean isSnapshot() {
    return isSnapshot;
  }

  public Map<String, Object> getLegacyBranchBuild() {
    Object min = "";
    Object max = "";

    if (isOk) {
      switch (branch) {
        case 90:
          min = 10000;
          max = 10000 + build;
          break;
        case 85:
          min = 9500;
          max = 9999;
          break;
        case 81:
          min = 9100;
          max = 9499;
          break;
        case 80:
          min = 8000;
          max = 9099;
          break;
        case 75:
          min = 7500;
          max = 7999;
          break;
        case 72:
          min = 7200;
          max = 7499;
          break;
        case 69:
          min = 6900;
          max = 7199;
          break;
        case 65:
          min = 6500;
          max = 6899;
          break;
        case 60:
          min = 6000;
          max = 6499;
          break;
        case 55:
          min = 5000;
          max = 5999;
          break;
        case 50:
          min = 4000;
          max = 4999;
          break;
        default:
          min = 0;
          max = 3999;
          break;
      }
    }

    Map<String, Object> res = new HashMap<String, Object>();
    res.put("min", min);
    res.put("max", max);

    return res;
  }

}
