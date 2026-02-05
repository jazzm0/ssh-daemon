-verbose
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclasseswithmembers class * {
    native <methods>;
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepclassmembers class * {
    public void onClick(android.view.View);
}

-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

-keep class androidx.work.** { *; }
-keep class androidx.lifecycle.** { *; }

-keep class **NavDirections { *; }
-keep class **Args { *; }

-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }

-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.** { *; }

-keep class com.android.org.conscrypt.** { *; }
-keep class org.apache.harmony.xnet.provider.** { *; }

-keep,allowshrinking class javax.** { *; }
-keep,allowshrinking class org.** { *; }


-dontwarn javax.management.**
-dontwarn javax.annotation.**
-dontwarn java.lang.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.commons.logging.**
-dontwarn org.slf4j.**
-dontwarn org.json.**
-dontwarn java.rmi.**
-dontwarn javax.lang.**
-dontwarn javax.naming.**
-dontwarn javax.security.auth.**
-dontwarn org.apache.tomcat.jni.**
-dontwarn org.ietf.jgss.**
-dontwarn org.hamcrest.**
-dontwarn org.junit.**
-dontwarn org.opentest4j.**
-dontwarn org.w3c.dom.bootstrap.**
-dontwarn edu.umd.cs.findbugs.**
-dontwarn com.android.org.conscrypt.**
-dontwarn org.apache.harmony.xnet.provider.**

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class org.bouncycastle.** { *; }
