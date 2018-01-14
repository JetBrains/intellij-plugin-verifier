package featureExtractor.moduleType;

import com.intellij.openapi.module.ModuleType;

public class StaticConstant extends ModuleType {

  private static final String MODULE_ID = "MODULE_ID";

  protected StaticConstant() {
    super(MODULE_ID);
  }
}
