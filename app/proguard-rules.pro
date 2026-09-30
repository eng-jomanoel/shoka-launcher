# Proguard rules for Modular Life Tracker Engine
# Optimize for size and strip unused classes
-dontwarn java.lang.invoke.**
-keep class org.luaj.** { *; }
-dontwarn org.luaj.**
