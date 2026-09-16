package com.reznik.krd.ssmdiagramplugin.analyzer

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch

class SsmConfigFinder {

    companion object {

        private const val CONFIGURER_CLASS =
            "org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter"
    }

    fun findAll(
        project: Project
    ): List<PsiJavaFile> {

        /*
         * Во время индексации IDEA поиск наследников
         * выполнять нельзя.
         *
         * Ждём smart mode.
         */
        DumbService
            .getInstance(project)
            .waitForSmartMode()

        return ReadAction.compute<List<PsiJavaFile>, RuntimeException> {

            val scope =
                GlobalSearchScope.projectScope(project)

            /*
             * Сам Spring-класс ищем во всём scope,
             * включая зависимости проекта.
             */
            val configurerClass =
                JavaPsiFacade
                    .getInstance(project)
                    .findClass(
                        CONFIGURER_CLASS,
                        GlobalSearchScope.allScope(project)
                    )
                    ?: return@compute emptyList()

            /*
             * А его наследников ищем только
             * внутри текущего проекта.
             */
            ClassInheritorsSearch
                .search(
                    configurerClass,
                    scope,
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
    }
}