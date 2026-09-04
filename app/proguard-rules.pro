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

# Shizuku — only the UserService class and AIDL need keeping
-keep class rikka.shizuku.** { *; }
-keep class com.tdvorak.nothingmodes.shizuku.PrivilegedShellUserService { *; }
-keep class com.tdvorak.nothingmodes.shizuku.IPrivilegedShellService { *; }
-keep class com.tdvorak.nothingmodes.shizuku.PrivilegedShell { *; }
-keep class com.tdvorak.nothingmodes.shizuku.PrivilegedShellFactory { *; }
-keep class com.tdvorak.nothingmodes.shizuku.ShizukuGateway { *; }

# Nothing Glyph SDK
-keep class com.nothing.ketchum.** { *; }
-keep class com.tdvorak.nothingmodes.nothing.** { *; }

# Keep model classes for serialization (serializers + constructors only)
-keep,includedescriptorclasses @kotlinx.serialization.Serializable class com.tdvorak.nothingmodes.** { *; }
