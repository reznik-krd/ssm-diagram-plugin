package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.reznik.krd.ssmdiagramplugin.analyzer.SsmConfigFinder
import com.reznik.krd.ssmdiagramplugin.analyzer.SsmPsiAnalyzer
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.ui.diagram.SsmDiagramPanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities

class SsmDiagramToolWindowPanel(
    private val project: Project
) : JPanel(BorderLayout()) {

    private val diagramService =
        SsmDiagramService.getInstance(project)

    private val analyzer =
        SsmPsiAnalyzer()

    private val configFinder =
        SsmConfigFinder()

    private val diagramPanel =
        SsmDiagramPanel(project)

    private val scrollPane =
        JBScrollPane(diagramPanel)

    private val configComboBox =
        ComboBox<PsiJavaFile>()

    private val generateButton =
        JButton("Generate")

    private val refreshButton =
        JButton("Refresh")

    private val clearButton =
        JButton("Clear")

    private val zoomOutButton =
        JButton("−")

    private val zoomButton =
        JButton("100%")

    private val zoomInButton =
        JButton("+")

    private val fitButton =
        JButton("Fit")

    private val centerButton =
        JButton("Center")

    /*
     * Конфигурация, схема которой сейчас реально
     * отображается на экране.
     *
     * Она может отличаться от выбранной в ComboBox,
     * пока пользователь не нажал Generate.
     */
    private var displayedConfigPath: String? = null

    init {

        border =
            JBUI.Borders.empty()

        configComboBox.renderer =
            SimpleListCellRenderer.create("") { file ->

                file.classes
                    .firstOrNull()
                    ?.name
                    ?: file.name
            }

        add(
            createTopPanel(),
            BorderLayout.NORTH
        )

        add(
            scrollPane,
            BorderLayout.CENTER
        )

        generateButton.addActionListener {
            generateSelectedConfig()
        }

        refreshButton.addActionListener {
            refreshDiagram()
        }

        clearButton.addActionListener {
            clearDiagram()
        }

        zoomOutButton.addActionListener {
            diagramPanel.zoomOut()
        }

        zoomInButton.addActionListener {
            diagramPanel.zoomIn()
        }

        zoomButton.addActionListener {
            diagramPanel.resetZoom()

            SwingUtilities.invokeLater {
                centerDiagram()
            }
        }

        fitButton.addActionListener {
            fitDiagram()
        }

        centerButton.addActionListener {
            centerDiagram()
        }

        /*
         * Процент в toolbar синхронизирован и с кнопками,
         * и с существующим Cmd/Ctrl + wheel zoom.
         */
        diagramPanel.setZoomChangedListener { zoom ->

            zoomButton.text =
                "${(zoom * 100).toInt()}%"
        }

        /*
         * Единый pipeline и для Generate из ToolWindow,
         * и для Generate через ПКМ по конфигу.
         */
        diagramService.addListener { model ->

            diagramPanel.setModel(model)

            val configFile =
                getConfigFile(model)

            displayedConfigPath =
                configFile
                    ?.virtualFile
                    ?.path

            selectConfigForModel(model)
            updateButtons()
        }

        loadConfigs()
        updateButtons()
    }

    // -------------------------------------------------------------------------
    // UI
    // -------------------------------------------------------------------------

    private fun createTopPanel(): JPanel {

        val panel =
            JPanel()

        panel.layout =
            BoxLayout(
                panel,
                BoxLayout.Y_AXIS
            )

        panel.add(
            createConfigurationPanel()
        )

        panel.add(
            createViewPanel()
        )

        return panel
    }

    private fun createConfigurationPanel(): JPanel {

        val panel =
            JPanel(
                FlowLayout(
                    FlowLayout.LEFT,
                    8,
                    6
                )
            )

        panel.border =
            JBUI.Borders.empty(
                4,
                6,
                0,
                6
            )

        panel.add(
            JBLabel("Configuration:")
        )

        configComboBox.preferredSize =
            Dimension(
                230,
                configComboBox.preferredSize.height
            )

        panel.add(configComboBox)
        panel.add(generateButton)
        panel.add(refreshButton)
        panel.add(clearButton)

        return panel
    }

    private fun createViewPanel(): JPanel {

        val panel =
            JPanel(
                FlowLayout(
                    FlowLayout.LEFT,
                    8,
                    6
                )
            )

        panel.border =
            JBUI.Borders.empty(
                0,
                6,
                4,
                6
            )

        panel.add(
            JBLabel("View:")
        )

        zoomButton.preferredSize =
            Dimension(
                70,
                zoomButton.preferredSize.height
            )

        panel.add(zoomOutButton)
        panel.add(zoomButton)
        panel.add(zoomInButton)
        panel.add(fitButton)
        panel.add(centerButton)

        return panel
    }

    // -------------------------------------------------------------------------
    // Config discovery
    // -------------------------------------------------------------------------

    private fun loadConfigs() {

        val selectedFile =
            configComboBox.selectedItem
                    as? PsiJavaFile

        val selectedPath =
            selectedFile
                ?.virtualFile
                ?.path

        val configs =
            configFinder.findAll(project)

        configComboBox.removeAllItems()

        configs.forEach { config ->
            configComboBox.addItem(config)
        }

        if (selectedPath != null) {

            for (
            index in
            0 until configComboBox.itemCount
            ) {

                val item =
                    configComboBox
                        .getItemAt(index)

                if (
                    item.virtualFile?.path ==
                    selectedPath
                ) {

                    configComboBox.selectedIndex =
                        index

                    break
                }
            }
        }

        updateButtons()
    }

    // -------------------------------------------------------------------------
    // Generate
    // -------------------------------------------------------------------------

    private fun generateSelectedConfig() {

        val psiFile =
            configComboBox.selectedItem
                    as? PsiJavaFile
                ?: return

        generateConfig(psiFile)
    }

    private fun generateConfig(
        psiFile: PsiJavaFile
    ) {

        val model =
            analyzer.analyze(psiFile)

        if (model == null) {

            Messages.showWarningDialog(
                project,
                "Spring StateMachine configuration was not found.",
                "Spring StateMachine Diagram"
            )

            return
        }

        diagramService.show(model)
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    private fun refreshDiagram() {

        val path =
            displayedConfigPath
                ?: return

        val config =
            findConfigByPath(path)

        if (config != null) {
            generateConfig(config)
            return
        }

        /*
         * Если файл был добавлен/переиндексирован,
         * один раз обновляем список конфигов.
         */
        loadConfigs()

        val refreshedConfig =
            findConfigByPath(path)

        if (refreshedConfig != null) {
            generateConfig(refreshedConfig)
            return
        }

        clearDiagram()

        Messages.showWarningDialog(
            project,
            "The StateMachine configuration is no longer available.",
            "Spring StateMachine Diagram"
        )
    }

    // -------------------------------------------------------------------------
    // Clear
    // -------------------------------------------------------------------------

    private fun clearDiagram() {

        diagramPanel.clear()

        displayedConfigPath =
            null

        scrollPane.viewport.viewPosition =
            Point(0, 0)

        updateButtons()
    }

    // -------------------------------------------------------------------------
    // View
    // -------------------------------------------------------------------------

    private fun fitDiagram() {

        diagramPanel.fitToViewport(
            scrollPane.viewport.extentSize
        )

        SwingUtilities.invokeLater {
            centerDiagram()
        }
    }

    private fun centerDiagram() {

        val viewport =
            scrollPane.viewport

        val viewSize =
            diagramPanel.preferredSize

        val extentSize =
            viewport.extentSize

        val x =
            ((viewSize.width - extentSize.width) / 2)
                .coerceAtLeast(0)

        val y =
            ((viewSize.height - extentSize.height) / 2)
                .coerceAtLeast(0)

        viewport.viewPosition =
            Point(
                x,
                y
            )
    }

    // -------------------------------------------------------------------------
    // Button state
    // -------------------------------------------------------------------------

    private fun updateButtons() {

        val hasSelectedConfig =
            configComboBox.selectedItem != null

        val hasDiagram =
            displayedConfigPath != null

        configComboBox.isEnabled =
            configComboBox.itemCount > 0

        generateButton.isEnabled =
            hasSelectedConfig

        refreshButton.isEnabled =
            hasDiagram

        clearButton.isEnabled =
            hasDiagram

        zoomOutButton.isEnabled =
            hasDiagram

        zoomButton.isEnabled =
            hasDiagram

        zoomInButton.isEnabled =
            hasDiagram

        fitButton.isEnabled =
            hasDiagram

        centerButton.isEnabled =
            hasDiagram
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun findConfigByPath(
        path: String
    ): PsiJavaFile? {

        for (
        index in
        0 until configComboBox.itemCount
        ) {

            val item =
                configComboBox
                    .getItemAt(index)

            if (
                item.virtualFile?.path ==
                path
            ) {
                return item
            }
        }

        return null
    }

    private fun getConfigFile(
        model: StateMachineModel
    ): PsiJavaFile? {

        return model
            .config
            .configClass
            .containingFile
                as? PsiJavaFile
    }

    // -------------------------------------------------------------------------
    // Synchronization with context-menu generation
    // -------------------------------------------------------------------------

    private fun selectConfigForModel(
        model: StateMachineModel
    ) {

        val configFile =
            getConfigFile(model)
                ?: return

        val configPath =
            configFile
                .virtualFile
                ?.path
                ?: return

        for (
        index in
        0 until configComboBox.itemCount
        ) {

            val item =
                configComboBox
                    .getItemAt(index)

            if (
                item.virtualFile?.path ==
                configPath
            ) {

                configComboBox.selectedIndex =
                    index

                return
            }
        }

        /*
         * Например новый config появился уже после
         * создания ToolWindow.
         */
        loadConfigs()

        for (
        index in
        0 until configComboBox.itemCount
        ) {

            val item =
                configComboBox
                    .getItemAt(index)

            if (
                item.virtualFile?.path ==
                configPath
            ) {

                configComboBox.selectedIndex =
                    index

                return
            }
        }
    }
}
