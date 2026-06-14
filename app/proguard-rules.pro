# Add project specific ProGuard rules here.

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers class * extends androidx.room.RoomDatabase { *; }

# MapLibre
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Vico
-keep class com.patrykandpatrick.vico.** { *; }

# Apache Commons CSV
-keep class org.apache.commons.csv.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.iandees.flights.**$$serializer { *; }
-keepclassmembers class com.iandees.flights.** {
    *** Companion;
}
-keepclasseswithmembers class com.iandees.flights.** {
    kotlinx.serialization.KSerializer serializer(...);
}
