rootProject.name = "PermPacks"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
        maven("https://repo.codemc.org/repository/maven-public/") {
            name = "codemc-repo"
        }
    }
}
