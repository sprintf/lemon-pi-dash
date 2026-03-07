import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val grpcVersion: String by project
val protobufVersion: String by project

plugins {
	idea
	application
	id("org.springframework.boot") version "3.2.0"
	id("io.spring.dependency-management") version "1.1.4"
	kotlin("jvm") version "1.9.21"
	kotlin("plugin.spring") version "1.9.21"
}

group = "com.normtronix"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
	maven {
		name = "GitHubPackages"
		url = uri("https://maven.pkg.github.com/sprintf/lemon-pi-protos")
		credentials {
			username = System.getenv("GITHUB_ACTOR")
			password = System.getenv("GITHUB_TOKEN")
		}
	}
}

extra["springCloudGcpVersion"] = "5.0.0"
extra["springCloudVersion"] = "2023.0.0"
extra["gcpLibrariesVersion"] = "26.32.0"

dependencies {
	implementation("com.normtronix:lemon-pi-protos:2.0")

	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.3")

	implementation("io.grpc:grpc-protobuf:${grpcVersion}")
	implementation("io.grpc:grpc-stub:${grpcVersion}")
	implementation("com.google.protobuf:protobuf-java:${protobufVersion}")

	implementation("io.grpc:grpc-kotlin-stub:1.4.1")
	compileOnly("jakarta.annotation:jakarta.annotation-api:1.3.5") // Java 9+ compatibility - Do NOT update to 2.0.0
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("org.hibernate.validator:hibernate-validator")
	implementation("com.google.cloud:google-cloud-firestore")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("com.squareup.okhttp3:okhttp:4.11.0")
	implementation("com.slack.api:slack-api-client:1.27.1")

	runtimeOnly("io.grpc:grpc-okhttp:${grpcVersion}")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.mockk:mockk:1.12.2")

	testImplementation("org.testcontainers:testcontainers:1.19.0")
	testImplementation("org.testcontainers:junit-jupiter:1.19.0")
	testImplementation("org.testcontainers:gcloud:1.19.0")
}

dependencyManagement {
	imports {
		mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:${property("springCloudGcpVersion")}")
		mavenBom("com.google.cloud:libraries-bom:${property("gcpLibrariesVersion")}")
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

configurations.all {
	resolutionStrategy.eachDependency {
		if (requested.group == "io.grpc" && requested.name != "grpc-kotlin-stub") {
			useVersion(grpcVersion)
		}
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
