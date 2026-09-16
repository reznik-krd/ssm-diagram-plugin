# SSM Diagram Plugin

> IntelliJ IDEA plugin for visualizing **Spring Statemachine**
> configurations directly inside the IDE.

SSM Diagram Plugin analyzes Java-based Spring Statemachine configuration
using IntelliJ PSI and builds an interactive diagram of states,
transitions, events, guards, actions and choice branches.

The goal is simple: **make large StateMachine configurations easier to
understand, navigate and maintain without leaving IntelliJ IDEA.**

------------------------------------------------------------------------

## Why this plugin?

As a Spring Statemachine configuration grows, understanding the complete
workflow from Java code becomes increasingly difficult. A configuration
may contain dozens of states, transitions, events, guards, actions,
choice pseudo-states and `first / then / last` branches.

The plugin converts that configuration into a visual graph directly
inside IntelliJ IDEA.

``` text
Spring Statemachine Java configuration
                  │
                  ▼
          SSM Diagram Plugin
                  │
       ┌──────────┼──────────┐
       ▼          ▼          ▼
     States   Transitions   Choices
                  │
          Events / Guards
                  │
               Actions
                  ▼
          Interactive diagram
```

No external website, Graphviz installation or running Spring application
is required.

------------------------------------------------------------------------

## Features

### Automatic configuration discovery

The plugin searches the current project for Spring Statemachine
configuration classes and makes discovered configurations available from
the **SSM Diagram** Tool Window.

### Diagram generation

The generated graph can contain:

-   states;
-   initial state;
-   explicit end states;
-   transitions;
-   events;
-   guards;
-   actions;
-   choice states;
-   choice branches;
-   `otherwise` branches.

### Choice visualization

Conditional branches such as `.first(...)`, `.then(...)` and
`.last(...)` are represented explicitly, so branching logic is visible
in the diagram instead of being hidden inside fluent Java configuration.

### Actions

Transition actions are displayed separately near the transition target.
Multiple actions remain separate rather than being merged into one long
label.

Error actions are preserved by the internal semantic model but are
intentionally not displayed on the main diagram to keep it readable.

### Interactive source navigation

Where source information can be resolved, diagram elements can navigate
back to project code:

-   states;
-   events;
-   guards;
-   actions.

The diagram therefore works not only as documentation, but also as an
additional navigation layer over a StateMachine implementation.

### Readable graph transformation

Real StateMachine graphs are not always visually pleasant. For states
with many incoming transitions, the plugin can create **visual copies**
of the same logical state to reduce unnecessarily long or tangled
connections.

``` text
        A                 B                 C
        │                 │                 │
        ▼                 ▼                 ▼
   ┌─────────┐       ┌─────────┐       ┌─────────┐
   │  READY  │       │  READY  │       │  READY  │
   └─────────┘       └─────────┘       └─────────┘
        │                 │                 │
        └──────── logicalId = READY ────────┘
```

These copies are only visual representations. They do **not** create
additional Spring states or alter StateMachine semantics.

### Hierarchical layout

The current layout engine places nodes in layers from top to bottom. The
pipeline includes graph-level calculation, ordering inside layers,
coordinate assignment, orthogonal routing, parallel-edge separation and
label positioning.

### Zoom and viewport controls

The Tool Window provides:

-   zoom out;
-   current zoom percentage / reset to 100%;
-   zoom in;
-   **Fit**;
-   **Center**;
-   `Ctrl` / `Cmd` + mouse wheel zoom.

### Generate, Refresh and Clear

-   **Generate** --- analyzes the configuration selected in the
    ComboBox.
-   **Refresh** --- re-analyzes the configuration currently displayed.
-   **Clear** --- removes the current diagram without clearing
    configuration selection.

### Generate from Project View

A diagram can also be generated directly from the IntelliJ project tree:

**Right click Java configuration → Generate SSM Diagram**

### Background PSI analysis

Configuration discovery and generation can require PSI resolution and
IntelliJ indexes. These operations are performed through non-blocking
read actions in smart mode instead of doing expensive analysis directly
on the Swing EDT.

The Tool Window can display statuses such as:

``` text
◌ Searching configurations...
◌ Generating diagram...
◌ Refreshing diagram...
```

