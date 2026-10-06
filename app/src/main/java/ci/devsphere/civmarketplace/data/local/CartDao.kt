package ci.devsphere.civmarketplace.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items")
    fun getCartItems(): Flow<List<CartEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToCart(item: CartEntity)

    @Query("UPDATE cart_items SET quantity = quantity + 1 WHERE id = :productId")
    suspend fun incrementQuantity(productId: Int): Int

    @Delete
    suspend fun removeFromCart(item: CartEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}

