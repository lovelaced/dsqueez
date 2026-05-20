# Keep JNI entry points
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep companion class loading our native lib
-keep class app.dsqueez.native.** { *; }
