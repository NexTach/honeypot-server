plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
	kotlin("plugin.jpa") version "2.3.21"
}

group = "team.themoment"
version = "0.0.1-SNAPSHOT"
description = "honeypot-server"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
	maven { url = uri("https://jitpack.io") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("tools.jackson.module:jackson-module-kotlin")
	runtimeOnly("com.mysql:mysql-connector-j")

	// Team SDKs
	implementation("com.github.themoment-team:the-sdk:1.4")
	implementation("com.github.themoment-team:datagsm-oauth-sdk-java:1.5.0")

	// AWS SDK v2 (SeaweedFS S3-compatible)
	implementation(platform("software.amazon.awssdk:bom:2.+"))
	implementation("software.amazon.awssdk:s3")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.+")
	implementation("io.jsonwebtoken:jjwt-impl:0.12.+")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.+")

	// SpringDoc (Swagger UI) — pin 3.0.x for Spring Boot 4 / Spring Framework 7 compatibility.
	// the-sdk pulls 2.8.x transitively, which is built against Boot 3 and fails at startup
	// (NoClassDefFoundError: WebMvcProperties). This direct dependency overrides it.
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-security-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
}

allOpen {
	annotation("jakarta.persistence.Entity")
	annotation("jakarta.persistence.MappedSuperclass")
	annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
