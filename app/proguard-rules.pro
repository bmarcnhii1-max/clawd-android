# Keep entry points & everything Parcelable/serializable by reflection
-keep class com.clawd.pet.** { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }
-keep class * extends android.content.BroadcastReceiver { *; }
-keep class * extends android.appwidget.AppWidgetProvider { *; }

# Inner lambdas / anonymous classes used with reflection
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes *Annotation*
