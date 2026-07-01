import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    intellijPlatform {
        create(properties("platformType"), properties("platformVersion"))

        bundledPlugins(properties("platformBundledPlugins").map { it.split(',').map(String::trim) })

        pluginVerifier()
        zipSigner()

        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    pluginConfiguration {
        version = properties("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and inject
        // it into the plugin.xml `<description>` element so users see the same copy.
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map { text ->
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"
            require(text.contains(start) && text.contains(end)) {
                "README.md is missing plugin description markers: $start / $end"
            }
            text.substringAfter(start).substringBefore(end).let(::markdownToHTML)
        }

        // Same trick for release notes – latest 'Unreleased' section of CHANGELOG.md
        // becomes the plugin.xml `<change-notes>`.
        val changelog = project.changelog
        changeNotes = properties("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    signing {
        certificateChain = environment("CERTIFICATE_CHAIN")
        privateKey = environment("PRIVATE_KEY")
        password = environment("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = environment("PUBLISH_TOKEN")

        // Pre-releases like 1.2.0-beta.1 are pushed to the matching channel (e.g. "beta");
        // stable releases go to the default (public) channel.
        channels = properties("pluginVersion").map {
            listOf(it.substringAfter('-', "").substringBefore('.').ifBlank { "default" })
        }
    }

    pluginVerification {
        ides { recommended() }
    }
}

changelog {
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}
