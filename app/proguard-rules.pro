# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-dontshrink
#-dontoptimize
#-dontpreverify
-verbose

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


-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep class com.android.org.conscrypt.** { *; }
-keep class org.apache.harmony.xnet.provider.** { *; }
-keep class javax** { *; }
-keep class org** { *; }

-keepclasseswithmembers class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
