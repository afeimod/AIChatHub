# Add project specific ProGuard rules here.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# Kotlinx Serialization - Critical for R8
-keepattributes *Annotation*, InnerClasses, EnclosingMethod
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion***;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aichathub.**$$serializer { *; }
-keepclassmembers class com.aichathub.** {
    *** Companion***;
}
-keepclasseswithmembers class com.aichathub.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class com.aichathub.data.model.** {
    *** Companion***;
}
-keepclassmembers class com.aichathub.domain.model.** {
    *** Companion***;
}

# Keep Kotlin Metadata for reflection-based operations
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }

# Hilt - Critical for DI reflection
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ComponentSupplier { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Error Prone Annotations - Required by Tink/Security Crypto
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }

# Data classes - Keep all model classes
-keep class com.aichathub.data.model.** { *; }
-keep class com.aichathub.domain.model.** { *; }
-keep class com.aichathub.data.remote.** { *; }

# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Security Crypto / Tink
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep generic type signatures for reflection
-keepattributes Signature
-keep class java.lang.reflect.Type
-keep class java.lang.reflect.ParameterizedType
-keep class java.lang.reflect.GenericArrayType
-keep class java.lang.reflect.WildcardType

# Keep TypeToken-like classes used for serialization
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }