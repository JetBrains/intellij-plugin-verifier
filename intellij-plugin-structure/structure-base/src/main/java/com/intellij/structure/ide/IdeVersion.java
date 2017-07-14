package com.intellij.structure.ide;

import com.intellij.structure.impl.domain.IdeVersionImpl;
import com.jetbrains.structure.product.ProductVersion;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeVersion implements ProductVersion<IdeVersion> {

  /**
   * Tries to parse specified text as an Ide-version
   *
   * @param version a string presentation of version to be parsed
   * @return an instance of IdeVersion
   * @throws IllegalArgumentException if specified {@code version} doesn't represent correct {@code IdeVersion}
   */
  @NotNull
  public static IdeVersion createIdeVersion(@NotNull String version) throws IllegalArgumentException {
    return IdeVersionImpl.Companion.fromString(version);
  }

  public static boolean isValidIdeVersion(@NotNull String version) {
    try {
      IdeVersionImpl.Companion.fromString(version);
      return true;
    } catch (Exception e) {
      return false;
    }
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
  public abstract String asString(boolean includeProductCode, boolean includeSnapshotMarker);

  /**
   * @return Returns a presentation with the product code and <i>SNAPSHOT</i> marker (if present)
   */
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

}
