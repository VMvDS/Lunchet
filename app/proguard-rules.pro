# R8 full mode + AGP defaults are sufficient: the app has no reflection on its own classes,
# the AccessibilityService and Activity are kept via manifest references.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
