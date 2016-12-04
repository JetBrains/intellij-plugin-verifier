package featureExtractor;

import com.intellij.execution.configurations.ConfigurationType;

/**
 * @author Sergey Patrikeev
 */
public class ConstantConfigurationType implements ConfigurationType {

  public static final String CONSTANT = "runConfiguration";

  @Override
  public String getId() {
    return CONSTANT;
  }
}
