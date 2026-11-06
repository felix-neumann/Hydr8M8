# Keep line numbers for readable crash stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Glance instantiates ActionCallback implementations by reflection (class name + no-arg
# constructor). Without this rule R8 strips the constructor and widget buttons fail silently
# with NoSuchMethodException in release builds.
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    <init>();
}
