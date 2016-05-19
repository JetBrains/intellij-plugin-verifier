package com.jetbrains.pluginverifier.verifiers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.impl.utils.StringUtil;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.utils.Pair;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.regex.Pattern;

public class PluginVerifierOptions {
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;
  private final String[] externalClassPrefixes;
  private final Set<String> myOptionalDependenciesIdsToIgnoreIfMissing;
  /**
   * Map of pluginDescriptor -> [problem] where pluginDescriptor := pluginXmlId and pluginVersion
   */
  private final Multimap<Pair<String, String>, Pattern> myProblemsToIgnore;

  private PluginVerifierOptions(String[] prefixesToSkipForDuplicateClassesCheck,
                                String[] externalClassPrefixes,
                                String[] optionalDependenciesIdsToIgnoreIfMissing,
                                Multimap<Pair<String, String>, Pattern> problemsToIgnore) {
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
    this.externalClassPrefixes = externalClassPrefixes;
    myOptionalDependenciesIdsToIgnoreIfMissing = new HashSet<>(Arrays.asList(optionalDependenciesIdsToIgnoreIfMissing));
    myProblemsToIgnore = problemsToIgnore;
  }

  @Nullable
  private static String getOption(CommandLine commandLine, String shortKey) {
    Option option = Util.CMD_OPTIONS.getOption(shortKey);

    String cmdValue = commandLine.getOptionValue(shortKey);
    if (cmdValue != null) return cmdValue;

    return RepositoryConfiguration.getInstance().getProperty(option.getLongOpt());
  }

  @NotNull
  private static List<String> getOptionValues(CommandLine commandLine, String shortKey) {
    List<String> res = new ArrayList<>();

    String[] cmdValues = commandLine.getOptionValues(shortKey);
    if (cmdValues != null) {
      Collections.addAll(res, cmdValues);
    }

    Option option = Util.CMD_OPTIONS.getOption(shortKey);
    String cfgProperty = RepositoryConfiguration.getInstance().getProperty(option.getLongOpt());

    if (cfgProperty != null) {
      res.add(cfgProperty);
    }

    return res;
  }

  @NotNull
  private static String[] getOptionValuesSplit(CommandLine commandLine, String splitter, String shortKey) {
    List<String> res = new ArrayList<>();
    for (String optionStr : getOptionValues(commandLine, shortKey)) {
      if (optionStr.isEmpty()) continue;

      Collections.addAll(res, optionStr.split(splitter));
    }

    return res.toArray(new String[res.size()]);
  }

  @NotNull
  public static PluginVerifierOptions parseOpts(@NotNull CommandLine commandLine) {
    String[] prefixesToSkipForDuplicateClassesCheck = getOptionValuesSplit(commandLine, ":", "s");
    for (int i = 0; i < prefixesToSkipForDuplicateClassesCheck.length; i++) {
      prefixesToSkipForDuplicateClassesCheck[i] = prefixesToSkipForDuplicateClassesCheck[i].replace('.', '/');
    }

    String[] externalClasses = getOptionValuesSplit(commandLine, ":", "e");
    for (int i = 0; i < externalClasses.length; i++) {
      externalClasses[i] = externalClasses[i].replace('.', '/');
    }
    String[] optionalDependenciesIdsToIgnoreIfMissing = getOptionValuesSplit(commandLine, ",", "imod");

    Multimap<Pair<String, String>, Pattern> problemsToIgnore = HashMultimap.create();

    String ignoreProblemsFile = getOption(commandLine, "ip");
    if (ignoreProblemsFile != null) {
      problemsToIgnore = getProblemsToIgnoreFromFile(ignoreProblemsFile);
    }

    return new PluginVerifierOptions(prefixesToSkipForDuplicateClassesCheck, externalClasses, optionalDependenciesIdsToIgnoreIfMissing, problemsToIgnore);
  }

  @NotNull
  private static Multimap<Pair<String, String>, Pattern> getProblemsToIgnoreFromFile(@NotNull String ignoreProblemsFile) {
    File file = new File(ignoreProblemsFile);
    if (!file.exists()) {
      throw new IllegalArgumentException("Ignored problems file doesn't exist " + ignoreProblemsFile);
    }

    Multimap<Pair<String, String>, Pattern> m = HashMultimap.create();
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String s;
      while ((s = br.readLine()) != null) {
        s = s.trim();
        if (s.isEmpty() || s.startsWith("//")) continue; //it is a comment

        String[] tokens = s.split(":");

        if (tokens.length != 3) {
          throw new IllegalArgumentException("incorrect problem line " + s +
              "\nthe line must be in the form: <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>" +
              "\n<plugin_version> may be empty (which means that a problem will be ignored in all the versions of the plugin)" +
              "\nexample 'org.jetbrains.kotlin::accessing to unknown class org/jetbrains/kotlin/compiler/.*' - ignore all the missing classes from org.jetbrains.kotlin.compiler package");
        }

        String pluginId = tokens[0].trim();
        String pluginVersion = tokens[1].trim();
        String ignorePattern = tokens[2].trim().replace('/', '.');

        m.put(Pair.create(pluginId, pluginVersion), Pattern.compile(ignorePattern));
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to parse ignored problems file " + ignoreProblemsFile, e);
    }
    return m;
  }

  boolean isIgnoredProblem(@NotNull Plugin plugin, @NotNull Problem problem) {
    String xmlId = plugin.getPluginId();
    String version = plugin.getPluginVersion();
    for (Map.Entry<Pair<String, String>, Pattern> entry : myProblemsToIgnore.entries()) {
      String ignoreXmlId = entry.getKey().getFirst();
      String ignoreVersion = entry.getKey().getSecond();
      Pattern ignoredPattern = entry.getValue();

      if (StringUtil.equal(xmlId, ignoreXmlId)) {
        if (StringUtil.isEmpty(ignoreVersion) || StringUtil.equal(version, ignoreVersion)) {
          if (ignoredPattern.matcher(problem.getDescription().replace('/', '.')).matches()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  boolean isIgnoreMissingOptionalDependency(@NotNull String pluginId) {
    return myOptionalDependenciesIdsToIgnoreIfMissing.contains(pluginId);
  }

  @NotNull
  public String[] getPrefixesToSkipForDuplicateClassesCheck() {
    return myPrefixesToSkipForDuplicateClassesCheck;
  }

  @NotNull
  public String[] getExternalClassPrefixes() {
    return externalClassPrefixes;
  }

  public boolean isExternalClass(@NotNull String className) {
    for (String prefix : externalClassPrefixes) {
      if (prefix != null && prefix.length() > 0 && className.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }
}
