package com.intellij.structure.impl.utils.validators;

import com.google.common.base.Strings;
import com.intellij.structure.domain.PluginProblem;
import com.intellij.structure.impl.beans.PluginBean;
import com.intellij.structure.impl.beans.PluginDependencyBean;
import com.intellij.structure.impl.utils.BiAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.structure.impl.utils.StringUtil.isEmpty;

/**
 * @author Sergey Patrikeev
 */
public abstract class Validator {
  protected List<PluginProblem> myProblems = new ArrayList<PluginProblem>();

  public void validateBean(@Nullable PluginBean bean, @NotNull String fileName) {
    if(bean == null){
      return;
    }

    if (Strings.isNullOrEmpty(bean.name)) {
      onMissingConfigElement("Invalid " + fileName + ": 'name' is not specified");
    }

    if (bean.pluginVersion == null) {
      onMissingConfigElement("Invalid " + fileName + ": version is not specified");
    }

    if (bean.vendor == null) {
      onMissingConfigElement("Invalid " + fileName + ": element 'vendor' is not found");
    }

    if (bean.ideaVersion == null) {
      onMissingConfigElement("Invalid " + fileName + ": element 'idea-version' not found");
    }

    if (isEmpty(bean.description)) {
      onMissingConfigElement("Invalid file: description is empty");
    }

    for (PluginDependencyBean dependencyBean : bean.dependencies) {
      if (dependencyBean.pluginId == null) {
        onIncorrectStructure("Invalid plugin.xml: invalid dependency tag " + dependencyBean);
      }
    }

    for (String module : bean.modules) {
      if (module == null) {
        onIncorrectStructure("Invalid <module> tag: value is not specified");
      }
    }
  }

  private void performAction(@NotNull Event event, @NotNull String message, @Nullable Throwable cause) {
    BiAction<String, Throwable> action = supplyAction(event);
    if (action != null) {
      action.call(message, cause);
    }
  }

  public void onMissingConfigElement(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_CONFIG_ELEMENT, message, null);
  }

  public void onMissingFile(@NotNull String message) throws RuntimeException {
    performAction(Event.MISSING_FILE, message, null);
  }

  public void onMissingDependency(@NotNull String message) throws RuntimeException{
    performAction(Event.MISSING_DEPENDENCY, message, null);
  }

  public void onIncorrectStructure(@NotNull String message) throws RuntimeException {
    performAction(Event.INCORRECT_STRUCTURE, message, null);
  }

  public void onMultipleConfigFiles(@NotNull String message) throws RuntimeException {
    performAction(Event.MULTIPLE_CONFIG_FILE, message, null);
  }

  public void onCheckedException(@NotNull String message, @NotNull Exception cause) throws RuntimeException {
    performAction(Event.CHECKED_EXCEPTION, message, cause);
  }

  public boolean hasErrors() {
    for(PluginProblem problem : getProblems()) {
      if(problem.getLevel() == PluginProblem.Level.ERROR) return true;
    }
    return false;
  }

  @Nullable
  protected abstract BiAction<String, Throwable> supplyAction(@NotNull Event event);

  private Validator createIgnoringValidator(@NotNull final Event ignoredEvent) {
    return new Validator() {
      @Nullable
      @Override
      protected BiAction<String, Throwable> supplyAction(@NotNull Event event) {
        if (event == ignoredEvent) {
          return null;
        }
        return Validator.this.supplyAction(event);
      }
    };
  }

  public Validator ignoreMissingFile() {
    return createIgnoringValidator(Event.MISSING_FILE);
  }

  public Validator ignoreMissingConfigElement() {
    return createIgnoringValidator(Event.MISSING_CONFIG_ELEMENT);
  }

  @NotNull
  public List<PluginProblem> getProblems() {
    return myProblems;
  };


  enum Event {
    MISSING_FILE,
    INCORRECT_STRUCTURE,
    CHECKED_EXCEPTION,
    MISSING_CONFIG_ELEMENT,
    MISSING_DEPENDENCY,
    MULTIPLE_CONFIG_FILE
  }

}
