buildscript {
    repositories {
        gradleKotlinDsl()
    }
    dependencies {
        classpath("net.idlestate:gradle-download-dependencies-plugin:1.+")
    }
}

repositories {
    mavenCentral()
}

group = "com.promethistai.datastore"
version = "1.0.0-SNAPSHOT"

plugins {
    application
	kotlin("jvm") version "1.2.71"
}

application {
    mainClassName = "com.promethistai.datastore.server.Server"

}

distributions {

}

tasks.getByName<Zip>("distZip").enabled = false
tasks.getByName<Tar>("distTar").archiveName = "${project.name}.tar"

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("org.glassfish.jersey.core:jersey-server:2.25.1")
    compile("org.glassfish.jersey.containers:jersey-container-netty-http:2.25.1")
    compile("org.glassfish.jersey.media:jersey-media-json-jackson:2.25.1")
    compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.8.4")
    compile("javax.xml.bind:jaxb-api:2.2.11")
    compile("com.sun.xml.bind:jaxb-core:2.2.11")
    compile("com.sun.xml.bind:jaxb-impl:2.2.11")
    compile("javax.activation:activation:1.1.1")
    compile("com.google.cloud:google-cloud-datastore:1.79.0")
    runtime("ch.qos.logback:logback-classic:1.1.8")
}