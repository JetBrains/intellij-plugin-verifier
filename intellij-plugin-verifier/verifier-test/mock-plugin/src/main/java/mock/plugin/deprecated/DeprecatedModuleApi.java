package mock.plugin.deprecated;

@Deprecated
public class DeprecatedModuleApi {
    @Deprecated
    private String deprecatedField;

    @Deprecated
    public void noOp() {
        // NO-OP
    }

    public String getDeprecatedField() {
        return deprecatedField;
    }

    public void setDeprecatedField(String deprecatedField) {
        this.deprecatedField = deprecatedField;
    }
}
