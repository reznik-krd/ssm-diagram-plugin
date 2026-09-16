package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

/**
 * Finds Spring StateMachine configuration files available in the current project.
 *
 * Must be called from a read action while the project is in smart mode.
 */
class SsmConfigFinder {

    /**
     * Finds all supported StateMachine Java configuration files in the project.
     */
    fun findAll(
        project: Project
    ): List<PsiJavaFile> {

        val projectScope =
            GlobalSearchScope.projectScope(project)

        val configurerClass =
            JavaPsiFacade
                .getInstance(project)
                .findClass(
                    CONFIGURER_CLASS,
                    GlobalSearchScope.allScope(project)
                )
                ?: return emptyList()

        return ClassInheritorsSearch
            .search(
                configurerClass,
                projectScope,
                true
            )
            .findAll()
            .asSequence()
            .mapNotNull { psiClass ->
                psiClass.containingFile as? PsiJavaFile
            }
            .distinctBy { psiFile ->
                psiFile.virtualFile?.path
            }
            .sortedBy { psiFile ->
                psiFile.classes
                    .firstOrNull()
                    ?.name
                    ?: psiFile.name
            }
            .toList()
    }

    private companion object {

        const val CONFIGURER_CLASS =
            "org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter"
    }
}