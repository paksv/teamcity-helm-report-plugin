package jetbrains.buildServer.helmReport.jsonOutput

import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

class ParsingUtil {
    companion object {
        fun getObjectMapper(): ObjectMapper {
            val kotlinModule = KotlinModule.Builder()
                .configure(KotlinFeature.NullIsSameAsDefault, enabled = true)
                .build()


            return JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(kotlinModule)
                .build()
        }
    }
}