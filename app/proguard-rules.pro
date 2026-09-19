# Proguard rules for Android Thermal Printer
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.thermalprinter.app.domain.model.** { *; }
-keep class com.thermalprinter.app.data.** { *; }
