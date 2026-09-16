package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel

@Service(Service.Level.PROJECT)
class SsmDiagramService {

    private var model: StateMachineModel? = null

    private val listeners = mutableListOf<(StateMachineModel) -> Unit>()

    fun show(model: StateMachineModel) {
        this.model = model

        listeners.forEach {
            it(model)
        }
    }

    fun currentModel(): StateMachineModel? = model

    fun addListener(
        listener: (StateMachineModel) -> Unit
    ) {
        listeners += listener

        model?.let(listener)
    }

    companion object {

        fun getInstance(project: Project): SsmDiagramService {
            return project.getService(SsmDiagramService::class.java)
        }
    }
}