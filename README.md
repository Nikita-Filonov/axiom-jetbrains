# Axiom Test Runner for GoLand

<p align="center">
  <img src="https://raw.githubusercontent.com/Nikita-Filonov/axiom/main/docs/assets/logo.png" alt="Axiom logo" width="220" />
</p>

🧪 First-class GoLand support for the [**Axiom**](https://github.com/Nikita-Filonov/axiom) Go test framework.

[![Build](https://github.com/Nikita-Filonov/axiom-jetbrains/actions/workflows/build.yml/badge.svg)](https://github.com/Nikita-Filonov/axiom-jetbrains/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/32606-axiom-test-runner.svg)](https://plugins.jetbrains.com/plugin/32606-axiom-test-runner)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/32606-axiom-test-runner.svg)](https://plugins.jetbrains.com/plugin/32606-axiom-test-runner)
[![Rating](https://img.shields.io/jetbrains/plugin/r/rating/32606-axiom-test-runner.svg)](https://plugins.jetbrains.com/plugin/32606-axiom-test-runner/reviews)
[![License](https://img.shields.io/github/license/Nikita-Filonov/axiom-jetbrains)](./LICENSE)

_Made with ❤️ by [@NikitaFilonov](https://t.me/sound_right)_

---

## 📑 Table of Contents

- ✨ [About](#-about)
- 🖼 [Demo](#-demo)
- 📦 [Installation](#-installation)
- 🚀 [Usage](#-usage)
- ⚙️ [How it works](#️-how-it-works)
- 🛠 [Development](#-development)
- 🤝 [Contributing](#-contributing)

---

<!-- Plugin description -->

**Axiom Test Runner** adds first-class GoLand support for the
[Axiom](https://github.com/Nikita-Filonov/axiom) Go test framework.

Every `TestXxx` method on a struct registered with `axiom.NewSuite` (or
`axiom.NewSuiteFactory`) gets a green gutter icon exactly like a plain
`func TestFoo(t *testing.T)`. Clicking it opens the full native GoLand
menu — **Run**, **Debug**, **Run with Coverage**, **Profile with CPU / Memory /
Blocking Profiler**, **Modify Run Configuration…** — and results appear in
the standard *Test Results* tool window.

Framework-agnostic: works regardless of what your suite struct embeds
(`axiom.Suite`, a company-specific `BaseSuite`, or a custom wrapper) — the
plugin only looks for the `axiom.NewSuite` registration, not for a magic
base type.

<!-- Plugin description end -->

---

## ✨ About

Axiom encourages structuring integration tests as suites — methods on a struct
that is registered with `axiom.NewSuite(t, new(mySuite))`. This mirrors the
grouping you get with `testify/suite`, `xUnit`, or `JUnit5`, but keeps Go's
`go test` runtime intact.

Native GoLand has no idea about the Axiom lifecycle: it only shows the
green run arrow on the top-level `TestFooSuite(t *testing.T)` function.
Running a single subtest requires typing a `-run "^TestFooSuite$/^TestBar$"`
pattern by hand into a run configuration — tedious and error-prone.

This plugin fixes that. It teaches GoLand to recognise Axiom suite methods and
build a proper `GoTestRunConfiguration` (with the correct `-run` filter and
package scope) automatically, so every subtest behaves like a first-class
Go test.

---

## 🖼 Demo

### Green gutter icon on every suite method

Every method that starts with `Test` on an Axiom suite gets a native run
icon — helpers are silently skipped.

![Gutter icons on Axiom suite methods](https://raw.githubusercontent.com/Nikita-Filonov/axiom-jetbrains/main/docs/media/gutter-icons.png)

### Full native Run / Debug / Coverage / Profile menu

Click the icon — get the exact same menu you'd get on a plain
`func TestFoo(t *testing.T)`. No custom wrappers, no fake tool windows.

![Right-click context menu with Run/Debug/Coverage/Profile actions](https://raw.githubusercontent.com/Nikita-Filonov/axiom-jetbrains/main/docs/media/context-menu.png)

### Standard Test Results tool window

Results land in GoLand's regular test tree — subtest hierarchy,
individual timings, re-run failed, everything for free.

![Test Results tool window with green tests](https://raw.githubusercontent.com/Nikita-Filonov/axiom-jetbrains/main/docs/media/test-results.png)

### Real debugger, real breakpoints

Set a breakpoint anywhere inside the suite method and hit **Debug** —
the debugger stops on the line, no "Cannot debug tests in
directory-kind run configurations" workarounds needed.

![Debugger stopped on a breakpoint inside a suite method](https://raw.githubusercontent.com/Nikita-Filonov/axiom-jetbrains/main/docs/media/debug-session.png)

---

## 📦 Installation

### From JetBrains Marketplace (recommended)

<a href="https://plugins.jetbrains.com/plugin/32606-axiom-test-runner">
  <img src="https://user-images.githubusercontent.com/6104164/50148598-7e51fa00-02b0-11e9-9c99-c3ea55d9daf7.png" alt="Get from Marketplace" width="200"/>
</a>

Or from inside the IDE:

1. **Settings → Plugins → Marketplace**.
2. Search for **Axiom Test Runner**.
3. **Install** and restart the IDE.

### From a release ZIP

1. Grab the latest `.zip` from [Releases](https://github.com/Nikita-Filonov/axiom-jetbrains/releases).
2. In GoLand: **Settings → Plugins → ⚙ → Install Plugin from Disk…**.
3. Select the ZIP and restart.

### Supported IDEs

| IDE                          | Version         |
|------------------------------|-----------------|
| GoLand                       | 2024.3 and newer |
| IntelliJ IDEA Ultimate       | 2024.3 and newer *(with the bundled Go plugin enabled)* |

---

## 🚀 Usage

Once installed, any file that contains a suite registration is instantly
supported. No configuration is required.

```go
package cards

import (
	"testing"

	"github.com/Nikita-Filonov/axiom"
)

func TestCardServiceSuite(t *testing.T) {
	axiom.NewSuite(t, new(serviceSuite)).Run()
}

type serviceSuite struct {
	axiom.Suite
}

//   ▶ Green gutter icon appears here → click for Run/Debug/Coverage/Profile menu.
func (s *serviceSuite) TestGetCard()   { /* ... */ }
func (s *serviceSuite) TestListCards() { /* ... */ }

// Helper – no gutter icon (does not start with `Test`).
func (s *serviceSuite) loadFixture(name string) string { return name }
```

Under the hood the plugin builds this run configuration for you:

```
Test kind:     Package
Package path:  github.com/you/project/pkg/cards
Pattern:       ^TestCardServiceSuite$/^TestGetCard$
Working dir:   <project root with go.mod>
```

---

## ⚙️ How it works

| Component                              | Responsibility                                                                 |
|----------------------------------------|--------------------------------------------------------------------------------|
| `AxiomSuiteDetector`                   | Pure PSI check: is this struct passed to `axiom.NewSuite`?                     |
| `AxiomTestTarget`                      | Resolves method + package import path + module root into a runnable target.    |
| `GoTestConfigurator`                   | Isolates all Go-plugin-specific API calls (single point of update per version). |
| `AxiomTestRunConfigurationProducer`    | Standard IntelliJ producer — powers Run / Debug / Coverage / Profile actions.  |
| `AxiomSuiteRunLineMarkerContributor`   | Adds the green icon and delegates to `ExecutorAction.getActions(0)`.           |

Detection is deliberately **framework-agnostic**: the plugin only looks for
receiver types that end up inside `axiom.NewSuite(t, new(T))` or
`axiom.NewSuiteFactory(t, func() *T { … })`. It does not care whether `T`
embeds `axiom.Suite`, `testsuite.BaseSuite`, or something else entirely.

Results are cached per PSI directory and invalidated on any source change,
so scrolling through a large file has zero impact.

---

## 🛠 Development

Requirements:

- JDK 21 (a Zulu build is used in CI)
- GoLand 2024.3 as the target IDE (auto-downloaded by Gradle)

Common tasks:

```bash
./gradlew runIde              # Launch a sandbox GoLand with the plugin installed
./gradlew test                # Run unit + Platform tests
./gradlew check               # Everything + static checks
./gradlew buildPlugin         # Produce build/distributions/*.zip
./gradlew verifyPlugin        # Run JetBrains Plugin Verifier
```

CI runs on every push and PR:

- `build` — compiles the plugin ZIP.
- `test` — executes unit tests and Platform tests.
- `verify` — runs JetBrains Plugin Verifier against several IDE versions.
- `releaseDraft` (main only) — prepares a draft GitHub Release.

### Publishing (maintainer notes)

Publishing to the Marketplace is fully automated once the required secrets
are configured:

1. Bump `pluginVersion` in `gradle.properties`.
2. Add a section to `CHANGELOG.md` under `## [Unreleased]`.
3. Merge to `main` — CI drafts a GitHub Release from the changelog.
4. Edit the draft release notes on GitHub, then **Publish release**.
5. The `release` workflow signs and uploads the ZIP to JetBrains Marketplace.

Required GitHub Secrets (see `.github/workflows/release.yml`):

| Secret                 | How to obtain |
|------------------------|---------------|
| `PUBLISH_TOKEN`        | https://plugins.jetbrains.com/author/me/tokens |
| `CERTIFICATE_CHAIN`    | Full PEM certificate chain used to sign the plugin. |
| `PRIVATE_KEY`          | Corresponding private key (PEM). |
| `PRIVATE_KEY_PASSWORD` | Password protecting the private key. |

Step-by-step instructions:
[Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html) ·
[Publishing a Plugin](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html).

---

## 🤝 Contributing

Bug reports, feature requests and pull requests are very welcome.
Please:

1. Check open [issues](https://github.com/Nikita-Filonov/axiom-jetbrains/issues) first.
2. Attach a minimal reproducer file (see `src/test/testData/axiom/*` for the shape).
3. Run `./gradlew check` locally before opening a PR.

---

## 📄 License

[Apache 2.0](./LICENSE)
