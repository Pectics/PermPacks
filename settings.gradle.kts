rootProject.name = "PermPacks"

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        maven("https://maven.aliyun.com/repository/central")
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo"
        }
        maven("https://repo.codemc.org/repository/maven-public/") {
            name = "codemc-repo"
        }
    }
}
