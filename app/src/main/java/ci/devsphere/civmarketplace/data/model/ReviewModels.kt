package ci.devsphere.civmarketplace.data.model

import com.google.gson.annotations.SerializedName

data class ReviewDto(
    val id: Int,
    val rating: Int,
    val comment: String?,
    @SerializedName(value = "client_name", alternate = ["clientName"])
    val clientName: String,
    @SerializedName(value = "created_at", alternate = ["createdAt"])
    val createdAt: String
)

/** Commande livrée par le client, pas encore notée — sert à proposer "Laisser un avis" au bon endroit. */
data class ReviewableOrderDto(
    @SerializedName(value = "order_id", alternate = ["orderId"])
    val orderId: Int,
    @SerializedName(value = "product_id", alternate = ["productId"])
    val productId: Int,
    @SerializedName(value = "product_name", alternate = ["productName"])
    val productName: String,
    @SerializedName(value = "image_url", alternate = ["imageUrl"])
    val imageUrl: String?
)

data class SubmitReviewRequest(
    @SerializedName("order_id")
    val orderId: Int,
    val rating: Int,
    val comment: String?
)

