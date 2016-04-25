package com.jetbrains.pluginverifier.verifiers;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.impl.utils.StringUtil;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.utils.Pair;
import com.jetbrains.pluginverifier.utils.Util;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PluginVerifierOptions {
  public static final String METHOD_DESCRIPTOR_REGEX = "[\\w$/]+#[^#]+\\([^#]*\\)[^#]+";
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;
  private final String[] externalClassPrefixes;
  private final Set<String> myOptionalDependenciesIdsToIgnoreIfMissing;
  /**
   * Map of pluginDescriptor -> [problem] where pluginDescriptor := pluginXmlId and pluginVersion
   */
  private final Multimap<Pair<String, String>, Problem> myProblemsToIgnore;

  private PluginVerifierOptions(String[] prefixesToSkipForDuplicateClassesCheck,
                                String[] externalClassPrefixes,
                                String[] optionalDependenciesIdsToIgnoreIfMissing,
                                Multimap<Pair<String, String>, Problem> problemsToIgnore) {
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
    this.externalClassPrefixes = externalClassPrefixes;
    myOptionalDependenciesIdsToIgnoreIfMissing = new HashSet<String>(Arrays.asList(optionalDependenciesIdsToIgnoreIfMissing));
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
    List<String> res = new ArrayList<String>();

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
    List<String> res = new ArrayList<String>();
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

    Multimap<Pair<String, String>, Problem> problemsToIgnore = HashMultimap.create();

    String ignoreProblemsFile = getOption(commandLine, "ip");
    if (ignoreProblemsFile != null) {
      problemsToIgnore = getProblemsToIgnoreFromFile(ignoreProblemsFile);
    }

    return new PluginVerifierOptions(prefixesToSkipForDuplicateClassesCheck, externalClasses, optionalDependenciesIdsToIgnoreIfMissing, problemsToIgnore);
  }

  @NotNull
  private static Multimap<Pair<String, String>, Problem> getProblemsToIgnoreFromFile(@NotNull String ignoreProblemsFile) {
    File file = new File(ignoreProblemsFile);
    if (file.exists()) {
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(file));
        final Multimap<Pair<String, String>, Problem> m = HashMultimap.create();

        String s;
        while ((s = br.readLine()) != null) {
          s = s.trim();
          if (s.isEmpty() || s.startsWith("//")) continue; //it is a comment

          String[] tokens = s.split(":");

          if (tokens.length != 4) {
            throw new IllegalArgumentException("incorrect problem line " + s +
                "\nthe line must be in the form: <plugin_xml_id>:<plugin_version>:<problem_type>:<problem_description>" +
                "\n<plugin_version> may be empty" +
                "\nexample 'org.jetbrains.kotlin::unknown_class:org/jetbrains/kotlin/compiler/plugin/CliOption'");
          }

          String pluginId = tokens[0];
          String pluginVersion = tokens[1];
          String problemType = tokens[2];
          String problemDescription = tokens[3];

          switch (problemType) {
            case "unknown_class":
              m.put(Pair.create(pluginId, pluginVersion), new ClassNotFoundProblem(problemDescription));
              break;
            case "unknown_method":
              if (!problemDescription.matches(METHOD_DESCRIPTOR_REGEX)) {
                throw new IllegalArgumentException("Incorrect ignoring method descriptor " + problemDescription);
              }
              m.put(Pair.create(pluginId, pluginVersion), new MethodNotFoundProblem(problemDescription));
              break;
            case "not_implemented_method":
              if (!problemDescription.matches(METHOD_DESCRIPTOR_REGEX)) {
                throw new IllegalArgumentException("Incorrect ignoring method descriptor " + problemDescription);
              }
              m.put(Pair.create(pluginId, pluginVersion), new MethodNotImplementedProblem(problemDescription));
              break;
            case "incompatible_class_to_interface_change":
              m.put(Pair.create(pluginId, pluginVersion), new IncompatibleClassChangeProblem(problemDescription, IncompatibleClassChangeProblem.Change.CLASS_TO_INTERFACE));
              break;
            case "incomptaible_interface_to_class_change":
              m.put(Pair.create(pluginId, pluginVersion), new IncompatibleClassChangeProblem(problemDescription, IncompatibleClassChangeProblem.Change.INTERFACE_TO_CLASS));
              break;
            default:
              throw new IllegalArgumentException("Unsupported ignore problem type " + problemType);
          }
        }

        return m;
      } catch (IOException ignored) {
      } finally {
        IOUtils.closeQuietly(br);
      }
    }
    return HashMultimap.create();
  }

  public boolean isIgnoredProblem(@NotNull Plugin plugin, @NotNull Problem problem) {
    String xmlId = plugin.getPluginId();
    String version = plugin.getPluginVersion();
    for (Map.Entry<Pair<String, String>, Problem> entry : myProblemsToIgnore.entries()) {
      String ignoreXmlId = entry.getKey().getFirst();
      String ignoreVersion = entry.getKey().getSecond();
      Problem ignoreProblem = entry.getValue();

      if (StringUtil.equal(xmlId, ignoreXmlId)) {
        if (StringUtil.isEmpty(ignoreVersion) || StringUtil.equal(version, ignoreVersion)) {
          if (problem.equals(ignoreProblem)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public boolean isIgnoreMissingOptionalDependency(@NotNull String pluginId) {
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