------------------------------------------------------------------------

## Tool Window

The plugin adds the **SSM Diagram** Tool Window to IntelliJ IDEA.

``` text
┌──────────────────────────────────────────────────────────┐
│ SSM Diagram                                              │
├──────────────────────────────────────────────────────────┤
│ Configuration: [ CarSMConfig ▼ ] [Generate][Refresh][Clear]
│ View:          [ − ] [100%] [ + ] [Fit] [Center]        │
│                                                          │
│                  ●                                       │
│                  │                                       │
│                  ▼                                       │
│             ┌───────────┐                                │
│             │  CREATED  │                                │
│             └───────────┘                                │
│                  │                                       │
│             CREATE_EVENT                                 │
│                  │                                       │
│             [validGuard]                                 │
│                  ▼                                       │
│             ┌───────────┐                                │
│             │ PROCESSING│                                │
│             └───────────┘                                │
│                  │                                       │
│             ⚡ processAction                             │
└──────────────────────────────────────────────────────────┘
```

The actual diagram is rendered entirely inside IntelliJ IDEA.

------------------------------------------------------------------------

# How it works

## High-level architecture

The plugin separates Spring Statemachine semantics from visualization.

``` text
                    Java source
                        │
                        ▼
              ┌───────────────────┐
              │ SSM Config Finder │
              └─────────┬─────────┘
                        │
                        ▼
              ┌───────────────────┐
              │   PSI Analyzer    │
              └─────────┬─────────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
       States      Transitions      Choices
                        │
                        ▼
              ┌───────────────────┐
              │ StateMachineModel │
              └─────────┬─────────┘
                        │
                        ▼
              ┌───────────────────┐
              │DiagramGraphBuilder│
              └─────────┬─────────┘
                        │
                        ▼
              Canonical DiagramGraph
                        │
                        ▼
           ┌─────────────────────────┐
           │ReadableGraphTransformer │
           └────────────┬────────────┘
                        │
                        ▼
               Visual DiagramGraph
                        │
                        ▼
           ┌─────────────────────────┐
           │HierarchicalLayoutEngine │
           └────────────┬────────────┘
                        │
                        ▼
           ┌─────────────────────────┐
           │ OrthogonalEdgeRouter    │
           └────────────┬────────────┘
                        │
                        ▼
                 DiagramLayout
                        │
                        ▼
                 Swing Renderer
                        │
                        ▼
              IntelliJ Tool Window
```

## 1. Configuration discovery

The plugin uses IntelliJ's project model, PSI and inheritance indexes to
find supported Spring Statemachine configuration classes based on
`EnumStateMachineConfigurerAdapter`.

The Spring application does not need to be started and the application
context is not instantiated.

## 2. PSI analysis

Java source is analyzed through the **IntelliJ PSI (Program Structure
Interface)**.

The plugin therefore avoids relying on formatting-dependent source-text
parsing and regular expressions for understanding Java configuration.

The analyzer extracts a normalized structure such as:

``` text
StateMachine
├── states
├── initial state
├── end states
├── transitions
│   ├── source
│   ├── target
│   ├── event
│   ├── guard
│   └── actions
└── choices
    ├── source
    ├── first
    ├── then
    └── last
```

The expression resolver supports common forms including enum constants,
local variables, loop parameters and `List.of(...)` constructs.

## 3. Semantic model

PSI analysis produces a normalized `StateMachineModel`.

``` text
StateMachineModel
│
├── SsmState
│
├── SsmTransition
│   ├── source
│   ├── target
│   ├── event
│   ├── guard
│   └── List<SsmAction>
│
└── SsmChoice
    └── branches
```

This model represents the meaning of the Spring configuration rather
than positions on screen.

## 4. Canonical graph

`DiagramGraphBuilder` converts `StateMachineModel` into a canonical
visual graph.

At this stage:

``` text
one logical state = one graph node
```

The builder also creates visual markers for the initial state and
explicit Spring end states.

## 5. Readability transformation

`ReadableGraphTransformer` may transform the canonical graph into a more
readable visual graph. Visual state copies keep the same `logicalId`,
preserving the distinction between **logical identity** and **visual
identity**.

## 6. Hierarchical layout

`HierarchicalLayoutEngine` arranges the visual graph approximately as:

