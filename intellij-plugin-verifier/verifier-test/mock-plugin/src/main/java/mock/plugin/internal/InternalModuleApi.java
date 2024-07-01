package mock.plugin.internal;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class InternalModuleApi {
    @ApiStatus.Internal
    private String internalField;

    @ApiStatus.Internal
    public void noOp() {
        // NO-OP
    }

    public String getInternalField() {
        return internalField;
    }

    public void setInternalField(String internalField) {
        this.internalField = internalField;
    }
}
