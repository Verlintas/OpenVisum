# libVLC
-keep class org.videolan.libvlc.** { *; }
-dontwarn org.videolan.**

# jcifs-ng (SMB)
-dontwarn jcifs.**
-keep class jcifs.** { *; }

# jUPnP
-dontwarn org.jupnp.**
-keep class org.jupnp.** { *; }

# Jetty (used by the DLNA receiver's HTTP server/client)
-dontwarn org.eclipse.jetty.**
-keep class org.eclipse.jetty.** { *; }
-dontwarn javax.servlet.**
-keep class javax.servlet.** { *; }
-dontwarn org.slf4j.**
-keep class org.slf4j.** { *; }
-keep class org.jupnp.internal.compat.java.beans.** { *; }

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
