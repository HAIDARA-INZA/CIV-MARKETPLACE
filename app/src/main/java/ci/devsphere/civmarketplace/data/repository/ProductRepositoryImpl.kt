package ci.devsphere.civmarketplace.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import ci.devsphere.civmarketplace.data.local.CartDao
import ci.devsphere.civmarketplace.data.local.CartEntity
import ci.devsphere.civmarketplace.data.model.CategoryDto
import ci.devsphere.civmarketplace.data.model.ProductDto
import ci.devsphere.civmarketplace.data.model.PromotionDto
import ci.devsphere.civmarketplace.data.remote.ProductService
import ci.devsphere.civmarketplace.domain.repository.ProductRepository
import ci.devsphere.civmarketplace.util.ApiErrorMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import javax.inject.Inject

class ProductRepositoryImpl @Inject constructor(
    private val productService: ProductService,
    private val cartDao: CartDao,
    @ApplicationContext private val context: Context
) : ProductRepository {

    override suspend fun getProducts(query: String?): Result<List<ProductDto>> {
        return try {
            val products = productService.getProducts(query)
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun getProductDetails(id: Int): Result<ProductDto> {
        return try {
            val product = productService.getProductDetails(id)
            Result.success(product)
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun getCategories(): Result<List<CategoryDto>> {
        return try {
            Result.success(productService.getCategories())
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getActivePromotions(): Result<List<PromotionDto>> {
        return try {
            Result.success(productService.getActivePromotions())
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getFavoriteProducts(): Result<List<ProductDto>> {
        return try {
            Result.success(productService.getFavorites())
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun setFavorite(productId: Int, favorite: Boolean): Result<Boolean> {
        return try {
            val response = if (favorite) {
                productService.addFavorite(productId)
            } else {
                productService.removeFavorite(productId)
            }
            Result.success(response.isFavorite)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    override suspend fun getSellerProducts(): Result<List<ProductDto>> {
        return try {
            val products = productService.getSellerProducts()
            Result.success(products)
        } catch (e: Exception) {
            Result.failure(handleError(e))
        }
    }

    override suspend fun createProduct(
        name: String,
        description: String,
        price: Double,
        stock: Int,
        category: String,
        type: String,
        imageUri: String?
    ): Result<ProductDto> {
        return try {
            if (imageUri == null) return Result.failure(Exception("Image requise"))

            val typeValue = if (type.equals("service", true)) "service" else "product"
            val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
            val descBody = description.toRequestBody("text/plain".toMediaTypeOrNull())
            val priceBody = price.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val stockBody = stock.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val catBody = category.toRequestBody("text/plain".toMediaTypeOrNull())
            val typeBody = typeValue.toRequestBody("text/plain".toMediaTypeOrNull())

            val uri = Uri.parse(imageUri)
            val upload = getFileFromUri(uri) ?: return Result.failure(Exception("Impossible de lire l'image"))

            val requestFile = upload.file.asRequestBody(upload.mimeType.toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", upload.file.name, requestFile)

            val response = productService.createProduct(
                name = nameBody,
                description = descBody,
                price = priceBody,
                stock = stockBody,
                category = catBody,
                type = typeBody,
                image = imagePart
            )
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(ApiErrorMapper.toException(e))
        }
    }

    private data class UploadFile(val file: File, val mimeType: String)

    private fun getFileFromUri(uri: Uri): UploadFile? {
        return try {
            val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
                ?: return null
            val maxDimension = 1920
            val scale = minOf(1f, maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height).toFloat())
            val normalized = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else bitmap
            val file = File(context.cacheDir, "upload_image_${System.currentTimeMillis()}.jpg")
            var quality = 88
            var jpegBytes: ByteArray
            do {
                val buffer = ByteArrayOutputStream()
                normalized.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
                jpegBytes = buffer.toByteArray()
                quality -= 10
            } while (jpegBytes.size > 1_500_000 && quality >= 48)

            if (jpegBytes.size > 1_500_000) {
                if (normalized !== bitmap) bitmap.recycle()
                return null
            }
            FileOutputStream(file).use { outputStream ->
                outputStream.write(jpegBytes)
            }
            if (normalized !== bitmap) bitmap.recycle()
            UploadFile(file, "image/jpeg")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun updateProduct(
        id: Int, name: String, description: String, price: Double, stock: Int,
        category: String, type: String, imageUri: String?
    ): Result<ProductDto> = try {
        val imagePart = imageUri?.let { uriString ->
            getFileFromUri(Uri.parse(uriString))?.let { upload ->
                MultipartBody.Part.createFormData("image", upload.file.name, upload.file.asRequestBody(upload.mimeType.toMediaTypeOrNull()))
            }
        }
        val typeValue = if (type.equals("service", true)) "service" else "product"
        Result.success(productService.updateProduct(
            id,
            name.toRequestBody("text/plain".toMediaTypeOrNull()),
            description.toRequestBody("text/plain".toMediaTypeOrNull()),
            price.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            stock.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
            category.toRequestBody("text/plain".toMediaTypeOrNull()),
            typeValue.toRequestBody("text/plain".toMediaTypeOrNull()),
            imagePart
        ))
    } catch (e: Exception) { Result.failure(handleError(e)) }

    override suspend fun deleteProduct(id: Int): Result<Unit> = try {
        productService.deleteProduct(id)
        Result.success(Unit)
    } catch (e: Exception) { Result.failure(handleError(e)) }

    private fun handleError(e: Exception): Exception {
        if (e is HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody != null) {
                try {
                    val jsonObject = JSONObject(errorBody)
                    val message = jsonObject.optString("message")
                    val errors = jsonObject.optJSONObject("errors")
                    val fieldMessage = errors?.keys()?.asSequence()?.firstOrNull()?.let { key ->
                        errors.optJSONArray(key)?.optString(0)
                    }
                    return Exception(fieldMessage?.takeUnless { it.startsWith("validation.") } ?: message.ifBlank { "La publication a échoué. Vérifiez l'image puis réessayez." })
                } catch (jsonException: Exception) {
                }
            }
        }
        return e
    }

    override fun getCartItems(): Flow<List<CartEntity>> {
        return cartDao.getCartItems()
    }

    override suspend fun addToCart(product: ProductDto) {
        val updatedRows = cartDao.incrementQuantity(product.id)
        if (updatedRows == 0) {
            cartDao.addToCart(
                CartEntity(
                    id = product.id,
                    name = product.name,
                    price = parsePrice(product.price),
                    imageUrl = product.getDisplayImageUrl()
                )
            )
        }
    }

    override suspend fun removeFromCart(item: CartEntity) {
        cartDao.removeFromCart(item)
    }

    private fun parsePrice(value: String): Double {
        val normalized = value
            .replace("FCFA", "", ignoreCase = true)
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace(",", ".")
            .replace(Regex("[^0-9.]"), "")

        val safeDecimal = if (normalized.count { it == '.' } > 1) {
            normalized.replace(".", "")
        } else {
            normalized
        }

        return safeDecimal.toDoubleOrNull() ?: 0.0
    }
}

