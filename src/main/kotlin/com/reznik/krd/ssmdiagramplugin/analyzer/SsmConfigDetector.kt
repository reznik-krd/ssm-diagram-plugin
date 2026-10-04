package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.util.InheritanceUtil
import com.reznik.krd.ssmdiagramplugin.model.SsmConfigInfo

/**
 * Определяет, является ли Java-класс поддерживаемой конфигурацией Spring StateMachine.
 */
class SsmConfigDetector {

    companion object {
        private const val CONFIGURER_CLASS = "org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter"
    }

    /**
     * Проверяет Java-файл или класс и возвращает данные поддерживаемой StateMachine-конфигурации.
     */
    fun detect(file: PsiJavaFile): SsmConfigInfo? {
        for (psiClass in file.classes) {
            val result = detect(psiClass)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Проверяет Java-файл или класс и возвращает данные поддерживаемой StateMachine-конфигурации.
     */
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

    /**
     * Находит тип EnumStateMachineConfigurerAdapter в иерархии класса.
     */
    private fun findConfigurerType(psiClass: PsiClass): PsiClassType? {
        return psiClass.superTypes
            .filterIsInstance<PsiClassType>()
            .firstOrNull {
                it.resolve()?.qualifiedName == CONFIGURER_CLASS
            }
    }
}