package featureExtractor.moduleType;

import com.intellij.openapi.module.ModuleType;

public class BaseModuleType extends ModuleType {
  public BaseModuleType() {
    this("BASE_MODULE_ID");
  }

  public BaseModuleType(String id) {
    super(id);
  }
}
