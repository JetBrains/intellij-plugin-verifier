package featureExtractor.moduleType;

import com.intellij.openapi.module.ModuleType;

public class DelegatedFromOtherConstructorWithExtractArgs extends ModuleType {

  public DelegatedFromOtherConstructorWithExtractArgs(String distractingParameter) {
    this("MODULE_ID", distractingParameter);
  }

  protected DelegatedFromOtherConstructorWithExtractArgs(String id, String distractingParameter) {
    super(id);
  }
}
