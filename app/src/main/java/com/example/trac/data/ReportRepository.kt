package com.example.trac.data

import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminNotificationEntity(
    val id: String? = null,
    @SerialName("report_id") val reportId: String? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("is_urgent") val isUrgent: Boolean = false,
    @SerialName("is_read") val isRead: Boolean = true
)

class ReportRepository {
    private val postgrest = SupabaseClientManager.client.postgrest

    // ==========================================
    // 1. REPORTS CRUD
    // ==========================================
    suspend fun getReports(): Result<List<ReportData>> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["reports"]
                .select()
                .decodeList<ReportData>()
        }
    }

    suspend fun createReport(report: ReportData): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["reports"].insert(report)
            Unit
        }
    }

    suspend fun updateReportStatus(
        reportId: String,
        newStatus: String,
        completionImageUrl: String? = null,
        completionNotes: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["reports"].update({
                set("status", newStatus)
                if (completionImageUrl != null) {
                    set("completion_image_url", completionImageUrl)
                }
                if (completionNotes != null) {
                    set("completion_notes", completionNotes)
                }
            }) {
                filter {
                    eq("id", reportId)
                }
            }
            Unit
        }
    }

    suspend fun upvoteReport(reportId: String, newCount: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["reports"].update({
                set("upvote_count", newCount)
            }) {
                filter {
                    eq("id", reportId)
                }
            }
            Unit
        }
    }

    // ==========================================
    // 2. STAFF MEMBERS SUPABASE CRUD
    // ==========================================
    suspend fun getStaffList(): Result<List<StaffMember>> = withContext(Dispatchers.IO) {
        runCatching {
            val list = postgrest["staff_members"]
                .select()
                .decodeList<StaffMember>()
            list.sortedBy { it.id }
        }
    }

    suspend fun addStaff(staff: StaffMember): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["staff_members"].upsert(staff)
            Unit
        }
    }

    suspend fun deleteStaff(staffId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["staff_members"].delete {
                filter {
                    eq("id", staffId)
                }
            }
            Unit
        }
    }

    suspend fun updateStaffAvailability(staffId: String, isAvailable: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["staff_members"].update({
                set("is_available", isAvailable)
            }) {
                filter {
                    eq("id", staffId)
                }
            }
            Unit
        }
    }

    suspend fun updateStaffActiveTasks(staffId: String, activeTasks: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["staff_members"].update({
                set("active_tasks", activeTasks)
            }) {
                filter {
                    eq("id", staffId)
                }
            }
            Unit
        }
    }

    // ==========================================
    // 3. FACILITY LOCATIONS SUPABASE CRUD
    // ==========================================
    suspend fun getFacilityLocations(): Result<List<FacilityLocation>> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = postgrest["facility_locations"]
                .select {
                    filter {
                        eq("is_active", true)
                    }
                }
                .decodeList<FacilityLocationRow>()

            if (rows.isEmpty()) {
                SchoolFacilityMasterData.defaultLocations
            } else {
                val floorOrder = listOf("LANTAI 1", "LANTAI 2", "LANTAI 3", "LANTAI 4")
                val grouped = rows.groupBy { it.floorName.trim().uppercase() }
                val allFloorKeys = (floorOrder + grouped.keys).distinct().filter { grouped.containsKey(it) }

                allFloorKeys.map { floorKey ->
                    val floorRows = grouped[floorKey].orEmpty()
                    val desc = floorRows.firstOrNull { !it.floorDesc.isNullOrBlank() }?.floorDesc
                        ?: SchoolFacilityMasterData.defaultLocations.find { it.floorName.equals(floorKey, ignoreCase = true) }?.floorDesc
                        ?: "Fasilitas $floorKey"
                    val rooms = floorRows.map { it.roomName.trim() }.filter { it.isNotBlank() }.distinct()
                    FacilityLocation(
                        floorName = floorKey,
                        floorDesc = desc,
                        roomCount = rooms.size,
                        activeReportsCount = 0,
                        rooms = rooms
                    )
                }
            }
        }
    }

    suspend fun addRoomToFloor(floorName: String, roomName: String, floorDesc: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entry = FacilityLocationRow(
                floorName = floorName.trim().uppercase(),
                floorDesc = floorDesc.ifBlank { "Fasilitas ${floorName.trim().uppercase()}" },
                roomName = roomName.trim(),
                isActive = true
            )
            postgrest["facility_locations"].upsert(entry)
            Unit
        }
    }

    suspend fun deleteRoomFromFloor(floorName: String, roomName: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            postgrest["facility_locations"].delete {
                filter {
                    eq("floor_name", floorName.trim().uppercase())
                    eq("room_name", roomName.trim())
                }
            }
            Unit
        }
    }

    // ==========================================
    // 4. NOTIFICATIONS PERSISTENT READ STATUS
    // ==========================================
    suspend fun getReadNotificationIds(): Result<Set<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = postgrest["admin_notifications"]
                .select {
                    filter {
                        eq("is_read", true)
                    }
                }
                .decodeList<AdminNotificationEntity>()
            rows.mapNotNull { it.reportId?.trim() }.filter { it.isNotBlank() }.toSet()
        }
    }

    suspend fun markNotificationAsReadInDb(notifId: String, title: String = "Notification", desc: String = ""): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanId = notifId.trim()
            if (cleanId.isNotBlank()) {
                postgrest["admin_notifications"].insert(
                    AdminNotificationEntity(
                        reportId = cleanId,
                        title = title,
                        description = desc,
                        isUrgent = false,
                        isRead = true
                    )
                )
            }
            Unit
        }
    }

    suspend fun markAllNotificationsAsReadInDb(notifIds: Collection<String>): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val validIds = notifIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
            if (validIds.isNotEmpty()) {
                val entities = validIds.map { id ->
                    AdminNotificationEntity(
                        reportId = id,
                        title = "Read Notification",
                        description = id,
                        isUrgent = false,
                        isRead = true
                    )
                }
                postgrest["admin_notifications"].insert(entities)
            }
            Unit
        }
    }
}
