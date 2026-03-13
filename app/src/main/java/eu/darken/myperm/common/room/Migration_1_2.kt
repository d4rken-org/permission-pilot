package eu.darken.myperm.common.room

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS `pending_snapshot_events` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `packageName` TEXT NOT NULL,
                `eventType` TEXT NOT NULL,
                `userHandleId` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE INDEX IF NOT EXISTS `index_pending_snapshot_events_packageName_userHandleId`
                ON `pending_snapshot_events` (`packageName`, `userHandleId`)"""
        )
    }
}
