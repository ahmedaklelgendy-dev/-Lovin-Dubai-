import java.util.Properties
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
     alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

val youtubeApiKey = localProperties.getProperty("YOUTUBE_API_KEY")
    ?: error("YOUTUBE_API_KEY is missing from local.properties")
