package com.reznik.krd.ssmdiagramplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiJavaFile
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import com.reznik.krd.ssmdiagramplugin.service.SsmBackgroundTaskService
import com.reznik.krd.ssmdiagramplugin.ui.diagram.SsmDiagramPanel
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import kotlin.math.roundToInt
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.reznik.krd.ssmdiagramplugin.ui.diagram.DiagramDisplayOptions
import javax.swing.JCheckBox

/**
 * Управляет выбором конфигурации, генерацией диаграммы и элементами Tool Window.
 */
class SsmDiagramToolWindowPanel(
    private val project: Project
) : JPanel(BorderLayout()) {

    private val diagramService = SsmDiagramService.getInstance(project)
    private val backgroundTaskService = SsmBackgroundTaskService(project)
    private val diagramPanel = SsmDiagramPanel(project)

    private val scrollPane = JBScrollPane(diagramPanel)
    private val configComboBox = ComboBox<PsiJavaFile>()
    private val generateButton = JButton("Generate")
    private val openConfigButton = JButton("Open")
    private val refreshButton = JButton("Refresh")
    private val clearButton = JButton("Clear")
    private val zoomOutButton = JButton("−")
    private val zoomButton = JButton("100%")
    private val zoomInButton = JButton("+")
    private val fitButton = JButton("Fit")
    private val centerButton = JButton("Center")
    private val hideGuardsCheckBox = JCheckBox("Hide Guards")
    private val hideActionsCheckBox = JCheckBox("Hide Actions")

    private val progressIcon = AsyncProcessIcon("SSM Diagram progress")
    private val statusLabel = JBLabel()
    private val statusPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))

    /**
     * Path of the configuration currently rendered.
     */
    private var displayedConfigPath: String? = null

    /**
     * Indicates whether a PSI/index operation is currently running.
     */
    private var busy = false

    /**
     * Sequence used to ignore results from outdated asynchronous operations.
     */
    private var operationSequence = 0L

    init {
        border = JBUI.Borders.empty()
        configureControls()
        add(createTopPanel(), BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        installActions()
        subscribeToDiagramService()
        loadConfigs()
        updateButtons()
    }

    /**
     * Настраивает внешний вид и размеры элементов управления.
     */
    private fun configureControls() {
        configComboBox.renderer =
            SimpleListCellRenderer.create("") { file ->
                file.virtualFile
                    ?.nameWithoutExtension
                    ?: file.name
            }

        configComboBox.preferredSize =
            Dimension(
                CONFIG_COMBO_WIDTH,
                configComboBox.preferredSize.height
            )

        zoomButton.preferredSize =
            Dimension(
                ZOOM_BUTTON_WIDTH,
                zoomButton.preferredSize.height
            )

        progressIcon.isVisible = false
        statusLabel.isVisible = false
        statusPanel.isVisible = false
    }

    /**
     * Назначает обработчики кнопкам и элементам управления.
     */
    private fun installActions() {
        generateButton.addActionListener { generateSelectedConfig() }
        openConfigButton.addActionListener { openSelectedConfig() }
        refreshButton.addActionListener { refreshDiagram() }
        clearButton.addActionListener { clearDiagram() }
        zoomOutButton.addActionListener { diagramPanel.zoomOut() }
        zoomInButton.addActionListener { diagramPanel.zoomIn() }
        zoomButton.addActionListener {
            diagramPanel.resetZoom()
            SwingUtilities.invokeLater(
                ::centerDiagram
            )
        }
        fitButton.addActionListener { fitDiagram() }
        centerButton.addActionListener { centerDiagram() }
        diagramPanel.setZoomChangedListener { zoom ->
            zoomButton.text = "${(zoom * 100).roundToInt()}%"
        }
        hideGuardsCheckBox.addActionListener { updateDisplayOptions() }
        hideActionsCheckBox.addActionListener { updateDisplayOptions() }
    }

    /**
     * Подписывает панель на модели, поступающие из сервиса диаграммы.
     */
    private fun subscribeToDiagramService() {
        diagramService.addListener { model ->
            diagramPanel.setModel(model)
            displayedConfigPath = getConfigFile(model)?.virtualFile?.path
            selectConfigForModel(model)
            updateButtons()
        }
    }

    /**
     * Создаёт верхнюю область Tool Window.
     */
    private fun createTopPanel(): JPanel =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(createConfigurationPanel())
            add(createViewPanel())
            add(createStatusPanel())
        }

    /**
     * Создаёт строку выбора и управления StateMachine-конфигурацией.
     */
    private fun createConfigurationPanel(): JPanel =
        createToolbarRow(label = "Configuration:", top = 4, bottom = 0).apply {
            add(configComboBox)
            add(generateButton)
            add(openConfigButton)
            add(refreshButton)
            add(clearButton)
        }

    /**
     * Создаёт строку управления масштабом и положением диаграммы.
     */
    private fun createViewPanel(): JPanel =
        createToolbarRow(label = "View:", top = 0, bottom = 0).apply {
            add(zoomOutButton)
            add(zoomButton)
            add(zoomInButton)
            add(fitButton)
            add(centerButton)
            add(hideGuardsCheckBox)
            add(hideActionsCheckBox)
        }

    /**
     * Создаёт строку прогресса фоновых операций.
     */
    private fun createStatusPanel(): JPanel {
        statusPanel.border = JBUI.Borders.empty(0, 6, 4, 6)
        statusPanel.add(progressIcon)
        statusPanel.add(statusLabel)
        return statusPanel
    }

    /**
     * Создаёт стандартную строку панели инструментов.
     */
    private fun createToolbarRow(label: String, top: Int, bottom: Int): JPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, 8, 6)).apply {
            border = JBUI.Borders.empty(top, 6, bottom, 6)
            add(JBLabel(label))
        }

    /**
     * Асинхронно загружает список доступных StateMachine-конфигураций.
     */
    private fun loadConfigs(onComplete: (() -> Unit)? = null) {
        val selectedPath = selectedConfig()?.virtualFile?.path
        val operation = beginOperation("Searching configurations...")

        backgroundTaskService.findConfigurations(
            onSuccess = { configs ->
                if (!isCurrentOperation(operation)) {
                    return@findConfigurations
                }
                configComboBox.removeAllItems()
                configs.forEach(
                    configComboBox::addItem
                )
                if (selectedPath != null) {
                    selectConfigByPath(selectedPath)
                }
                finishOperation(operation)
                onComplete?.invoke()
            },
            onError = { error ->
                if (!isCurrentOperation(operation)) {
                    return@findConfigurations
                }
                finishOperation(operation)
                showError("Failed to search StateMachine configurations.", error)
            }
        )
    }

    /**
     * Запускает генерацию диаграммы для выбранной конфигурации.
     */
    private fun generateSelectedConfig() {
        val config = selectedConfig() ?: return
        generateConfig(
            psiFile = config,
            status = "Generating diagram..."
        )
    }

    /**
     * Открывает выбранный класс StateMachine-конфигурации в редакторе.
     */
    private fun openSelectedConfig() {
        val config = selectedConfig() ?: return

        val configClass =
            ReadAction.compute<com.intellij.psi.PsiClass?, RuntimeException> {
                config.classes.firstOrNull()
            } ?: return

        val virtualFile = configClass.containingFile
            ?.virtualFile
            ?: return

        OpenFileDescriptor(
            project,
            virtualFile,
            configClass.textOffset
        ).navigate(true)
    }

    /**
     * Асинхронно анализирует конфигурацию и передаёт модель в сервис диаграммы.
     */
    private fun generateConfig(psiFile: PsiJavaFile, status: String) {
        val operation = beginOperation(status)

        backgroundTaskService.analyze(
            psiFile = psiFile,
            onSuccess = { model ->
                if (!isCurrentOperation(operation)) {
                    return@analyze
                }
                finishOperation(operation)
                if (model == null) {
                    Messages.showWarningDialog(
                        project,
                        "Spring StateMachine configuration was not found.",
                        DIALOG_TITLE
                    )
                    return@analyze
                }
                diagramService.show(model)
            },
            onError = { error ->
                if (!isCurrentOperation(operation)) {
                    return@analyze
                }
                finishOperation(operation)
                showError(
                    "Failed to generate StateMachine diagram.",
                    error
                )
            }
        )
    }

    /**
     * Повторно анализирует конфигурацию текущей отображаемой диаграммы.
     */
    private fun refreshDiagram() {
        val path = displayedConfigPath ?: return

        val currentConfig = findConfigByPath(path)
        if (currentConfig != null) {
            generateConfig(psiFile = currentConfig, status = "Refreshing diagram...")
            return
        }

        loadConfigs {
            val refreshedConfig = findConfigByPath(path)
            if (refreshedConfig == null) {
                clearDiagram()
                Messages.showWarningDialog(
                    project,
                    "The StateMachine configuration is no longer available.",
                    DIALOG_TITLE
                )
                return@loadConfigs
            }
            generateConfig(
                psiFile = refreshedConfig,
                status = "Refreshing diagram..."
            )
        }
    }

    /**
     * Очищает текущую диаграмму, сохраняя выбранную конфигурацию.
     */
    private fun clearDiagram() {
        operationSequence++
        busy = false
        hideProgress()
        diagramPanel.clear()
        displayedConfigPath = null
        scrollPane
            .viewport
            .viewPosition =
            Point(0, 0)
        updateButtons()
    }

    /**
     * Подбирает масштаб так, чтобы диаграмма помещалась в видимую область.
     */
    private fun fitDiagram() {
        diagramPanel.fitToViewport(scrollPane.viewport.extentSize)
        SwingUtilities.invokeLater(
            ::centerDiagram
        )
    }

    /**
     * Центрирует диаграмму внутри viewport.
     */
    private fun centerDiagram() {
        val viewport = scrollPane.viewport
        val viewSize = diagramPanel.preferredSize
        val extentSize = viewport.extentSize

        val x = ((viewSize.width - extentSize.width) / 2).coerceAtLeast(0)
        val y = ((viewSize.height - extentSize.height) / 2).coerceAtLeast(0)
        viewport.viewPosition = Point(x, y)
    }

    /**
     * Начинает фоновую UI-операцию и показывает прогресс.
     */
    private fun beginOperation(status: String): Long {
        val operation = ++operationSequence
        busy = true
        showProgress(status)
        updateButtons()
        return operation
    }

    /**
     * Завершает актуальную фоновую операцию и скрывает прогресс.
     */
    private fun finishOperation(operation: Long) {
        if (!isCurrentOperation(operation)) {
            return
        }
        busy = false
        hideProgress()
        updateButtons()
    }

    /**
     * Проверяет, относится ли callback к последней запущенной операции.
     */
    private fun isCurrentOperation(operation: Long): Boolean = operation == operationSequence

    /**
     * Показывает индикатор и текст текущей операции.
     */
    private fun showProgress(status: String) {
        statusLabel.text = status
        progressIcon.isVisible = true
        statusLabel.isVisible = true
        statusPanel.isVisible = true
        progressIcon.resume()
        statusPanel.revalidate()
        statusPanel.repaint()
    }

    /**
     * Скрывает индикатор фоновой операции.
     */
    private fun hideProgress() {
        progressIcon.suspend()
        progressIcon.isVisible = false
        statusLabel.isVisible = false
        statusPanel.isVisible = false
        statusPanel.revalidate()
        statusPanel.repaint()
    }

    /**
     * Обновляет доступность кнопок согласно текущему состоянию панели.
     */
    private fun updateButtons() {
        val hasSelectedConfig = selectedConfig() != null
        val hasDiagram = displayedConfigPath != null
        configComboBox.isEnabled = !busy && configComboBox.itemCount > 0
        generateButton.isEnabled = !busy && hasSelectedConfig
        openConfigButton.isEnabled = !busy && hasSelectedConfig
        refreshButton.isEnabled = !busy && hasDiagram

        clearButton.isEnabled = hasDiagram
        zoomOutButton.isEnabled = hasDiagram
        zoomButton.isEnabled = hasDiagram
        zoomInButton.isEnabled = hasDiagram
        fitButton.isEnabled = hasDiagram
        centerButton.isEnabled = hasDiagram
    }

    /**
     * Возвращает конфигурацию, выбранную в ComboBox.
     */
    private fun selectedConfig(): PsiJavaFile? = configComboBox.selectedItem as? PsiJavaFile

    /**
     * Ищет загруженную конфигурацию по пути файла.
     */
    private fun findConfigByPath(path: String): PsiJavaFile? =
        (0 until configComboBox.itemCount)
            .map(configComboBox::getItemAt)
            .firstOrNull { file ->
                file.virtualFile?.path == path
            }

    /**
     * Возвращает Java-файл, содержащий анализируемую конфигурацию.
     */
    private fun getConfigFile(model: StateMachineModel): PsiJavaFile? =
        model
            .config
            .configClass
            .containingFile
                as? PsiJavaFile

    /**
     * Синхронизирует выбор ComboBox с отображаемой моделью.
     */
    private fun selectConfigForModel(model: StateMachineModel) {
        val path = getConfigFile(model)
            ?.virtualFile
            ?.path
            ?: return

        if (selectConfigByPath(path)) {
            return
        }
        if (busy) {
            return
        }
        loadConfigs {
            selectConfigByPath(path)
        }
    }

    /**
     * Выбирает конфигурацию в ComboBox по пути файла.
     */
    private fun selectConfigByPath(path: String): Boolean {
        for (index in 0 until configComboBox.itemCount) {
            val file = configComboBox.getItemAt(index)
            if (file.virtualFile?.path == path) {
                configComboBox.selectedIndex = index
                return true
            }
        }
        return false
    }

    /**
     * Показывает диалог с ошибкой фоновой операции.
     */
    private fun showError(message: String, error: Throwable) {
        Messages.showErrorDialog(
            project,
            buildString {
                append(message)
                error.message
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        append("\n\n")
                        append(it)
                    }
            },
            DIALOG_TITLE
        )
    }

    /**
     * Передаёт настройки отображения в панель диаграммы.
     */
    private fun updateDisplayOptions() {
        diagramPanel.setDisplayOptions(
            DiagramDisplayOptions(
                hideGuards = hideGuardsCheckBox.isSelected,
                hideActions = hideActionsCheckBox.isSelected
            )
        )
    }

    private companion object {
        const val CONFIG_COMBO_WIDTH = 230
        const val ZOOM_BUTTON_WIDTH = 70
        const val DIALOG_TITLE = "Spring StateMachine Diagram"
    }
}