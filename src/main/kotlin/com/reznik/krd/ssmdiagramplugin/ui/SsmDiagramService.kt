package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel

@Service(Service.Level.PROJECT)
/**
 * Передаёт построенную модель StateMachine между action и Tool Window в рамках проекта.
 */
class SsmDiagramService {

    private var model: StateMachineModel? = null
    private val listeners = mutableListOf<(StateMachineModel) -> Unit>()

    /**
     * Сохраняет модель и уведомляет подписчиков о новой диаграмме.
     */
    fun show(model: StateMachineModel) {
        this.model = model
        listeners.forEach {
            it(model)
        }
    }

    /**
     * Возвращает последнюю отображённую модель.
     */
    fun currentModel(): StateMachineModel? = model

    /**
     * Регистрирует слушателя изменений текущей модели.
     */
    fun addListener(listener: (StateMachineModel) -> Unit) {
        listeners += listener
        model?.let(listener)
    }

    companion object {
        /** Возвращает сервис диаграммы для указанного проекта. */
        fun getInstance(project: Project): SsmDiagramService {
            return project.getService(SsmDiagramService::class.java)
        }
    }
}