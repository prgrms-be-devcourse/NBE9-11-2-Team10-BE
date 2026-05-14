plugins {
    java
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.21"
    kotlin("kapt") version "2.2.21"
}

group = "com.back"
version = "1.0.0-SNAPSHOT"
description = "NBE9-11-3-Team10"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 📌 Spring Boot (버전은 BOM이 자동 관리하므로 명시하지 않음)
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")

    // 📌 AWS
    implementation(platform("software.amazon.awssdk:bom:2.25.70"))
    implementation("software.amazon.awssdk:s3")

    // 📌 JWT
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    // 📌 Dev & DB
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    developmentOnly("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.mysql:mysql-connector-j")

    // 📌 Spring Retry & AOP
    implementation("org.springframework.retry:spring-retry:2.0.12")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.5.14")

    // 📌 Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // 📌 Test (Spring Boot는 단일 테스트 스타터로 통합 관리)
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // 📌 QueryDSL (KSP 기반, 추후 추가)
//    implementation ("io.github.openfeign.querydsl:querydsl-jpa:7.1")
//    kapt ("io.github.openfeign.querydsl:querydsl-apt:7.1:jpa")

    // ✅ Lombok: Java용 (annotationProcessor) + Kotlin용 (kapt) 모두 등록
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    kapt("org.projectlombok:lombok")

    // 테스트용도 동일하게 적용
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
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