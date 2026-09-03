# Nothing Modes — ProGuard/R8 Rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.tdvorak.nothingmodes.**$$serializer { *; }
-keepclassmembers class com.tdvorak.nothingmodes.** {
    *** Companion;
}
-keepclasseswithmembers class com.tdvorak.nothingmodes.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Shizuku
-keep class rikka.shizuku.** { *; }
-keep class com.tdvorak.nothingmodes.shizuku.** { *; }

# Nothing Glyph SDK
-keep class com.nothing.ketchum.** { *; }
-keep class com.tdvorak.nothingmodes.nothing.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# Keep model classes for serialization
-keep @kotlinx.serialization.Serializable class com.tdvorak.nothingmodes.** { *; }
