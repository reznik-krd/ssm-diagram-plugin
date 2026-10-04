package com.reznik.krd.ssmdiagramplugin.service

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiJavaFile
import com.intellij.util.concurrency.AppExecutorUtil
import com.reznik.krd.ssmdiagramplugin.analyzer.SsmConfigFinder
import com.reznik.krd.ssmdiagramplugin.analyzer.SsmPsiAnalyzer
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel

/**
 * Выполняет поиск и PSI-анализ StateMachine в фоновых read actions IntelliJ IDEA.
 */
class SsmBackgroundTaskService(private val project: Project) {

    private val configFinder = SsmConfigFinder()
    private val analyzer = SsmPsiAnalyzer()

    /**
     * Асинхронно ищет конфигурации StateMachine в проекте.
     */
    fun findConfigurations(onSuccess: (List<PsiJavaFile>) -> Unit, onError: (Throwable) -> Unit) {
        ReadAction
            .nonBlocking<List<PsiJavaFile>> {
                configFinder.findAll(project)
            }
            .inSmartMode(project)
            .expireWith(project)
            .finishOnUiThread(ModalityState.any()) { configs ->
                onSuccess(configs)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
            .onError { error ->
                onError(error)
            }
    }

    /**
     * Анализирует переданный PSI-элемент и возвращает соответствующую модель.
     */
    fun analyze(
        psiFile: PsiJavaFile,
        onSuccess: (StateMachineModel?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        ReadAction
            .nonBlocking<StateMachineModel?> {
                if (!psiFile.isValid) {
                    return@nonBlocking null
                }
                analyzer.analyze(psiFile)
            }
            .inSmartMode(project)
            .expireWith(project)
            .expireWhen { !psiFile.isValid }
            .finishOnUiThread(ModalityState.any()) { model ->
                onSuccess(model)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
            .onError { error ->
                onError(error)
            }
    }
}