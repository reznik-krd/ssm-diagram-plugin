package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.InheritanceUtil
import com.reznik.krd.ssmdiagramplugin.model.SsmConfigInfo

/**
 * Detects EnumStateMachineConfigurerAdapter configurations and resolves state/event types.
 */
class SsmConfigDetector {

    companion object {
        private const val CONFIGURER_CLASS =
            "org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter"
    }

    /** Returns configuration metadata when the Java file contains a supported SSM config. */
    fun detect(file: PsiJavaFile): SsmConfigInfo? {
        for (psiClass in file.classes) {
            val result = detect(psiClass)

            if (result != null) {
                return result
            }
        }

        return null
    }

    private fun detect(psiClass: PsiClass): SsmConfigInfo? {
        if (!InheritanceUtil.isInheritor(psiClass, CONFIGURER_CLASS)) {
            return null
        }

        val superType = findConfigurerType(psiClass)
            ?: return SsmConfigInfo(
                configClass = psiClass,
                stateClass = null,
                eventClass = null
            )

        val parameters = superType.parameters

        val stateClass = (parameters.getOrNull(0) as? PsiClassType)?.resolve()
        val eventClass = (parameters.getOrNull(1) as? PsiClassType)?.resolve()

        return SsmConfigInfo(
            configClass = psiClass,
            stateClass = stateClass,
            eventClass = eventClass
        )
    }

    private fun findConfigurerType(psiClass: PsiClass): PsiClassType? {
        return psiClass.superTypes
            .filterIsInstance<PsiClassType>()
            .firstOrNull {
                it.resolve()?.qualifiedName == CONFIGURER_CLASS
            }
    }
}