``` text
Level 0                 INITIAL
                           │
Level 1                 CREATED
                           │
                ┌──────────┴──────────┐
                ▼                     ▼
Level 2      PROCESSING             FAILED
                │
                ▼
Level 3       FINISHED
```

Nodes are positioned before edge routing begins.

## 7. Edge routing

`OrthogonalEdgeRouter` creates routes between final node positions and
calculates locations for transition labels.

Conceptually:

``` text
       EVENT
         ↓
      [GUARD]
         ↓
 ┌──────────────┐
 │ TARGET STATE │
 └──────────────┘
         ↓
   ⚡ firstAction
   ⚡ secondAction
```

Parallel transitions between the same source and target receive separate
offsets.

## 8. Rendering and interaction

The renderer receives already prepared graph geometry. Its main
responsibilities are drawing, hit areas, hover behavior and interaction.

Parsing, semantic analysis and graph layout remain outside the renderer.

------------------------------------------------------------------------

# Threading model

IntelliJ PSI and indexes have strict threading requirements. Potentially
expensive PSI/index operations must not block the Swing Event Dispatch
Thread.

The plugin follows this model:

``` text
                  EDT / Swing
                      │
            user presses Generate
                      │
                      ▼
          Non-blocking Read Action
                      │
                 Smart Mode
                      │
          ┌───────────┼───────────┐
          ▼           ▼           ▼
         PSI       indexes      resolve
          └───────────┼───────────┘
                      │
                      ▼
             StateMachineModel
                      │
                      ▼
                  EDT / Swing
                      │
                      ▼
                show diagram
```

------------------------------------------------------------------------

# Project structure

The structure may evolve as development continues, but responsibilities
are divided approximately as follows:

``` text
src/main/kotlin/com/reznik/krd/ssmdiagramplugin/
│
├── action/
│   └── IntelliJ actions
│
├── analyzer/
│   ├── configuration discovery
│   ├── PSI analysis
│   ├── transition analysis
│   ├── choice analysis
│   └── expression/component resolution
│
├── model/
│   └── StateMachine semantic model
│
├── service/
│   └── background analysis orchestration
│
└── ui/
    ├── Tool Window
    └── diagram/
        ├── graph model
        ├── graph builder
        ├── transformations
        ├── layout
        ├── routing
        ├── rendering
        └── interaction/navigation
```

------------------------------------------------------------------------

# Requirements

The current development setup uses:

-   **Java 21**;
-   **Gradle**;
-   **IntelliJ Platform Gradle Plugin**;
-   IntelliJ IDEA Java support;
-   a project containing Spring Statemachine Java configuration.

The exact supported IntelliJ build range is controlled by the
Gradle/plugin metadata of the project.

------------------------------------------------------------------------

# Build from source

## 1. Clone the repository

``` bash
git clone <repository-url>
cd ssm-diagram-plugin
```

Replace `<repository-url>` with the repository URL.

## 2. Build the plugin

macOS / Linux:

``` bash
./gradlew clean buildPlugin
```

Windows:

``` powershell
gradlew.bat clean buildPlugin
```

The distributable ZIP is generated in:

``` text
build/distributions/
```

For example:

``` text
build/distributions/ssm-diagram-plugin-1.0-SNAPSHOT.zip
```

**Do not unpack the ZIP before installing it into IntelliJ IDEA.**

------------------------------------------------------------------------

# Run in a development IDE

To launch an IntelliJ sandbox with the plugin installed:

``` bash
./gradlew runIde
```

This starts a separate IntelliJ IDEA instance suitable for plugin
development and testing.

------------------------------------------------------------------------

# Verify the plugin

Before sharing a build with testers:

``` bash
./gradlew clean buildPlugin
./gradlew verifyPlugin
```

Typical development flow:

``` text
change code
    │
    ▼
./gradlew runIde
    │
    ▼
manual testing
    │
    ▼
./gradlew clean buildPlugin
    │
    ▼
./gradlew verifyPlugin
    │
    ▼
build/distributions/*.zip
    │
    ▼
share with testers
```

------------------------------------------------------------------------

# Installation

## Install from ZIP

