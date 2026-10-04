# Spring StateMachine Diagram Plugin

An IntelliJ IDEA plugin for visualizing Spring StateMachine
configurations as interactive diagrams directly inside the IDE.

The plugin analyzes your Java StateMachine configuration and builds a
state/transition diagram without starting the Spring application.

## Features

-   Automatic discovery of Spring StateMachine configurations.
-   State and transition diagrams.
-   Visualization of states, initial / end states and choice states.
-   Visualization of events, guards and actions.
-   Navigation from diagram elements to source code.
-   Navigation to the selected StateMachine configuration.
-   State and transition highlighting.
-   Zoom, Fit and Center controls for large diagrams.
-   Multiple StateMachine configurations in one project.

## Requirements

-   IntelliJ IDEA 2025.3+
-   Java project
-   Spring StateMachine
-   The project must be indexed by IntelliJ IDEA.
-   StateMachine configuration extending:

``` java
EnumStateMachineConfigurerAdapter<State, Event>
```

Example:

``` java
@Configuration
@EnableStateMachine
public class CarStateMachineConfig
        extends EnumStateMachineConfigurerAdapter<CarState, CarEvent> {
}
```

The plugin uses IntelliJ PSI to analyze source code and does not require
starting the Spring Application Context.

## Installation

1.  Build or download the plugin ZIP archive.
2.  Open IntelliJ IDEA.
3.  Go to `Settings → Plugins → ⚙ → Install Plugin from Disk...`.
4.  Select the plugin ZIP archive.
5.  Restart IntelliJ IDEA if required.

After installation open `View → Tool Windows → SSM Diagram`.

Select a StateMachine configuration and click **Generate**.

## How it works

The plugin analyzes the StateMachine configuration using IntelliJ PSI:

`StateMachine Config → PSI Analysis → StateMachine Model → Diagram Graph → Interactive Diagram`

The Spring application is not started.
