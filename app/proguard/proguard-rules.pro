-dontobfuscate

# Keep nested objects of sealed classes that use Kotlin reflection (::class.nestedClasses)
# for runtime enumeration. R8 strips them as "unused" since the references are reflection-only.
-keep class eu.darken.myperm.permissions.core.known.APerm$* { *; }
-keep class eu.darken.myperm.permissions.core.known.APermGrp$* { *; }
-keep class eu.darken.myperm.permissions.core.known.AExtraPerm$* { *; }
-keep class eu.darken.myperm.apps.core.known.AKnownPkg$* { *; }