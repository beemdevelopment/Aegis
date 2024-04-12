-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile
-dontobfuscate

-keepclasseswithmembers public class androidx.recyclerview.widget.RecyclerView { *; }
-keep class com.beemdevelopment.aegis.ui.fragments.preferences.*
-keep class com.beemdevelopment.aegis.importers.** { *; }
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

-dontwarn javax.naming.**
