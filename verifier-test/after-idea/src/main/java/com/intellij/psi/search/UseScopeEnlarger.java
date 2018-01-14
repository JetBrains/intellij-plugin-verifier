package com.intellij.psi.search;

import com.intellij.psi.PsiElement;

public abstract class UseScopeEnlarger {

  public abstract SearchScope getAdditionalUseScope(PsiElement psiElement);

}
