package com.jetbrains.plugin.structure.intellij.version;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public abstract class IdeVersion implements Comparable<IdeVersion>, Serializable {

  /**
   * Tries to parse specified string as IDE version and throws an {@link IllegalArgumentException}
   * if the string is not a valid IDE version.
   *
   * @param version a string presentation of a version to be parsed
   * @return an instance of {@link IdeVersion}
   * @throws IllegalArgumentException if specified {@code version} doesn't represent correct {@code IdeVersion}
   * @see #createIdeVersionIfValid a version of the method that returns null instead of exception
   */
  @NotNull
  public static IdeVersion createIdeVersion(@NotNull String version) throws IllegalArgumentException {
    return IdeVersionImpl.Companion.fromString(version);
  }

  /**
   * Tries to parse specified string as IDE version and returns null if not succeed.
   *
   * @param version a string presentation of a version to be parsed
   * @return instance of {@link IdeVersion} for specified string, or null
   * if the string is not a valid IDE version
   */
  @Nullable
  public static IdeVersion createIdeVersionIfValid(@NotNull String version) {
    try {
      return IdeVersionImpl.Companion.fromString(version);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  /**
   * Determines whether the specified string is a valid {@link IdeVersion}.
   *
   * @param version string which must be validated
   * @return true if the specified string is a valid {@link IdeVersion}, false otherwise
   */
  public static boolean isValidIdeVersion(@NotNull String version) {
    return createIdeVersionIfValid(version) != null;
  }

  /**
   * Returns a string presentation of {@code this} version.
   * For the details of the presentation form refer to
   * <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html">
   * <i>IntelliJ Build Number Ranges</i></a>.
   * <p>
   * Examples are:
   * <ul>
   * <li>IU-143.1532.7</li>
   * <li>IU-143.1532.SNAPSHOT</li>
   * <li>143.1532</li>
   * <li>7341 (for historical builds)</li>
   * <li>IU-7341</li>
   * <li>IU-162.94.11.256.42</li>
   * </ul>
   *
   * @param includeProductCode    whether to append product code
   * @param includeSnapshotMarker whether to append <i>SNAPSHOT</i> marker (if present)
   * @return string presentation of the ide version
   */
  @NotNull
  public abstract String asString(boolean includeProductCode, boolean includeSnapshotMarker);

  /**
   * @return Returns a presentation with the product code and <i>SNAPSHOT</i> marker (if present)
   */
  @NotNull
  public String asString() {
    return asString(true, true);
  }

  public String asStringWithoutProductCode() {
    return asString(false, true);
  }

  public String asStringWithoutProductCodeAndSnapshot() {
    return asString(false, false);
  }

  @NotNull
  public abstract String getProductCode();

  public abstract int getBaselineVersion();

  public abstract int getBuild();

  public abstract boolean isSnapshot();

  public abstract int[] getComponents();

  @Override
  final public int hashCode() {
    return Arrays.hashCode(getComponents()) + Objects.hash(getProductCode(), isSnapshot());
  }

  @Override
  final public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj instanceof IdeVersion) {
      IdeVersion other = (IdeVersion) obj;
      return getProductCode().equals(other.getProductCode())
          && isSnapshot() == other.isSnapshot()
          && Arrays.equals(getComponents(), other.getComponents());
    }
    return false;
  }

  @Override
  final public int compareTo(@NotNull IdeVersion other) {
    String thisCode = getProductCode();
    String otherCode = other.getProductCode();

    int compareProductCodes = thisCode.compareTo(otherCode);
    if (!thisCode.isEmpty() && !otherCode.isEmpty() && compareProductCodes != 0) {
      return compareProductCodes;
    }

    int[] c1 = getComponents();
    int[] c2 = other.getComponents();

    for (int i = 0; i < Math.min(c1.length, c2.length); i++) {
      if (c1[i] == c2[i] && c1[i] == Integer.MAX_VALUE) {
        return 0;
      }
      if (c1[i] == Integer.MAX_VALUE) {
        return 1;
      }
      if (c2[i] == Integer.MAX_VALUE) {
        return -1;
      }
      int result = Integer.compare(c1[i], c2[i]);
      if (result != 0) {
        return result;
      }
    }
    return Integer.compare(c1.length, c2.length);
  }

}
