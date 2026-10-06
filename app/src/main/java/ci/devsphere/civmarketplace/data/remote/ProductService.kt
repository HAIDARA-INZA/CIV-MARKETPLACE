package ci.devsphere.civmarketplace.data.remote

import ci.devsphere.civmarketplace.data.model.CategoryDto
import ci.devsphere.civmarketplace.data.model.FavoriteResponse
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.data.model.PromotionDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface ProductService {
    @GET("products")
    suspend fun getProducts(@Query("q") query: String? = null): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProductDetails(@Path("id") id: Int): ProductDto

    @GET("categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("promotions/active")
    suspend fun getActivePromotions(): List<PromotionDto>

    @GET("favorites")
    suspend fun getFavorites(): List<ProductDto>

    @GET("seller/products")
    suspend fun getSellerProducts(): List<ProductDto>

    @POST("products/{id}/favorite")
    suspend fun addFavorite(@Path("id") id: Int): FavoriteResponse

    @DELETE("products/{id}/favorite")
    suspend fun removeFavorite(@Path("id") id: Int): FavoriteResponse

    @Multipart
    @POST("products")
    suspend fun createProduct(
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("price") price: RequestBody,
        @Part("stock") stock: RequestBody,
        @Part("category") category: RequestBody,
        @Part("type") type: RequestBody,
        @Part image: MultipartBody.Part
    ): ProductDto

    @Multipart
    @POST("products/{id}/update")
    suspend fun updateProduct(
        @Path("id") id: Int,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("price") price: RequestBody,
        @Part("stock") stock: RequestBody,
        @Part("category") category: RequestBody,
        @Part("type") type: RequestBody,
        @Part image: MultipartBody.Part? = null
    ): ProductDto

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: Int)
}

