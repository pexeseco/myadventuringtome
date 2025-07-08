pluginManagement {
    repositories {
        google()  // Adiciona o repositório Google para plugins
        mavenCentral()  // Repositório Maven Central
        gradlePluginPortal()  // Repositório de plugins do Gradle
    }
}

dependencyResolutionManagement {
    repositories {
        google()  // Adiciona o repositório Google para dependências
        mavenCentral()  // Repositório Maven Central
    }
}

rootProject.name = "MyAdventuringTome"
include(":app")