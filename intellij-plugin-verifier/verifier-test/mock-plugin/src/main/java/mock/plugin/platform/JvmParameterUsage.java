package mock.plugin.platform;

import com.intellij.lang.jvm.JvmParameter;

/*expected(EXPERIMENTAL)
Experimental API interface com.intellij.lang.jvm.JvmParameter reference

Experimental API interface com.intellij.lang.jvm.JvmParameter is referenced in mock.plugin.platform.JvmParameterUsage$1. This interface can be changed in a future release leading to incompatibilities
*/
/*expected(EXPERIMENTAL)
Experimental API interface com.intellij.lang.jvm.JvmParameter reference

Experimental API interface com.intellij.lang.jvm.JvmParameter is referenced in mock.plugin.platform.JvmParameterUsage.<init>(). This interface can be changed in a future release leading to incompatibilities
*/
public class JvmParameterUsage {

    public JvmParameterUsage() {
        JvmParameter param = new JvmParameter() {};
        //noinspection ResultOfMethodCallIgnored
        param.toString();
    }
}
