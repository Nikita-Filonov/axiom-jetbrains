# Axiom Test Runner Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2026-07-01

### Added

- Green gutter icon on every `TestXxx` method of an Axiom suite.
- Full native GoLand menu (Run / Debug / Run with Coverage / Profile with CPU,
  Memory, Blocking Profiler / Modify Run Configuration…) via a proper
  `RunConfigurationProducer`.
- Framework-agnostic detection: supports `axiom.NewSuite(t, new(T))` and
  `axiom.NewSuiteFactory(t, func() *T { … })`, regardless of what `T` embeds.
- Correct `Kind = Package` + module-aware import path resolution so the
  debugger works and only the target subtest is executed.
- PSI-level caching (`CachedValuesManager` + `PsiModificationTracker`) — no
  performance impact when scrolling large files.

[Unreleased]: https://github.com/Nikita-Filonov/axiom-jetbrains/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Nikita-Filonov/axiom-jetbrains/releases/tag/v1.0.0
