# ðŸ›¡ï¸ DEVSphere Marketplace - ProGuard/R8 Security Rules

# --- RETROFIT & OKHTTP ---
-keepattributes Signature, InnerClasses, AnnotationDefault
-keepclassmembers class retrofit2.BuiltInConverters$* { *; }
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# --- GSON (API Data Models) ---
-keep class ci.devsphere.civmarketplace.data.model.** { *; }
-keep class ci.devsphere.civmarketplace.data.remote.** { *; } # <-- AJOUTÃ‰ : ProtÃ¨ge aussi les modÃ¨les de requÃªtes
-keepattributes EnclosingMethod, Signature, *Annotation*

# --- HILT / DAGGER ---
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class * extends androidx.lifecycle.ViewModel

# --- PUSHER (Real-time) ---
-keep class com.pusher.client.** { *; }
-dontwarn com.pusher.client.**
-dontwarn org.slf4j.**

# --- ROOM ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# --- ANDROID SYSTEM ---
-keepattributes SourceFile, LineNumberTable
-keep public class * extends android.app.Application
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service

