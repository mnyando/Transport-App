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
# Firestore maps documents to these classes by reflection (toObject / set(pojo)),
# so their no-arg constructors, fields and getters/setters must survive R8.
-keep class com.example.transportapp.Driver { *; }
-keep class com.example.transportapp.Notification { *; }
-keep class com.example.transportapp.Route { *; }
-keep class com.example.transportapp.Staff { *; }
-keep class com.example.transportapp.Student { *; }
-keep class com.example.transportapp.Vehicle { *; }

# Keep line numbers so release crash reports are readable.
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*
