import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val grpcVersion: String by project
val protobufVersion: String by project
val protobufPluginVersion: String by project

plugins {
	idea
	application
	id("org.springframework.boot") version "2.6.4"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	kotlin("jvm")
	kotlin("plugin.spring")
}

group = "com.normtronix"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

extra["springCloudGcpVersion"] = "2.0.6"
extra["springCloudVersion"] = "2020.0.4"
extra["gcpLibrariesVersion"] = "26.1.4"

dependencies {
	implementation(project(":lemon-pi-protos"))

	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.5.2")

	implementation("io.grpc:grpc-protobuf:${grpcVersion}")
	implementation("io.grpc:grpc-stub:${grpcVersion}")
	implementation("io.grpc:grpc-kotlin-stub:0.1.5")
	implementation("jakarta.validation:jakarta.validation-api")
	implementation("org.hibernate.validator:hibernate-validator")
	implementation("com.google.cloud:google-cloud-firestore")

	runtimeOnly("io.grpc:grpc-okhttp:${grpcVersion}")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("io.mockk:mockk:1.12.2")
}

dependencyManagement {
	imports {
		mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:${property("springCloudGcpVersion")}")
		mavenBom("com.google.cloud:libraries-bom:${property("gcpLibrariesVersion")}")
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
