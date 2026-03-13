package eu.darken.myperm.common.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE permission_change_reports ADD COLUMN previousVersionCode INTEGER")
        db.execSQL("ALTER TABLE permission_change_reports ADD COLUMN previousVersionName TEXT")
    }
}
