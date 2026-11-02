# Changelog

All notable changes to SSM Diagram are documented in this file.

## [1.0.14] - 2026-11-2

### Added
- Скрыть guards, actions
- тех. статус внутри ромбика

## [1.0.13] - 2026-11-2

### Added
- Изменена подсветка выбранных элементов на диаграмме.
- Решена проблема сложных состояний, у которых много вх. и вых. стрелок

## [1.0.12] - 2026-10-30

### Added
- Refactoring

## [1.0.11] - 2026-10-30

### Added
- Zoom control has been added using the mouse wheel.
- Movement along the diagram has been added via the space + left mouse button.

## [1.0.10] - 2026-10-26

### Added
- Support for Spring StateMachine states declared with `states(EnumSet.allOf(CarState.class)`.
- icons
- description

## [1.0.7] - 2026-10-04

### Added
- Interactive states selection with incoming transitions

---

## [1.0.6] - 2026-10-04

### Added
- Support for Spring StateMachine states declared with `Set.of(MyState.values())`.
- Navigation from diagram states directly to enum constants.

### Fixed
- Improved diagram interaction.

---

## [1.0.5] - 2026-10-04

### Added
- Interactive transition selection.
- Event, guard and action highlighting.

### Fixed
- Improved diagram interaction.

---

## [1.0.4] - 2026-09-28

### Added
- Action navigation.
- Multiple actions support.

### Fixed
- Fixed PSI access from Swing EDT.

---

## [1.0.0] - 2026-09-18

### Added
- Initial release.
- Spring StateMachine configuration detection.
- State and transition visualization.
- Event navigation.
- Guard navigation.
- Hierarchical diagram layout.
- Zoom, Fit and Center controls.