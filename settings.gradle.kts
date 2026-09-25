pluginManagement {
    if (providers.gradleProperty("openvisum.cnMirrors").map { it.toBoolean() }.getOrElse(false)) {
        repositories {
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
        }
    }
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    if (providers.gradleProperty("openvisum.cnMirrors").map { it.toBoolean() }.getOrElse(false)) {
        repositories {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
        }
    }
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "OpenVisum"

include(":app")
include(":core:common")
include(":core:player")
include(":core:data")
