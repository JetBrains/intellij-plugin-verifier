package com.intellij.structure.impl.domain;

import com.google.common.base.Strings;
import com.intellij.structure.impl.beans.PluginBean;
import com.intellij.structure.impl.beans.PluginBeanExtractor;
import com.intellij.structure.impl.beans.PluginDependencyBean;
import com.intellij.structure.impl.resolvers.PluginResolver;
import com.intellij.structure.impl.utils.xml.JDOMXIncluder;
import com.intellij.structure.plugin.PluginCreationResult;
import com.intellij.structure.plugin.PluginCreationSuccess;
import com.intellij.structure.plugin.PluginDependency;
import com.intellij.structure.problems.*;
import com.intellij.structure.resolvers.Resolver;
import org.jdom2.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.structure.impl.utils.StringUtil.isEmpty;

/**
 * @author Sergey Patrikeev
 */
final class PluginCreator {

  private static final Logger LOG = LoggerFactory.getLogger(PluginCreator.class);
  private final PluginImpl myPlugin;
  private final List<PluginProblem> myProblems = new ArrayList<PluginProblem>();
  private final String myDescriptorPath;
  private final boolean myValidateDescriptor;
  private Resolver myResolver;

  PluginCreator(String descriptorPath,
                boolean validateDescriptor,
                Document document,
                URL documentUrl,
                JDOMXIncluder.PathResolver pathResolver) {
    myDescriptorPath = descriptorPath;
    myValidateDescriptor = validateDescriptor;
    myPlugin = resolveDocumentAndValidateBean(document, documentUrl, pathResolver);
  }

  PluginCreator(String descriptorPath, PluginProblem singleProblem) {
    if (singleProblem.getLevel() != PluginProblem.Level.ERROR) {
      throw new IllegalArgumentException("Only severe problems allowed here");
    }
    myDescriptorPath = descriptorPath;
    myValidateDescriptor = true;
    myProblems.add(singleProblem);
    myPlugin = null;
  }

  private void validateDescriptor(PluginBean bean) {
    if (myValidateDescriptor) {
      if (Strings.isNullOrEmpty(bean.name)) {
        registerProblem(new PluginNameIsNotSpecified(myDescriptorPath));
      }

      if (bean.pluginVersion == null) {
        registerProblem(new VersionIsNotSpecified(myDescriptorPath));
      }

      if (bean.vendor == null) {
        registerProblem(new VendorIsNotSpecified(myDescriptorPath));
      }

      if (bean.ideaVersion == null) {
        registerProblem(new IdeaVersionIsNotSpecified(myDescriptorPath));
      }

      if (isEmpty(bean.description)) {
        registerProblem(new EmptyDescription(myDescriptorPath));
      }

      for (PluginDependencyBean dependencyBean : bean.dependencies) {
        if (isEmpty(dependencyBean.pluginId)) {
          registerProblem(new InvalidDependencyBean(myDescriptorPath));
        }
      }

      for (String module : bean.modules) {
        if (isEmpty(module)) {
          registerProblem(new InvalidModuleBean(myDescriptorPath));
        }
      }
    }
  }

  public Map<PluginDependency, String> getOptionalDependenciesConfigurationFiles() {
    return myPlugin.getOptionalDependenciesConfigFiles();
  }

  public void addOptionalDescriptor(PluginDependency pluginDependency, String configurationFile, PluginCreator optionalCreator) {
    PluginCreationResult pluginCreationResult = optionalCreator.getPluginCreationResult();
    if (pluginCreationResult instanceof PluginCreationSuccess) {
      myPlugin.addOptionalDescriptor(configurationFile, ((PluginCreationSuccess) pluginCreationResult).getPlugin());
    } else {
      registerProblem(new MissingOptionalDependency(pluginDependency, configurationFile));
    }
  }

  @Nullable
  private PluginImpl resolveDocumentAndValidateBean(@NotNull Document originalDocument,
                                                    @NotNull URL documentUrl,
                                                    @NotNull JDOMXIncluder.PathResolver pathResolver) {
    try {
      Document document = PluginXmlExtractor.resolveXIncludes(originalDocument, documentUrl, pathResolver);
      PluginBean bean = PluginBeanExtractor.extractPluginBean(document, new ReportingValidationEventHandler());
      validateDescriptor(bean);
      return new PluginImpl(document, bean);
    } catch (Exception e) {
      LOG.error("Unable to read plugin descriptor " + myDescriptorPath, e);
      registerProblem(new UnableToReadDescriptor(myDescriptorPath));
      return null;
    }
  }

  public void registerProblem(@NotNull PluginProblem problem) {
    myProblems.add(problem);
  }

  public boolean isSuccess() {
    return !hasErrors();
  }

  public boolean hasErrors() {
    for (PluginProblem problem : getProblems()) {
      if (problem.getLevel() == PluginProblem.Level.ERROR) return true;
    }
    return false;
  }

  @NotNull
  public PluginCreationResult getPluginCreationResult() {
    if (hasErrors()) {
      return new PluginCreationFailImpl(myProblems);
    }
    return new PluginCreationSuccessImpl(myPlugin, myProblems, myResolver);
  }

  @NotNull
  public List<PluginProblem> getProblems() {
    return myProblems;
  }

  public void setOriginalFile(File originalFile) {
    if (myPlugin != null) {
      myPlugin.setOriginalPluginFile(originalFile);
    }
  }

  public void readClassFiles(@NotNull File jarOrDirectory, boolean deleteOnResolverClose) {
    if (myPlugin != null) {
      try {
        myResolver = new PluginResolver(jarOrDirectory, deleteOnResolverClose);
      } catch (Exception e) {
        LOG.warn("Unable to read plugin class files " + jarOrDirectory, e);
        registerProblem(new UnableToReadPluginClassFiles(jarOrDirectory));
      }
    }
  }

  private static class ReportingValidationEventHandler implements ValidationEventHandler {
    @Override
    public boolean handleEvent(ValidationEvent event) {
      return true;
      //todo: fix
      /*String reason = event.getMessage();
      if (reason.contains("unexpected element")) return true;
      registerProblem(new UnableToParseDescriptor(descriptorPath, reason));
      return true;*/
    }
  }
}
