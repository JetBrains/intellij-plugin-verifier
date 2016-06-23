package com.intellij.structure.impl.domain;

import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.impl.utils.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public class IdeVersionImpl extends IdeVersion {

  private static final String BUILD_NUMBER = "__BUILD_NUMBER__";
  private static final String STAR = "*";
  private static final String SNAPSHOT = "SNAPSHOT";
  private static final String FALLBACK_VERSION = "999.SNAPSHOT";
  private static final int SNAPSHOT_VALUE = Integer.MAX_VALUE;

  private final String myProductCode;
  private final int[] myComponents;
  private final boolean myIsSnapshot;

  private IdeVersionImpl(@NotNull String productCode, int baselineVersion, int buildNumber) {
    this(productCode, false, baselineVersion, buildNumber);
  }

  private IdeVersionImpl(@NotNull String productCode, boolean isSnapshot, int... components) {
    myProductCode = productCode;
    myComponents = components;
    myIsSnapshot = isSnapshot;
  }

  @NotNull
  public static IdeVersionImpl fromString(@NotNull String version) throws IllegalArgumentException {
    if (StringUtil.isEmptyOrSpaces(version)) {
      throw new IllegalArgumentException("Ide-version string must not be empty");
    }

    if (BUILD_NUMBER.equals(version) || SNAPSHOT.equals(version)) {
      IdeVersionImpl fallback = IdeVersionImpl.fromString(FALLBACK_VERSION);
      return new IdeVersionImpl("", true, fallback.myComponents);
    }

    String code = version;
    int productSeparator = code.lastIndexOf('-'); //some products have multiple parts, e.g. "FB-IC-143.157"
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

    if (baselineVersionSeparator > 0) {
      String baselineVersionString = code.substring(0, baselineVersionSeparator);
      if (baselineVersionString.trim().isEmpty()) {
        throw new IllegalArgumentException("Invalid version number: " + version);
      }

      List<String> components = StringUtil.split(code, ".");
      List<Integer> intComponentsList = new ArrayList<Integer>();

      boolean isSnapshot = false;
      for (String component : components) {
        int comp = parseBuildNumber(version, component);
        intComponentsList.add(comp);
        if (comp == SNAPSHOT_VALUE) {
          if (component.equals(SNAPSHOT)) isSnapshot = true;
          break;
        }
      }

      int[] intComponents = new int[intComponentsList.size()];
      for (int i = 0; i < intComponentsList.size(); i++) {
        intComponents[i] = intComponentsList.get(i);
      }

      return new IdeVersionImpl(productCode, isSnapshot, intComponents);

    } else {
      buildNumber = parseBuildNumber(version, code);

      if (buildNumber <= 2000) {
        // it's probably a baseline, not a build number
        return new IdeVersionImpl(productCode, buildNumber, 0);
      }

      baselineVersion = getBaseLineForHistoricBuilds(buildNumber);
      return new IdeVersionImpl(productCode, baselineVersion, buildNumber);
    }

  }

  private static int parseBuildNumber(String version, String code) {
    if (SNAPSHOT.equals(code) || STAR.equals(code) || BUILD_NUMBER.equals(code)) {
      return SNAPSHOT_VALUE;
    }
    try {
      return Integer.parseInt(code);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid version number: " + version);
    }
  }

  // See http://www.jetbrains.net/confluence/display/IDEADEV/Build+Number+Ranges for historic build ranges
  private static int getBaseLineForHistoricBuilds(int bn) {
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
  public int compareTo(@NotNull IdeVersion o) {
    if (!(o instanceof IdeVersionImpl)) {
      if (getBaselineVersion() != o.getBaselineVersion()) {
        return getBaselineVersion() - o.getBaselineVersion();
      }
      if (getBuild() != o.getBuild()) {
        return getBuild() - o.getBuild();
      }
      if (isSnapshot() != o.isSnapshot()) {
        return isSnapshot() ? 1 : -1;
      }
      return 0;
    }

    int[] c1 = myComponents;
    int[] c2 = ((IdeVersionImpl) o).myComponents;

    for (int i = 0; i < Math.min(c1.length, c2.length); i++) {
      if (c1[i] == c2[i] && c1[i] == SNAPSHOT_VALUE) return 0;
      if (c1[i] == SNAPSHOT_VALUE) return 1;
      if (c2[i] == SNAPSHOT_VALUE) return -1;

      int result = c1[i] - c2[i];
      if (result != 0) return result;
    }
    return c1.length - c2.length;
  }

  @Override
  public String asString(boolean includeProductCode, boolean includeSnapshotMarker) {
    StringBuilder builder = new StringBuilder();

    if (includeProductCode && !StringUtil.isEmpty(myProductCode)) {
      builder.append(myProductCode).append('-');
    }

    builder.append(myComponents[0]);
    for (int i = 1; i < myComponents.length; i++) {
      if (myComponents[i] != SNAPSHOT_VALUE) {
        builder.append('.').append(myComponents[i]);
      } else if (includeSnapshotMarker) {
        builder.append('.').append(myIsSnapshot ? SNAPSHOT : STAR);
      }
    }

    return builder.toString();
  }

  @Override
  public int[] getComponents() {
    return myComponents.clone();
  }

  @Override
  public int getBaselineVersion() {
    return myComponents[0];
  }

  @Override
  @NotNull
  public String getProductCode() {
    return StringUtil.notNullize(myProductCode);
  }

  @Override
  public String toString() {
    return asString();
  }

  @Override
  public int getBuild() {
    return myComponents[1];
  }

  @Override
  public boolean isSnapshot() {
    return myIsSnapshot;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    IdeVersionImpl that = (IdeVersionImpl) o;

    return myProductCode.equals(that.myProductCode) &&
            myIsSnapshot == that.myIsSnapshot &&
            Arrays.equals(myComponents, that.myComponents);

  }

  @Override
  public int hashCode() {
    int result = myProductCode.hashCode();
    result = 31 * result + Arrays.hashCode(myComponents);
    result = 31 * result + (myIsSnapshot ? 1 : 0);
    return result;
  }
}
