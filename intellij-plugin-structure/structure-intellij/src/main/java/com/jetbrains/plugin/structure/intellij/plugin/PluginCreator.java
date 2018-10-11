package com.jetbrains.plugin.structure.intellij.plugin;

import com.jetbrains.plugin.structure.base.plugin.*;
import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem;
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified;
import com.jetbrains.plugin.structure.base.problems.UnableToReadDescriptor;
import com.jetbrains.plugin.structure.intellij.beans.IdeaVersionBean;
import com.jetbrains.plugin.structure.intellij.beans.PluginBean;
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean;
import com.jetbrains.plugin.structure.intellij.beans.PluginVendorBean;
import com.jetbrains.plugin.structure.intellij.extractor.PluginBeanExtractor;
import com.jetbrains.plugin.structure.intellij.problems.*;
import com.jetbrains.plugin.structure.intellij.utils.StringUtil;
import com.jetbrains.plugin.structure.intellij.utils.xincludes.JDOMXIncluder;
import com.jetbrains.plugin.structure.intellij.utils.xincludes.XIncludePathResolver;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

final class PluginCreator {

  private static final Logger LOG = LoggerFactory.getLogger(PluginCreator.class);

  private static final int MAX_VERSION_LENGTH = 64;
  private static final int MAX_PROPERTY_LENGTH = 255;
  private static final int MAX_LONG_PROPERTY_LENGTH = 65535;

  private final IdePluginImpl myPlugin;
  private final List<PluginProblem> myProblems = new ArrayList<PluginProblem>();
  private final String myDescriptorPath;
  private final boolean myValidateDescriptor;
  private final File myActualFile;

  PluginCreator(@NotNull String descriptorPath,
                boolean validateDescriptor,
                @NotNull Document document,
                @NotNull URL documentUrl,
                @NotNull XIncludePathResolver pathResolver,
                @NotNull File actualFile,
                @NotNull List<PluginIcon> icons) {
    myDescriptorPath = descriptorPath;
    myValidateDescriptor = validateDescriptor;
    myActualFile = actualFile;
    myPlugin = resolveDocumentAndValidateBean(document, documentUrl, pathResolver);
    if (myPlugin != null) {
      myPlugin.setIcons(icons);
    }
  }

  PluginCreator(String descriptorPath, PluginProblem singleProblem, File actualFile) {
    if (singleProblem.getLevel() != PluginProblem.Level.ERROR) {
      throw new IllegalArgumentException("Only severe problems allowed here");
    }
    myActualFile = actualFile;
    myDescriptorPath = descriptorPath;
    myValidateDescriptor = true;
    myProblems.add(singleProblem);
    myPlugin = null;
  }

  private void validatePluginBean(PluginBean bean) {
    if (myValidateDescriptor) {
      validateAttributes(bean);
      validateId(bean.id);
      validateName(bean.name);
      validateVersion(bean.pluginVersion);
      validateDescription(bean.description);
      validateChangeNotes(bean.changeNotes);
      validateVendor(bean.vendor);
      validateIdeaVersion(bean.ideaVersion);

      if (bean.dependencies != null) {
        for (PluginDependencyBean dependencyBean : bean.dependencies) {
          if (StringUtil.isEmpty(dependencyBean.pluginId)) {
            registerProblem(new InvalidDependencyBean(myDescriptorPath));
          }
        }
      }

      if (bean.modules != null) {
        for (String module : bean.modules) {
          if (StringUtil.isEmpty(module)) {
            registerProblem(new InvalidModuleBean(myDescriptorPath));
          }
        }
      }
    }
  }

  private void validateAttributes(@NotNull PluginBean bean) {
    if (bean.url != null) {
      validatePropertyLength("plugin url", bean.url, MAX_PROPERTY_LENGTH);
    }
  }

  private void validateVersion(String pluginVersion) {
    if (StringUtil.isEmpty(pluginVersion)) {
      registerProblem(new PropertyNotSpecified("version", myDescriptorPath));
    } else {
      validatePropertyLength("version", pluginVersion, MAX_VERSION_LENGTH);
    }
  }

  private void validatePlugin(IdePlugin plugin) {
    int moduleDependenciesCnt = 0;
    for (PluginDependency dependency : plugin.getDependencies()) {
      if (dependency.isModule()) {
        moduleDependenciesCnt++;
      }
    }
    if (moduleDependenciesCnt == 0) {
      registerProblem(new NoModuleDependencies(myDescriptorPath));
    }

    IdeVersion sinceBuild = plugin.getSinceBuild();
    IdeVersion untilBuild = plugin.getUntilBuild();
    if (sinceBuild != null && untilBuild != null && sinceBuild.compareTo(untilBuild) > 0) {
      registerProblem(new SinceBuildGreaterThanUntilBuild(myDescriptorPath, sinceBuild, untilBuild));
    }
  }

