package data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
//import io.ktor.client.plugins.serialization.JsonConvertException
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import utils.FileData
import utils.BASE_URL // <--- SOLUCIÓN: Importando la constante BASE_URL
import utils.encodeBase64

/**
 * Data class para deserializar la respuesta de nuestra API /api/upload.
 * Contiene la URL final y la URL temporal de subida.
 */
@Serializable
data class VercelBlobResponse(
    val url: String,
    // val uploadUrl: String, <--- ELIMINA ESTA LÍNEA
    val pathname: String,
    val contentType: String? = null,
    val contentDisposition: String? = null // Hazlo nullable por seguridad
)

/**
 * Repositorio para manejar la lógica de subida de archivos a Vercel Blob.
 * Sigue el patrón de 2 pasos (POST para permiso, PUT para subir).
 */
class BlobUploadRepository(
    private val httpClient: HttpClient,
    private val jsonParser: Json
) {
    /**
     * Sube un archivo (imagen, etc.) a Vercel Blob.
     * @param fileData El objeto que contiene los 'bytes' y el 'fileName' del archivo.
     * @return Un Result<String> que, si es exitoso, contiene la URL permanente y pública del archivo.
     */
    suspend fun uploadFile(fileData: FileData): Result<String> {
        val apiUrl = "$BASE_URL/api/upload"

        return try {
            println(" KMP -> Backend: Codificando imagen a Base64...")

            // 1. Usamos TU función de extensión para convertir los bytes a String Base64
            val base64String = fileData.bytes.encodeBase64()

            // 2. Creamos el JSON con el nombre y el contenido
            val requestBody = mapOf(
                "fileName" to fileData.fileName,
                "fileBase64" to base64String
            )

            println(" KMP -> Backend: Enviando imagen a $apiUrl...")

            // 3. Hacemos un único POST con todo
            val response = httpClient.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                // Ktor deserializa automáticamente a VercelBlobResponse
                val blobResponse: VercelBlobResponse = response.body()
                println(" KMP <- Backend: ¡Subida exitosa! URL: ${blobResponse.url}")
                Result.success(blobResponse.url)
            } else {
                val errorBody = response.body<String>() // Leemos como texto para ver el error
                println(" KMP x Backend: Error ${response.status}: $errorBody")
                Result.failure(Exception("Error del servidor: ${response.status}"))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}