#========================================
# General rules
#========================================
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify
-repackageclasses ''
-printconfiguration 'final-proguard-rules.log'
-printmapping 'mapping.log'

-keepattributes *Annotation*,EnclosingMethod,Signature

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-keepclassmembers,allowoptimization enum * {
   public static **[] values();
   public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
   static final long serialVersionUID;
   private static final java.io.ObjectStreamField[] serialPersistentFields;
   !static !transient <fields>;
   !private <fields>;
   !private <methods>;
   private void writeObject(java.io.ObjectOutputStream);
   private void readObject(java.io.ObjectInputStream);
   java.lang.Object writeReplace();
   java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
}

#========================================
# Rules for Libraries
#========================================
-dontwarn java.awt.geom.AffineTransform
-dontwarn com.squareup.okhttp.**