  public Map<PluginDependency, String> getOptionalDependenciesConfigurationFiles() {
    return myPlugin.getOptionalDependenciesConfigFiles();
  }

  public void addOptionalDescriptor(PluginDependency pluginDependency, String configurationFile, PluginCreator optionalCreator) {
    PluginCreationResult<IdePlugin> pluginCreationResult = optionalCreator.getPluginCreationResult();
    if (pluginCreationResult instanceof PluginCreationSuccess) {
      myPlugin.addOptionalDescriptor(configurationFile, ((PluginCreationSuccess<IdePlugin>) pluginCreationResult).getPlugin());
    } else {
      registerProblem(new MissingOptionalDependencyConfigurationFile(configurationFile, pluginDependency.getId()));
    }
  }

  @Nullable
  private IdePluginImpl resolveDocumentAndValidateBean(@NotNull Document originalDocument,
                                                       @NotNull URL documentUrl,
                                                       @NotNull XIncludePathResolver pathResolver) {
    Document document = resolveXIncludesOfDocument(originalDocument, documentUrl, pathResolver);
    if (document == null) {
      return null;
    }
    PluginBean bean = readDocumentIntoXmlBean(document);
    if (bean == null) {
      return null;
    }
    validatePluginBean(bean);
    if (hasErrors()) {
      return null;
    }
    IdePluginImpl plugin = new IdePluginImpl(document, bean);
    validatePlugin(plugin);
    if (hasErrors()) {
      return null;
    }
    return plugin;
  }

  @Nullable
  private Document resolveXIncludesOfDocument(@NotNull Document originalDocument, @NotNull URL documentUrl, @NotNull XIncludePathResolver pathResolver) {
    try {
      return JDOMXIncluder.resolve(originalDocument, documentUrl.toExternalForm(), false, pathResolver);
    } catch (Exception e) {
      LOG.info("Unable to resolve x-include elements of descriptor " + myDescriptorPath + " of " + myActualFile, e);
      registerProblem(new UnresolvedXIncludeElements(myDescriptorPath));
      return null;
    }
  }

  @Nullable
  private PluginBean readDocumentIntoXmlBean(@NotNull Document document) {
    try {
      return PluginBeanExtractor.INSTANCE.extractPluginBean(document);
    } catch (Exception e) {
      registerProblem(new UnableToReadDescriptor(myDescriptorPath));
      LOG.info("Unable to read plugin descriptor " + myDescriptorPath + " of " + myActualFile, e);
      return null;
    }
  }

  public File getActualFile() {
    return myActualFile;
  }

  public void registerProblem(@NotNull PluginProblem problem) {
    myProblems.add(problem);
  }

  public boolean isSuccess() {
    return !hasErrors();
  }

  public boolean hasOnlyInvalidDescriptorErrors() {
    for (PluginProblem problem : getProblems()) {
      if (problem.getLevel() == PluginProblem.Level.ERROR && !(problem instanceof InvalidDescriptorProblem)) {
        return false;
      }
    }
    return true;
  }

  public boolean hasErrors() {
    for (PluginProblem problem : getProblems()) {
      if (problem.getLevel() == PluginProblem.Level.ERROR) return true;
    }
    return false;
  }

  @NotNull
  public PluginCreationResult<IdePlugin> getPluginCreationResult() {
    if (hasErrors()) {
      return new PluginCreationFail<IdePlugin>(myProblems);
    }
    return new PluginCreationSuccess<IdePlugin>(myPlugin, myProblems);
  }

  @NotNull
  public List<PluginProblem> getProblems() {
    return myProblems;
  }

  public void setOriginalFileAndExtractDir(@NotNull File originalFile, @NotNull File extractedDir) {
    if (myPlugin != null) {
      myPlugin.setOriginalPluginFile(originalFile);
      myPlugin.setExtractDirectory(extractedDir);
    }
  }

  private void validateId(@Nullable String id) {
    if (id != null) {
      if ("com.your.company.unique.plugin.id".equals(id)) {
        registerProblem(new PropertyWithDefaultValue(myDescriptorPath, "id"));
      } else {
        validatePropertyLength("id", id, MAX_PROPERTY_LENGTH);
      }
    }
  }

  private void validateName(@Nullable String name) {
    if (StringUtil.isEmpty(name)) {
      registerProblem(new PropertyNotSpecified("name", myDescriptorPath));
    } else if ("Plugin display name here".equals(name)) {
      registerProblem(new PropertyWithDefaultValue(myDescriptorPath, "name"));
    } else if ("plugin".contains(name)) {
      registerProblem(new PluginWordInPluginName(myDescriptorPath));
    } else {
      validatePropertyLength("name", name, MAX_PROPERTY_LENGTH);
    }
  }

