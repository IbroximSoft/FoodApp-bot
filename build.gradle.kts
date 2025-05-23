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
tasks.shadowJar {
    archiveBaseName.set("FoodBot")
    archiveClassifier.set("fat")
    archiveVersion.set(project.version.toString()) // Loyiha versiyasini avtomatik oladi
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "uz.ibrohim.food.HomeKt"))
    }
}

tasks.named("jar") {
    enabled = false // Standart jar vazifasini o'chiramiz
}
tasks.named("shadowJar") {
    val shadowTask = this
    tasks.named("assemble") { // Asosiy 'assemble' vazifasini shadowJar'ga bog'laymiz
        dependsOn(shadowTask)
    }
    // Bu classifier'ni olib tashlasak, standart nom bilan fayl yaratadi (masalan, FoodBot-1.0.1-TEST.jar)
    // Agar "-fat" qo'shimchasi bilan kerak bo'lsa, buni qoldiring
    // archiveClassifier.set("fat") 
    // Yoki butunlay olib tashlang, shunda FoodBot-1.0.1-TEST.jar bo'ladi
    archiveClassifier.set("") // ✅ "-fat" qo'shimchasisiz, standart nom bilan yaratish uchun
}


kotlin {
    jvmToolchain(17)
}
