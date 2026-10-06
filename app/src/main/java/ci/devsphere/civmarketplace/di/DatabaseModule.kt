package ci.devsphere.civmarketplace.di

import android.content.Context
import androidx.room.Room
import ci.devsphere.civmarketplace.data.local.AppDatabase
import ci.devsphere.civmarketplace.data.local.CartDao
import ci.devsphere.civmarketplace.data.local.ConversationDao
import ci.devsphere.civmarketplace.data.local.MessageDao
import ci.devsphere.civmarketplace.data.local.PendingActionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS messages (
                    localKey TEXT NOT NULL,
                    id INTEGER NOT NULL,
                    conversationUserId INTEGER NOT NULL,
                    senderId INTEGER NOT NULL,
                    receiverId INTEGER NOT NULL,
                    clientMessageId TEXT NOT NULL,
                    message TEXT,
                    timestamp TEXT NOT NULL,
                    isRead INTEGER NOT NULL,
                    status TEXT,
                    deliveredAt TEXT,
                    readAt TEXT,
                    attachmentUrl TEXT,
                    attachmentType TEXT,
                    attachmentName TEXT,
                    PRIMARY KEY(localKey)
                )
                """.trimIndent()
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "devsphere_db"
        )
            .addMigrations(migration3To4)
            .fallbackToDestructiveMigration() // Évite les crashs si on change la structure du panier
            .build()
    }

    @Provides
    @Singleton
    fun provideCartDao(database: AppDatabase): CartDao {
        return database.cartDao()
    }

    @Provides
    @Singleton
    fun providePendingActionDao(database: AppDatabase): PendingActionDao {
        return database.pendingActionDao()
    }

    @Provides
    @Singleton
    fun provideConversationDao(database: AppDatabase): ConversationDao {
        return database.conversationDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: AppDatabase): MessageDao {
        return database.messageDao()
    }
}