  private void validateDescription(@Nullable String htmlDescription) {
    if (StringUtil.isEmpty(htmlDescription)) {
      registerProblem(new PropertyNotSpecified("description", myDescriptorPath));
      return;
    }
    validatePropertyLength("description", htmlDescription, MAX_LONG_PROPERTY_LENGTH);

    String textDescription = Jsoup.parseBodyFragment(htmlDescription).text();

    if (textDescription.length() < 40) {
      registerProblem(new ShortDescription());
      return;
    }

    if (textDescription.contains("Enter short description for your plugin here.") ||
        textDescription.contains("most HTML tags may be used")) {
      registerProblem(new DefaultDescription(myDescriptorPath));
      return;
    }

    int latinSymbols = StringUtil.numberOfPatternMatches(textDescription, Pattern.compile("[A-Za-z]|\\s"));
    if (latinSymbols < 40) {
      registerProblem(new NonLatinDescription());
    }
  }

  private void validateChangeNotes(@Nullable String changeNotes) {
    if (StringUtil.isEmptyOrSpaces(changeNotes)) {
      //Too many plugins don't specify the change-notes, so it's too strict to require them.
      //But if specified, let's check that the change-notes are long enough.
      return;
    }

    if (changeNotes.length() < 40) {
      registerProblem(new ShortChangeNotes(myDescriptorPath));
      return;
    }

    if (changeNotes.contains("Add change notes here") ||
        changeNotes.contains("most HTML tags may be used")) {
      registerProblem(new DefaultChangeNotes(myDescriptorPath));
    }

    validatePropertyLength("<change-notes>", changeNotes, MAX_LONG_PROPERTY_LENGTH);
  }

  private void validatePropertyLength(@NotNull String propertyName, @NotNull String propertyValue, int maxLength) {
    if (propertyValue.length() > maxLength) {
      registerProblem(new TooLongPropertyValue(myDescriptorPath, propertyName, propertyValue.length(), maxLength));
    }
  }

  private void validateVendor(PluginVendorBean vendorBean) {
    if (vendorBean == null) {
      registerProblem(new PropertyNotSpecified("vendor", myDescriptorPath));
      return;
    }

    if (StringUtil.isEmptyOrSpaces(vendorBean.url) &&
        StringUtil.isEmptyOrSpaces(vendorBean.email) &&
        StringUtil.isEmptyOrSpaces(vendorBean.name)) {
      registerProblem(new PropertyNotSpecified("vendor", myDescriptorPath));
      return;
    }

    if ("YourCompany".equals(vendorBean.name)) {
      registerProblem(new PropertyWithDefaultValue(myDescriptorPath, "vendor"));
    }
    validatePropertyLength("vendor", vendorBean.name, MAX_PROPERTY_LENGTH);

    if ("http://www.yourcompany.com".equals(vendorBean.url)) {
      registerProblem(new PropertyWithDefaultValue(myDescriptorPath, "vendor url"));
    }
    validatePropertyLength("vendor url", vendorBean.url, MAX_PROPERTY_LENGTH);

    if ("support@yourcompany.com".equals(vendorBean.email)) {
      registerProblem(new PropertyWithDefaultValue(myDescriptorPath, "vendor email"));
    }
    validatePropertyLength("vendor email", vendorBean.email, MAX_PROPERTY_LENGTH);
  }

  private void validateSinceBuild(@Nullable String sinceBuild) {
    if (sinceBuild == null) {
      registerProblem(new SinceBuildNotSpecified(myDescriptorPath));
    } else {
      IdeVersion sinceBuildParsed = IdeVersion.createIdeVersionIfValid(sinceBuild);
      if (sinceBuildParsed == null) {
        registerProblem(new InvalidSinceBuild(myDescriptorPath, sinceBuild));
      } else {
        if (sinceBuildParsed.getBaselineVersion() < 130 && sinceBuild.endsWith(".*")) {
          registerProblem(new InvalidSinceBuild(myDescriptorPath, sinceBuild));
        }
        if (sinceBuildParsed.getBaselineVersion() > 2000) {
          registerProblem(new ErroneousSinceBuild(myDescriptorPath, sinceBuildParsed));
        }
      }
    }
  }

  private void validateUntilBuild(@NotNull String untilBuild) {
    IdeVersion untilBuildParsed = IdeVersion.createIdeVersionIfValid(untilBuild);
    if (untilBuildParsed == null) {
      registerProblem(new InvalidUntilBuild(myDescriptorPath, untilBuild));
    } else {
      if (untilBuildParsed.getBaselineVersion() > 2000) {
        registerProblem(new ErroneousUntilBuild(myDescriptorPath, untilBuildParsed));
      }
    }
  }

  private void validateIdeaVersion(IdeaVersionBean versionBean) {
    if (versionBean == null) {
      registerProblem(new PropertyNotSpecified("idea-version", myDescriptorPath));
      return;
    }

    String sinceBuild = versionBean.sinceBuild;
    validateSinceBuild(sinceBuild);

    String untilBuild = versionBean.untilBuild;
    if (untilBuild != null) {
      validateUntilBuild(untilBuild);
    }
  }

}