1.  Open **IntelliJ IDEA**.
2.  Open **Settings / Preferences**.
3.  Go to **Plugins**.
4.  Click the **⚙️** menu.
5.  Select **Install Plugin from Disk...**.
6.  Select the generated ZIP from `build/distributions`.
7.  Confirm installation.
8.  Restart IntelliJ IDEA if requested.

Select the ZIP directly --- it does not need to be extracted.

------------------------------------------------------------------------

# Usage

## Tool Window

Open **View → Tool Windows → SSM Diagram**, or use the **SSM Diagram**
Tool Window button on the side of IntelliJ IDEA.

The plugin searches for available configurations and populates the
selector. Select a configuration and press **Generate**.

## Refresh

After changing the Java StateMachine configuration, press **Refresh**.
The configuration currently displayed will be analyzed again.

## Clear

Press **Clear** to remove the current diagram while preserving
configuration selection.

## Generate from a Java file

1.  Find the StateMachine Java configuration in the project tree.
2.  Right-click the file.
3.  Select **Generate SSM Diagram**.
4.  The Tool Window opens and displays the generated diagram.

------------------------------------------------------------------------

# Diagram semantics

The plugin deliberately distinguishes several layers:

``` text
Spring semantics
      │
      ▼
StateMachineModel
      │
      ▼
Canonical DiagramGraph
      │
      ▼
Visual DiagramGraph
      │
      ▼
Rendered diagram
```

This separation allows visualization to improve without changing the
meaning of the StateMachine.

------------------------------------------------------------------------

# Design principles

### Keep Spring semantics intact

Visualization must never modify the semantic meaning extracted from
source configuration.

### PSI instead of source-text parsing

Java configuration should be understood through IntelliJ's program model
rather than formatting-dependent regular expressions.

### Renderer only renders

Parsing, semantic analysis, graph transformation and layout belong
outside the renderer.

### Visual readability over literal geometry

A diagram should explain the StateMachine rather than simply reproduce
every connection in the most literal possible geometry.

### Stay inside IntelliJ IDEA

The main workflow should not require copying generated definitions to
external diagram websites or running a separate visualization service.

------------------------------------------------------------------------

# Known limitations

The plugin is under active development.

Spring Statemachine configurations can be written in many different
styles. The analyzer currently focuses on Java configurations that can
be resolved statically through IntelliJ PSI and expression patterns
supported by the analyzer.

Highly dynamic configuration may require additional analyzer support.

The current layout engine prioritizes readability and may continue to
evolve for especially large, dense or cyclic graphs.

------------------------------------------------------------------------

# Possible future improvements

Potential directions include:

-   richer details popup for states and transitions;
-   displaying error actions in transition details;
-   additional source navigation;
-   improved handling of very large graphs;
-   further layout optimization;
-   improved cyclic/back-edge visualization;
-   diagram search;
-   highlighting related transitions;
-   diagram export;
-   support for additional Spring Statemachine configuration patterns.

------------------------------------------------------------------------

# Development notes

When extending the plugin, preserve the boundary between semantic and
visual models:

``` text
Java / PSI
    │
    ▼
Analyzer
    │
    ▼
StateMachineModel      ← Spring semantics
    │
    ▼
DiagramGraphBuilder
    │
    ▼
DiagramGraph           ← visual semantics
    │
    ▼
Layout / Router
    │
    ▼
Renderer
```

If a feature changes only how something looks, it should normally be
implemented **after** `StateMachineModel`.

If a feature changes what is understood from Spring configuration, it
belongs in the analyzer/model layer.

------------------------------------------------------------------------

# Contributing

Contributions, bug reports and ideas are welcome.

For parsing or visualization issues, useful information includes:

-   IntelliJ IDEA version;
-   plugin version;
-   a minimal Spring Statemachine configuration reproducing the issue;
-   expected behavior;
-   actual behavior;
-   relevant IDE log output if an exception occurs.

------------------------------------------------------------------------

# License

Add the project's license information here before public distribution.

If the project uses a standard license such as MIT or Apache 2.0, add
the corresponding `LICENSE` file and update this section.

------------------------------------------------------------------------

## Project status

The plugin is currently in active development and testing.

The current focus is reliable Spring Statemachine analysis, readable
visualization, IntelliJ-native navigation and responsive background
processing.
