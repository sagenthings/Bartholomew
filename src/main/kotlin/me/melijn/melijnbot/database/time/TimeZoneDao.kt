package me.melijn.melijnbot.database.time

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TimeZoneDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "timeZones"
    override val tableStructure: String = "id bigint, zoneId varchar(64)"
    override val primaryKey: String = "id"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun put(id: Long, zoneId: String) {
        driverManager.executeUpdate("INSERT INTO $table (id, zoneId) VALUES (?, ?) ON CONFLICT (id) DO UPDATE SET zoneId = ?",
            id, zoneId, zoneId)
    }

    suspend fun remove(id: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE id = ?", id)
    }

    suspend fun getZoneId(id: Long): String = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE id = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("zoneId"))
            } else {
                it.resume("")
            }              
        }, id)
    }
}