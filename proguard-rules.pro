# Remove all Android Debug Logs in Release Build
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve Security Annotations
-keepattributes *Annotation*,Signature,InnerClasses

# Protect OkHttp, Retrofit, and Gson Data Models from Aggressive Obfuscation
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Preserve Compose UI and Lifecycle classes
-keep class androidx.compose.** { *; }
-keep class com.example.viewmodel.** { *; }
