package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.impl.utils.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Sergey Evdokimov
 */
public class IdeVersionImpl extends IdeVersion {

  private static final String BUILD_NUMBER = "__BUILD_NUMBER__";
  private static final String STAR = "*";
  private static final String SNAPSHOT = "SNAPSHOT";
  private static final String FALLBACK_VERSION = "999.SNAPSHOT";
  private final String myProductCode;
  private final int myBaselineVersion;
  private final int myBuildNumber;
  private final String myAttemptInfo;

  private IdeVersionImpl(@NotNull String productCode, int baselineVersion, int buildNumber, @Nullable String attemptInfo) {
    myProductCode = productCode;
    myBaselineVersion = baselineVersion;
    myBuildNumber = buildNumber;
    myAttemptInfo = StringUtil.isEmpty(attemptInfo) ? null : attemptInfo;
  }

  @NotNull
  public static IdeVersionImpl fromString(@NotNull String version) throws IllegalArgumentException {
    if (BUILD_NUMBER.equals(version)) {
      return new IdeVersionImpl("", Holder.TOP_BASELINE_VERSION, Integer.MAX_VALUE, null);
    }

    String code = version;
    int productSeparator = code.indexOf('-');
    final String productCode;
    if (productSeparator > 0) {
      productCode = code.substring(0, productSeparator);
      code = code.substring(productSeparator + 1);
    } else {
      productCode = "";
    }

    int baselineVersionSeparator = code.indexOf('.');
    int baselineVersion;
    int buildNumber;
    String attemptInfo = null;

    if (baselineVersionSeparator > 0) {
      try {
        String baselineVersionString = code.substring(0, baselineVersionSeparator);
        if (baselineVersionString.trim().isEmpty()) {
          throw new IllegalArgumentException("Invalid version number: " + version);
        }
        baselineVersion = Integer.parseInt(baselineVersionString);
        code = code.substring(baselineVersionSeparator + 1);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Invalid version number: " + version);
      }

      int minorBuildSeparator = code.indexOf('.'); // allow <BuildNumber>.<BuildAttemptNumber> skipping BuildAttemptNumber
      if (minorBuildSeparator > 0) {
        attemptInfo = code.substring(minorBuildSeparator + 1);
        code = code.substring(0, minorBuildSeparator);
      }
      buildNumber = parseBuildNumber(version, code);
    } else {
      buildNumber = parseBuildNumber(version, code);

      if (buildNumber <= 2000) {
        // it's probably a baseline, not a build number
        return new IdeVersionImpl(productCode, buildNumber, 0, null);
      }

      baselineVersion = getBaseLineForHistoricBuilds(buildNumber);
    }

    return new IdeVersionImpl(productCode, baselineVersion, buildNumber, attemptInfo);
  }

  private static int parseBuildNumber(String version, String code) {
    if (SNAPSHOT.equals(code) || STAR.equals(code) || BUILD_NUMBER.equals(code)) {
      return Integer.MAX_VALUE;
    }
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid version number: " + version);
    }
  }

  // See http://www.jetbrains.net/confluence/display/IDEADEV/Build+Number+Ranges for historic build ranges
  private static int getBaseLineForHistoricBuilds(int bn) {
    if (bn == Integer.MAX_VALUE) {
      return Holder.TOP_BASELINE_VERSION; // SNAPSHOTS
    }

    if (bn >= 10000) {
      return 88; // Maia, 9x builds
    }

    if (bn >= 9500) {
      return 85; // 8.1 builds
    }

    if (bn >= 9100) {
      return 81; // 8.0.x builds
    }

    if (bn >= 8000) {
      return 80; // 8.0, including pre-release builds
    }

    if (bn >= 7500) {
      return 75; // 7.0.2+
    }

    if (bn >= 7200) {
      return 72; // 7.0 final
    }

    if (bn >= 6900) {
      return 69; // 7.0 pre-M2
    }

    if (bn >= 6500) {
      return 65; // 7.0 pre-M1
    }

    if (bn >= 6000) {
      return 60; // 6.0.2+
    }

    if (bn >= 5000) {
      return 55; // 6.0 branch, including all 6.0 EAP builds
    }

    if (bn >= 4000) {
      return 50; // 5.1 branch
    }

    return 40;
  }

  @Override
  public String asString(boolean withProductCode, boolean withBuildAttempt) {
    StringBuilder builder = new StringBuilder();

    if (withProductCode && !StringUtil.isEmpty(myProductCode)) {
      builder.append(myProductCode).append('-');
    }

    builder.append(myBaselineVersion).append('.');

    if (myBuildNumber != Integer.MAX_VALUE) {
      builder.append(myBuildNumber);
    } else {
      builder.append(SNAPSHOT);
    }

    if (withBuildAttempt && myAttemptInfo != null) {
      builder.append('.').append(myAttemptInfo);
    }

    return builder.toString();
  }

  @Override
  @NotNull
  public String getProductCode() {
    return myProductCode == null ? "" : myProductCode;
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int getBaselineVersion() {
    return myBaselineVersion;
  }

  @Override
  public int getBuild() {
    return myBuildNumber;
  }

  @Override
  public String getAttempt() {
    return myAttemptInfo;
  }

  @Override
  public boolean isSnapshot() {
    return myBuildNumber == Integer.MAX_VALUE;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IdeVersionImpl that = (IdeVersionImpl) o;

    if (myBaselineVersion != that.myBaselineVersion) return false;
    if (myBuildNumber != that.myBuildNumber) return false;
    if (!myProductCode.equals(that.myProductCode)) return false;
    return StringUtil.equal(myAttemptInfo, that.myAttemptInfo);

  }

  @Override
  public int hashCode() {
    int result = myProductCode.hashCode();
    result = 31 * result + myBaselineVersion;
    result = 31 * result + myBuildNumber;
    if (myAttemptInfo != null) result = 31 * result + myAttemptInfo.hashCode();
    return result;
  }

  private static class Holder {
    private static final int TOP_BASELINE_VERSION = fromString(FALLBACK_VERSION).getBaselineVersion();
  }


}
