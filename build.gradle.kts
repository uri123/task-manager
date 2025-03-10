import org.eclipse.jkube.kit.common.AssemblyConfiguration
import org.eclipse.jkube.kit.config.image.build.BuildConfiguration
import java.util.*

plugins {
    java
    alias(libs.plugins.quarkus)
    alias(libs.plugins.jkube.kubernetes)
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.platform))
    implementation(libs.bundles.quarkus.build)
    runtimeOnly(libs.quarkus.reactive.pg.client)
    testImplementation(libs.bundles.quarkus.test)
}

group = "com.example.fullstack"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kubernetes {
    addImage {
        name("uris753/task-manager:fly")
        build(BuildConfiguration.builder().apply {
            dockerFile("$projectDir/Dockerfile.fly")
            contextDir("$projectDir")
            assembly(AssemblyConfiguration.builder().apply {
                name("fly")
            }.build())
        }.build())
    }
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

val osName: String
    get() {
        val name = System.getProperty("os.name").lowercase(Locale.getDefault())
        return when {
            name.contains("windows") -> "win"
            name.contains("mac") -> "darwin"
            name.contains("linux") -> "linux"
            name.contains("freebsd") -> "linux"
            name.contains("sunos") -> "sunos"
            else -> error("Unsupported OS: $name")
        }
    }

val isWindows: Boolean
    get() = osName == "win"

fun translateCommand(command: String): String = if (isWindows) "${command}.cmd" else command

val npmCommand: String
    get() = translateCommand("npm")

val frontEndDir: String = "src/main/frontend"
val frontStartWorkingDir: String = "$projectDir/$frontEndDir"
val frontEndPackageFile = "$frontEndDir/package.json"
val frontEndPackageLockFile = "$frontEndDir/package-lock.json"

tasks.register<Exec>("npmInstall") {
    inputs.file(frontEndPackageFile)
    outputs.file(frontEndPackageLockFile)
    outputs.dir("$frontEndDir/node_modules")
    workingDir(frontStartWorkingDir)
    commandLine(npmCommand, "install")
}
tasks.register<Exec>("npmBuild") {
    inputs.file(frontEndPackageFile)
    inputs.file(frontEndPackageLockFile)
    outputs.dir("$frontEndDir/build")
    dependsOn("npmInstall")
    workingDir(frontStartWorkingDir)
    commandLine(npmCommand, "run", "build")
}
tasks.named<ProcessResources>("processResources") {
    dependsOn("npmBuild")
    doLast {
        copy {
            from("$projectDir/$frontEndDir/build")
            into("$destinationDir/frontend")
        }
    }
}
