package ci.devsphere.civmarketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val price: Double,
    val imageUrl: String? = null,
    val quantity: Int = 1
)

