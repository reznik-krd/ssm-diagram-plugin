package com.reznik.krd.ssmdiagramplugin.export

import com.reznik.krd.ssmdiagramplugin.model.SsmState
import com.reznik.krd.ssmdiagramplugin.model.StateMachineModel
import java.util.ArrayDeque

/**
 * Генерирует PlantUML State Diagram из логической модели
 * Spring StateMachine.
 *
 * Не экспортирует Guards, Actions и визуальные копии состояний.
 */
class PlantUmlExporter {

    /**
     * Настройки экспорта диаграммы состояний.
     */
    data class Options(
        val compactTransitions: Boolean = true,
        val topToBottom: Boolean = true,
        val showChoiceNames: Boolean = true,
        val useColors: Boolean = true
    )

    /**
     * Описывает обычный переход для группировки по
     * исходному и целевому состояниям.
     */
    private data class TransitionData(
        val source: String,
        val target: String,
        val event: String?
    )

    /**
     * Генерирует полное описание PlantUML.
     */
    fun export(
        model: StateMachineModel,
        options: Options = Options()
    ): String {
        val states = orderStates(model)

        val aliases = states.mapIndexed { index, state ->
            state.name to "S$index"
        }.toMap()

        return buildString {
            appendHeader(options)
            appendStates(states, aliases, options)
            appendInitialStates(states, aliases)
            appendTransitions(model, states, aliases, options)
            appendChoices(model, states, aliases)
            appendFinalStates(states, aliases)
            appendLine("@enduml")
        }
    }

    /**
     * Упорядочивает состояния обходом от INITIAL.
     *
     * Включает обычные переходы и ветви Choice.
     * Циклы не приводят к повторному посещению состояний.
     * Недостижимые состояния добавляются в конце.
     */
    private fun orderStates(
        model: StateMachineModel
    ): List<SsmState> {
        val statesByName = model.states.associateBy { it.name }

        val adjacency =
            mutableMapOf<String, MutableList<String>>()

        model.transitions.forEach { transition ->
            adjacency.getOrPut(transition.source) {
                mutableListOf()
            }.add(transition.target)
        }

        model.choices.forEach { choice ->
            choice.branches.forEach { branch ->
                adjacency.getOrPut(choice.source) {
                    mutableListOf()
                }.add(branch.target)
            }
        }

        val visited = mutableSetOf<String>()
        val ordered = mutableListOf<SsmState>()

        fun traverse(start: String) {
            val queue = ArrayDeque<String>()
            queue.addLast(start)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()

                if (!visited.add(current)) {
                    continue
                }

                statesByName[current]?.let(ordered::add)

                adjacency[current]
                    .orEmpty()
                    .distinct()
                    .forEach { target ->
                        if (target !in visited) {
                            queue.addLast(target)
                        }
                    }
            }
        }

        model.states
            .filter { it.type == SsmState.Type.INITIAL }
            .forEach { traverse(it.name) }

        model.states.forEach { state ->
            if (state.name !in visited) {
                traverse(state.name)
            }
        }

