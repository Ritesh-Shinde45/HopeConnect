-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

-keep class io.appwrite.** { *; }
-keepclassmembers class io.appwrite.** { *; }

-keep class org.tensorflow.lite.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }

-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_face.** { *; }

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class com.bumptech.glide.GeneratedAppGlideModuleImpl
-keep @com.bumptech.glide.annotation.GlideModule public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** { *; }

-keep class com.journeyapps.** { *; }
-keep class com.google.zxing.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

-keep class com.ritesh.hoppeconnect.models.** { *; }
-keep class com.ritesh.hoppeconnect.data.** { *; }
-keep class com.ritesh.hoppeconnect.databinding.** { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver