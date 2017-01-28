package featureExtractor.configurationType;

import com.intellij.execution.configurations.ConfigurationTypeBase;

import javax.swing.*;

/**
 * @author Sergey Patrikeev
 */
public class ConfigurationTypeBaseImplementor extends ConfigurationTypeBase {
  protected ConfigurationTypeBaseImplementor(String id, String displayName, String description, Icon icon) {
    super("ConfigurationId", displayName, description, icon);
  }
}
