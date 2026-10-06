package ci.devsphere.civmarketplace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CartEntity::class,
        PendingActionEntity::class,
        ConversationEntity::class,
        MessageEntity::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun pendingActionDao(): PendingActionDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
}

