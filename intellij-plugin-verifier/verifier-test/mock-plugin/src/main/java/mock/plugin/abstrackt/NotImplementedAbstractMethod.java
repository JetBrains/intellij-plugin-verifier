package mock.plugin.abstrackt;

import com.intellij.psi.search.UseScopeEnlarger;

/*expected(PROBLEM)
Abstract method com.intellij.psi.search.UseScopeEnlarger.getAdditionalUseScope(PsiElement arg0) : SearchScope is not implemented

Concrete class mock.plugin.abstrackt.NotImplementedAbstractMethod inherits from com.intellij.psi.search.UseScopeEnlarger but doesn't implement the abstract method getAdditionalUseScope(PsiElement arg0) : SearchScope. This can lead to **AbstractMethodError** exception at runtime.
*/
public class NotImplementedAbstractMethod extends UseScopeEnlarger {
}
