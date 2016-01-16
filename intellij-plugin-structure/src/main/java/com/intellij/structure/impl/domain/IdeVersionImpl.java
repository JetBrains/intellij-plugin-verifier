package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;

/**
 * @author Sergey Evdokimov
 */
public class IdeVersionImpl extends IdeVersion {

  private String productCode;
  private int branch;
  private int build;
  private int attempt;
  private boolean isSnapshot;
  private String productName;

  public IdeVersionImpl(@NotNull String text) throws IllegalArgumentException {
    text = text.trim();

    if (text.isEmpty()) {
      throw new IllegalArgumentException("Empty string is not a correct IdeVersion");
    }

    Matcher matcher = PATTERN.matcher(text);
    if (matcher.matches()) {
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

  @Override
  @NotNull
  public String getProductCode() {
    return productCode == null ? "" : productCode;
  }

  @Override
  @Nullable
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

}
