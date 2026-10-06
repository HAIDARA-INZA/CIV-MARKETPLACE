package ci.devsphere.civmarketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val payloadJson: String,
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

