# Play scores obfuscation as part of app optimization (threshold 25%), so this flavor is
# obfuscated. Keep file/line attributes so R8 retrace with the mapping file recovers the
# original frames from Play vitals and user-submitted logs.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Exception class names are shown to users in error dialogs (LocalizedError, ErrorContent).
# Keeps the names and prevents R8 from merging or inlining exception classes; shrinking is
# unaffected.
-keep,allowshrinking class * extends java.lang.Throwable

# ViewModel log tags derive from the simple class name (ViewModel1). Names only.
-keepnames class * extends eu.darken.myperm.common.uix.ViewModel1
