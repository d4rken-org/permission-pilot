package eu.darken.myperm.common

enum class AndroidVersionCodes(
    val label: String,
    val versionName: String,
    val apiLevel: Int,
) {

    Android13("Android 13", "13", 33),
    Android12L("Android 12L", "12", 32),
    Android12("Android 12", "12", 31),
    Android11("Android 11", "11", 30),
    Android10("Android 10", "10", 29),
    Pie("Pie", "9", 28),
    Oreo2("Oreo", "8.1.0", 27),
    Oreo1("Oreo", "8.0.0", 26),
    Nougat2("Nougat", "7.1", 25),
    Nougat1("Nougat", "7.0", 24),
    Marshmallow("Marshmallow", "6.0", 23),
    Lollipop2("Lollipop", "5.1", 22),
    Lollipop1("Lollipop", "5.0", 21),
    KitKat("KitKat", "4.4.x", 19),
    Jelly3("Jelly Bean", "4.3.x", 18),
    Jelly2("Jelly Bean", "4.2.x", 17),
    Jelly1("Jelly Bean", "4.1.x", 16),
    Ice2("Ice Cream Sandwich", "4.0.3 - 4.0.4", 15),
    Ice1("Ice Cream Sandwich", "4.0.1 - 4.0.2", 14),
    Honeycomb3("Honeycomb", "3.2.x", 13),
    Honeycomb2("Honeycomb", "3.1", 12),
    Honeycomb1("Honeycomb", "3.0", 11),
    Gingerbread2("Gingerbread", "2.3.3 - 2.3.7", 10),
    Gingerbread1("Gingerbread", "2.3 - 2.3.2", 9),

    ;

    val longFormat: String = "$label (${versionName}) [API $apiLevel]"

    companion object {
        val current = values().singleOrNull { it.apiLevel == BuildWrap.VersionWrap.SDK_INT }
    }
}