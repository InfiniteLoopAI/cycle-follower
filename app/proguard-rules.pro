# Minification is disabled for release builds (see app/build.gradle.kts).
# Rules kept here so enabling R8 later is a one-line change.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.infiniteloop.cyclefollower.data.** {
    *** Companion;
}
