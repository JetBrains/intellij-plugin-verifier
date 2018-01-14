package featureExtractor.configurationType;

import com.intellij.execution.configurations.ConfigurationType;

public class ConstantConfigurationType implements ConfigurationType {

  public static final String CONSTANT = "runConfiguration";

  @Override
  public String getId() {
    return CONSTANT;
  }
}
