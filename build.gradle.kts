plugins {
    kotlin("jvm") version "1.9.20"
    id("application") // ❗ `application` ni to‘g‘ri qo‘shish
    id("com.github.johnrengelman.shadow") version "8.1.1" 
}

group = "uz.ibrohim.food"
// version = "1.0-SNAPSHOT" // eski
version = "1.0.1-TEST"    // yangi

repositories {
    google()
    mavenCentral()
    maven {  setUrl("https://jitpack.io") }
}

dependencies {
    testImplementation(kotlin("test"))

    // Firebase SDK for Admin (JVM)
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // Firebase SDK for Admin (JVM)
    implementation("org.slf4j:slf4j-nop:2.0.0")

    // Telegram bot lib
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.6.4")

    //QR Code
    implementation("com.google.zxing:core:3.5.1")
    implementation("com.google.zxing:javase:3.5.1")
    implementation(kotlin("reflect"))

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
}

application {  // ✅ To‘g‘ri ishlatish
    mainClass.set("uz.ibrohim.food.HomeKt")
}

tasks.test {
    useJUnitPlatform()
}


kotlin {
    jvmToolchain(17)
}
