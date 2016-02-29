package com.intellij.structure.domain;

import com.intellij.structure.impl.domain.IdeVersionImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public abstract class IdeVersion implements Comparable<IdeVersion> {

  /**
   * Tries to parse specified text as an Ide-version
   *
   * @param version a string presentation of version to be parsed
   * @return an instance of IdeVersion
   * @throws IllegalArgumentException if specified {@code version} doesn't represent correct {@code IdeVersion}
   */
  @NotNull
  public static IdeVersion createIdeVersion(@NotNull String version) throws IllegalArgumentException {
    return new IdeVersionImpl(version);
  }

  /**
   * Returns a string presentation of this version in the form:<p>
   * <b> {@literal [<product_code>-]<branch #>.<build #>[.<SNAPSHOT>|.<attempt #>]}</b> <p>
   * If the version represents a baseline (an integer number, e.g 9567) this number is returned<p>
   * Examples are:
   *   <ul>
   *     <li>IU-143.1532.7</li>
   *     <li>IU-143.1532.SNAPSHOT</li>
   *     <li>143.1532</li>
   *     <li>7341</li>
   *   </ul>
   *
   * @see
   * <blockquote>
   *    <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html">
   *    <i>IntelliJ Build Number Ranges</i></a>
   * </blockquote>
   * @return presentation
   */
  public abstract String getFullPresentation();

  @NotNull
  public abstract String getProductCode();

  @NotNull
  public abstract String getProductName();

  public abstract int getBranch();

  public abstract int getBuild();

  public abstract int getAttempt();

  public abstract boolean isSnapshot();

  @Override
  public int compareTo(@NotNull IdeVersion v2) {
    if (getBranch() > v2.getBranch()) return 1;
    if (getBranch() < v2.getBranch()) return -1;
    if (getBuild() > v2.getBuild()) return 1;
    if (getBuild() < v2.getBuild()) return -1;
    if (getAttempt() > v2.getAttempt()) return 1;
    if (getAttempt() < v2.getAttempt()) return -1;
    return 0;
  }
}
