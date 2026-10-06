package ci.devsphere.civmarketplace.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class FlexibleBooleanAdapter : JsonDeserializer<Boolean> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean {
        if (json == null || json.isJsonNull || !json.isJsonPrimitive) return false

        val primitive = json.asJsonPrimitive
        return when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asInt != 0
            primitive.isString -> {
                when (primitive.asString.trim().lowercase()) {
                    "1", "true", "yes", "oui", "on" -> true
                    else -> false
                }
            }
            else -> false
        }
    }
}

