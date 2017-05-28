# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/wiktor/Programy/Android/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-dontwarn java.lang.invoke**
-dontwarn sun.misc.Unsafe
-keep class rx.internal.util.unsafe.** { *; }

#Probably a temporary solution. Doco says that configuring ProGuard for Google Play Services is not necessary.
#https://stackoverflow.com/questions/18646899/proguard-cant-find-referenced-class-com-google-android-gms-r
#https://developers.google.com/android/guides/setup#Proguard
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**
-keep public class com.google.firebase.* { public *; }
-dontwarn com.google.firebase.**
 #android.support.v7.widget.FitWindowsLinearLayout
-keep public class android.support.v7.widget.* { public *; }