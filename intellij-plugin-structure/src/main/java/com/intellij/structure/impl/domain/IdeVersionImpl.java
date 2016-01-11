package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * @author Sergey Evdokimov
 */
public class IdeVersionImpl extends IdeVersion {

  private String productCode;
  private int branch;
  private int build;
  private int attempt;
  private boolean isOk;
  private boolean isSnapshot;

  public IdeVersionImpl(@Nullable String text) {
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
          isOk = false;
        }
      }

      return;
    }

    try {
      int x = Integer.parseInt(text);

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
        isOk = false;
      }
    } catch (NumberFormatException ignored) {
      isOk = false;
    }
  }

  @Override
  public String getProductCode() {
    return productCode == null ? "" : productCode;
  }

  @Override
  public String getProductName() {
    return getProductIdByCode(productCode);
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
  public boolean isOk() {
    return isOk;
  }

  @Override
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
