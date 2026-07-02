# Keep Retrofit and Gson annotations/types to prevent breaking serialization and API interfaces
-keepattributes Signature, InnerClasses, EnclosingMethod, AnnotationDefault
-keepattributes *Annotation*
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep data models used for JSON parsing
-keep class com.college.campusapp.api.** { *; }

# Keep AndroidX security and Crypto helpers
-keep class androidx.security.crypto.** { *; }

# Proguard rules for Coil image loading library
-keep class coil.** { *; }
-dontwarn coil.**