        return ordered
    }

    /**
     * Добавляет заголовок и параметры оформления диаграммы.
     */
    private fun StringBuilder.appendHeader(
        options: Options
    ) {
        appendLine("@startuml")
        appendLine("hide empty description")

        if (options.topToBottom) {
            appendLine("top to bottom direction")
        } else {
            appendLine("left to right direction")
        }

        appendLine("skinparam shadowing false")
        appendLine("skinparam linetype polyline")
        appendLine("skinparam ranksep 45")
        appendLine("skinparam nodesep 30")

        if (options.useColors) {
            appendColors()
        }

        appendLine()
    }

    /**
     * Настраивает зелёные состояния, лавовые Choice
     * и нейтральные стрелки.
     */
    private fun StringBuilder.appendColors() {
        appendLine("<style>")

        appendLine("stateDiagram {")
        appendLine("  state {")
        appendLine("    BackgroundColor #E4F4E9")
        appendLine("    LineColor #50A875")
        appendLine("    FontColor #256C45")
        appendLine("    FontSize 12")
        appendLine("    RoundCorner 8")
        appendLine("  }")

        appendLine("  arrow {")
        appendLine("    LineColor #8291A6")
        appendLine("    FontColor #536273")
        appendLine("    FontSize 11")
        appendLine("  }")

        appendLine("}")

        appendLine("diamond {")
        appendLine("  BackgroundColor #FFE6D5")
        appendLine("  LineColor #E67E22")
        appendLine("  LineThickness 2")
        appendLine("}")

        appendLine("note {")
        appendLine("  BackgroundColor #FFE6D5")
        appendLine("  LineColor #E67E22")
        appendLine("  FontColor #8A4312")
        appendLine("  FontSize 11")
        appendLine("}")

        appendLine("</style>")
    }

    /**
     * Объявляет все состояния.
     * Для технических Choice добавляет именные заметки.
     */
    private fun StringBuilder.appendStates(
        states: List<SsmState>,
        aliases: Map<String, String>,
        options: Options
    ) {
        states.forEach { state ->
            val alias = aliases.getValue(state.name)
            val label = escape(state.name)

            if (state.type == SsmState.Type.CHOICE) {
                appendLine(
                    "state \"$label\" as $alias <<choice>>"
                )
            } else {
                appendLine(
                    "state \"$label\" as $alias"
                )
            }
        }

        appendLine()

        if (options.showChoiceNames) {
            states
                .filter { it.type == SsmState.Type.CHOICE }
                .forEach { state ->
                    val alias = aliases.getValue(state.name)

                    appendLine(
                        "note right of $alias : ${escape(state.name)}"
                    )
                }
        }

        appendLine()
    }

    /**
     * Выводит начальные маркеры StateMachine.
     */
    private fun StringBuilder.appendInitialStates(
        states: List<SsmState>,
        aliases: Map<String, String>
    ) {
        states
            .filter { it.type == SsmState.Type.INITIAL }
            .forEach { state ->
                appendLine(
                    "[*] --> ${aliases.getValue(state.name)}"
                )
            }

        appendLine()
    }

    /**
     * Экспортирует переходы в обычном или компактном режиме.
     */
    private fun StringBuilder.appendTransitions(
        model: StateMachineModel,
        states: List<SsmState>,
        aliases: Map<String, String>,
        options: Options
    ) {
        val transitions = model.transitions.map {
            TransitionData(
                source = it.source,
                target = it.target,
                event = it.event
            )
        }

        val bySource = transitions.groupBy { it.source }

        states.forEach { state ->
            val sourceTransitions =
                bySource[state.name].orEmpty()

            if (options.compactTransitions) {
                appendCompactTransitions(
                    sourceTransitions,
                    aliases
                )
            } else {
                appendFullTransitions(
                    sourceTransitions,
                    aliases
                )
            }
        }

        appendLine()
    }

    /**
     * Выводит отдельную стрелку для каждого перехода.
     */
    private fun StringBuilder.appendFullTransitions(
        transitions: List<TransitionData>,
        aliases: Map<String, String>
    ) {
        transitions.forEach { transition ->
            val source = aliases[transition.source]
                ?: return@forEach

            val target = aliases[transition.target]
                ?: return@forEach

            val event = transition.event
                ?.takeIf { it.isNotBlank() }
                ?.let { " : ${escape(it)}" }
                .orEmpty()

            appendLine("$source --> $target$event")
        }
    }

    /**
     * Объединяет несколько событий одной пары состояний
     * в единственную визуальную стрелку.
     *
     * Все непустые уникальные названия событий сохраняются.
     */
    private fun StringBuilder.appendCompactTransitions(
        transitions: List<TransitionData>,
        aliases: Map<String, String>
    ) {
        transitions
            .groupBy { it.source to it.target }
            .forEach { (pair, grouped) ->
                val source = aliases[pair.first]
                    ?: return@forEach

                val target = aliases[pair.second]
                    ?: return@forEach

                val events = grouped
                    .mapNotNull { it.event }
                    .filter { it.isNotBlank() }
                    .distinct()

                val label = events
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString("\\n") {
                        escape(it)
                    }
                    ?.let { " : $it" }
                    .orEmpty()

                appendLine("$source --> $target$label")
            }
    }

    /**
     * Экспортирует все ветви Choice без Guards и Actions.
     */
    private fun StringBuilder.appendChoices(
        model: StateMachineModel,
        states: List<SsmState>,
        aliases: Map<String, String>
    ) {
        val choicesBySource =
            model.choices.groupBy { it.source }

        states.forEach { state ->
            choicesBySource[state.name]
                .orEmpty()
                .forEach { choice ->
                    val source = aliases[choice.source]
                        ?: return@forEach

                    choice.branches.forEach { branch ->
                        val target = aliases[branch.target]
                            ?: return@forEach

                        appendLine("$source --> $target")
                    }
                }
        }

        appendLine()
    }

    /**
     * Добавляет маркеры завершения для END-состояний.
     */
    private fun StringBuilder.appendFinalStates(
        states: List<SsmState>,
        aliases: Map<String, String>
    ) {
        states
            .filter { it.type == SsmState.Type.END }
            .forEach { state ->
                appendLine(
                    "${aliases.getValue(state.name)} --> [*]"
                )
            }

        appendLine()
    }

    /**
     * Экранирует текстовые значения PlantUML.
     */
    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "")
            .replace("\n", " ")
}
