package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.*;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.misc.RepositoryConfiguration;
import kotlin.Pair;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

public class Util {

  public static final Options CMD_OPTIONS = new Options()
      .addOption("h", "help", false, "Show help")
      .addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)")
      .addOption("s", "skip-class-for-dup-check", true, "Class name prefixes to skip in duplicate classes check, delimited by ':'")
      .addOption("e", "external-classes", true, "Classes from external libraries. Error will not be reported if class not found. Delimited by ':'")
      .addOption("all", "check-all-plugins-with-ide", false, "Check IDE build with all compatible plugins")
      .addOption("p", "plugin-to-check", true, "A plugin id to check with IDE, plugin verifier will check ALL compatible plugin builds")
      .addOption("u", "update-to-check", true, "A plugin id to check with IDE, plugin verifier will check LAST plugin build only")
      .addOption("iv", "ide-version", true, "Version of IDE that will be tested, e.g. IU-133.439")
      .addOption("epf", "excluded-plugin-file", true, "File with list of excluded plugin builds.")
      .addOption("d", "dump-broken-plugin-list", true, "File to dump broken plugin list.")
      .addOption("report", "make-report", true, "Create a detailed report about broken plugins.")
      .addOption("xr", "save-results-xml", true, "Save results to xml file")
      .addOption("tc", "team-city-output", false, "Print TeamCity compatible output.")
      .addOption("pluginsFile", "plugins-to-check-file", true, "The file that contains list of plugins to check.")
      .addOption("cp", "external-class-path", true, "External class path")
      .addOption("printFile", true, ".xml report file to be printed in TeamCity")
      .addOption("repo", "results-repository", true, "Url of repository which contains check results")
      .addOption("pcr", "plugin-check-result", true, "File to dump result of checking plugin against IDEs")
      .addOption("g", "group", true, "Whether to group problems presentation (possible args are 'plugin' - group by plugin and 'type' - group by error-type)")
      .addOption("dce", "dont-check-excluded", false, "If specified no plugins from -epf will be checked at all")
      .addOption("imod", "ignore-missing-optional-dependencies", true, "Missing optional dependencies on the plugin IDs specified in this parameter will be ignored")
      .addOption("ip", "ignore-problems", true, "Problems specified in this file will be ignored. File must contain lines in form <plugin_xml_id>:<plugin_version>:<problem_description_regexp_pattern>");

  //TODO: write a System.option for appending this list.
  private static final ImmutableList<String> IDEA_ULTIMATE_MODULES = ImmutableList.of(
      "com.intellij.modules.platform",
      "com.intellij.modules.lang",
      "com.intellij.modules.vcs",
      "com.intellij.modules.xml",
      "com.intellij.modules.xdebugger",
      "com.intellij.modules.java",
      "com.intellij.modules.ultimate",
      "com.intellij.modules.all"
  );

  public static void printHelp() {
    new HelpFormatter().printHelp("java -jar verifier.jar <command> [<args>]", CMD_OPTIONS);
  }

  @NotNull
  public static String getStackTrace(@Nullable Throwable t) {
    if (t == null) return "";
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }


  public static <T> List<T> concat(Collection<T> first, Collection<T> second) {
    List<T> res = new ArrayList<>(first.size() + second.size());
    res.addAll(first);
    res.addAll(second);
    return res;
  }

  @NotNull
  public static Pair<List<String>, List<String>> extractPluginToCheckList(@NotNull CommandLine commandLine) {
    List<String> pluginsCheckAllBuilds = new ArrayList<>();
    List<String> pluginsCheckLastBuilds = new ArrayList<>();

    String[] pluginIdsCheckAllBuilds = commandLine.getOptionValues('p'); //plugin-to-check
    if (pluginIdsCheckAllBuilds != null) {
      pluginsCheckAllBuilds.addAll(Arrays.asList(pluginIdsCheckAllBuilds));
    }

    String[] pluginIdsCheckLastBuilds = commandLine.getOptionValues('u'); //update-to-check
    if (pluginIdsCheckLastBuilds != null) {
      pluginsCheckLastBuilds.addAll(Arrays.asList(pluginIdsCheckLastBuilds));
    }

    String pluginsFile = commandLine.getOptionValue("pluginsFile"); //plugins-to-check-file (usually checkedPlugins.txt)
    if (pluginsFile != null) {
      try {
        BufferedReader reader = new BufferedReader(new FileReader(pluginsFile));
        String s;
        while ((s = reader.readLine()) != null) {
          s = s.trim();
          if (s.isEmpty() || s.startsWith("//")) continue;

          boolean checkAllBuilds = true;
          if (s.endsWith("$")) {
            s = s.substring(0, s.length() - 1).trim();
            checkAllBuilds = false;
          }
          if (s.startsWith("$")) {
            s = s.substring(1).trim();
            checkAllBuilds = false;
          }

          if (checkAllBuilds) {
            pluginsCheckAllBuilds.add(s);
          } else {
            if (s.isEmpty()) continue;

            pluginsCheckLastBuilds.add(s);
          }
        }
      } catch (IOException e) {
        throw FailUtil.fail("Failed to read plugins file " + pluginsFile + ": " + e.getLocalizedMessage(), e);
      }
    }

    System.out.println("List of plugins to check: " + Joiner.on(", ").join(Iterables.concat(pluginsCheckAllBuilds, pluginsCheckLastBuilds)));

    return new Pair<>(pluginsCheckAllBuilds, pluginsCheckLastBuilds);
  }

  @NotNull
  public static Multimap<String, String> getExcludedPlugins(@NotNull CommandLine commandLine) throws IOException {
    String epf = commandLine.getOptionValue("epf"); //excluded-plugin-file (usually brokenPlugins.txt)
    if (epf == null) {
      //no predicate specified
      return ArrayListMultimap.create();
    }

    //file containing list of broken plugins (e.g. IDEA-*/lib/resources.jar!/brokenPlugins.txt)
    BufferedReader br = new BufferedReader(new FileReader(new File(epf)));
    try {
      final SetMultimap<String, String> m = HashMultimap.create();

      String s;
      while ((s = br.readLine()) != null) {
        s = s.trim();
        if (s.startsWith("//")) continue; //it is a comment

        List<String> tokens = ParametersListUtil.parse(s);
        if (tokens.isEmpty()) continue;

        if (tokens.size() == 1) {
          throw new IOException(epf + " is broken. The line contains plugin name, but does not contain version: " + s);
        }

        String pluginId = tokens.get(0);

        m.putAll(pluginId, tokens.subList(1, tokens.size())); //"plugin id" -> [all its builds]
      }

      return m;

    } finally {
      IOUtils.closeQuietly(br);
    }
  }

  public static void dumbBrokenPluginsList(@NotNull String dumpBrokenPluginsFile, @NotNull Collection<UpdateInfo> brokenUpdates)
      throws IOException {
    //pluginId -> [list of its builds in DESC order]
    Multimap<String, String> m = TreeMultimap.create(Ordering.natural(), Ordering.natural().reverse());

    for (UpdateInfo update : brokenUpdates) {
      m.put(update.getPluginId(), update.getVersion());
    }

    try (PrintWriter out = new PrintWriter(dumpBrokenPluginsFile)) {
      out.println("// This file contains list of broken plugins.\n" +
          "// Each line contains plugin ID and list of versions that are broken.\n" +
          "// If plugin name or version contains a space you can quote it like in command line.\n");

      for (Map.Entry<String, Collection<String>> entry : m.asMap().entrySet()) {

        out.print(ParametersListUtil.join(Collections.singletonList(entry.getKey())));
        out.print("    ");
        out.println(ParametersListUtil.join(new ArrayList<>(entry.getValue())));
      }
    }
  }

  public static boolean isDefaultModule(String moduleId) {
    return IDEA_ULTIMATE_MODULES.contains(moduleId);
  }

  public static boolean failOnCyclicDependency() {
    //TODO: change this with a method parameter
    return Boolean.parseBoolean(RepositoryConfiguration.getInstance().getProperty("fail.on.cyclic.dependencies"));
  }
}
