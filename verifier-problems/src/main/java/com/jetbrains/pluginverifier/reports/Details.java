package com.jetbrains.pluginverifier.reports;

import com.google.common.collect.Multimap;
import com.intellij.structure.domain.IdeVersion;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

/**
 * <p>
 * The class contains the result of verification of the {@link #checkedPlugin()} against {@link #checkedIde()}.
 * <p>
 * The existing problems of the plugin against this IDE may be obtained via {@link #problems()}.
 * <p>
 * The verification {@link #overview()} represents a meta-information (minor plugin problems and tips for the plugin
 * developer).
 *
 * @author Sergey Patrikeev
 */
public interface Details {

  @NotNull
  IdeVersion checkedIde();

  @NotNull
  UpdateInfo checkedPlugin();

  @NotNull
  Multimap<Problem, ProblemLocation> problems();

  @NotNull
  String overview();

}
