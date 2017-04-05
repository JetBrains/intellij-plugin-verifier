package com.intellij.psi.search;

import com.intellij.psi.PsiElement;

/**
 * @author Sergey Patrikeev
 */
public abstract class UseScopeEnlarger {

  public abstract SearchScope getAdditionalUseScope(PsiElement psiElement);

}
