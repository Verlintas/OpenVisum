# libVLC
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.**

# jcifs-ng (SMB)
-dontwarn jcifs.**
-keep class jcifs.** { *; }

# jUPnP
-dontwarn org.jupnp.**
-keep class org.jupnp.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
