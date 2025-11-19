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

/**
 * Data class para deserializar la respuesta de nuestra API /api/upload.
 * Contiene la URL final y la URL temporal de subida.
 */
@Serializable
data class VercelBlobResponse(
    val url: String, // La URL final y permanente del archivo (ej: .../files/qr-yape.png)
    val uploadUrl: String, // La URL temporal de un solo uso para hacer el PUT
    val pathname: String,
    val contentType: String? = null,
    val contentDisposition: String
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
        // La URL de tu API de backend (la que creaste en Node.js)
        val apiUrl = "$BASE_URL/api/upload"
        println(" KMP -> Vercel API: Pidiendo permiso para subir ${fileData.fileName}")

        try {
            // --- PASO 1: Pedir la URL de subida (POST a /api/upload) ---
            // Creamos el cuerpo JSON simple que espera nuestra API: {"fileName": "..."}
            val requestBody = mapOf("fileName" to fileData.fileName)

            // CORRECCIÓN CLAVE: Usamos .post().body<VercelBlobResponse>() directamente
            val blobResponse: VercelBlobResponse = httpClient.post(apiUrl) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body() // <--- El compilador ahora sabe que T es VercelBlobResponse

            // No necesitamos revisar el estado HTTP aquí si usamos .body(),
            // ya que Ktor lanza una excepción (ClientRequestException) si el estado no es exitoso (4xx o 5xx).

            // El código original hacía esto:
            // val responseBodyText = httpResponse.bodyAsText()
            // if (!httpResponse.status.isSuccess()) { ... }
            // val blobResponse: VercelBlobResponse = jsonParser.decodeFromString(responseBodyText)

            println(" KMP <- Vercel API: Permiso OK. Deserializando respuesta...")
            println(" KMP -> Vercel Blob: Subiendo ${fileData.bytes.size} bytes a ${blobResponse.uploadUrl.take(70)}...")

            // --- PASO 2: Subir los bytes (PUT a blobResponse.uploadUrl) ---
            val uploadHttpResponse: HttpResponse = httpClient.put(blobResponse.uploadUrl) {
                setBody(fileData.bytes) // Enviamos los bytes crudos

                // Ayudamos a Vercel Blob dándole el tipo de contenido
                contentType(determineContentType(fileData.fileName))
            }

            if (!uploadHttpResponse.status.isSuccess()) {
                val uploadErrorText = uploadHttpResponse.bodyAsText()
                println(" KMP <- Vercel Blob: Error en la subida PUT (${uploadHttpResponse.status}): $uploadErrorText")
                return Result.failure(Exception("Error final al subir el archivo a Vercel Blob: ${uploadHttpResponse.status}"))
            }

            println(" KMP <- Vercel Blob: Subida exitosa.")

            // --- ÉXITO ---
            // Devolvemos la URL *permanente* (blobResponse.url)
            return Result.success(blobResponse.url)

            // ...
        } catch (e: JsonConvertException) {
            println(" KMP x Vercel: ERROR de Ktor al deserializar: ${e.message}")
            e.printStackTrace()
            // Intentamos obtener el JSON que causó el error para dar más contexto
            val problematicJson = e.message?.substringAfter("JSON input: ") ?: "(No se pudo extraer el JSON del error)"
            println(" KMP x Vercel: JSON recibido que causó el error de deserialización: $problematicJson")
            // SOLUCIÓN: Añadir "return" y usar la función correcta "failure"
            return Result.failure(Exception("Error al interpretar la respuesta del servidor (formato inesperado). JSON problemático: $problematicJson", e))
        } catch (e: ClientRequestException) {
            println(" KMP x Vercel: ERROR de Ktor (ClientRequestException): ${e.message}")
            val errorBody = e.response.bodyAsText()
            println(" KMP x Vercel: Cuerpo del error: $errorBody")
            e.printStackTrace()
            // SOLUCIÓN: Añadir "return" también aquí
            return Result.failure(Exception("Error en la solicitud de red: ${e.response.status}. Detalle: ${parseErrorMessage(errorBody, e.response.status.value)}", e))
        }
// ...
        catch (e: Exception) {
            println(" KMP x Vercel: ERROR general en la llamada Ktor: ${e.message}")
            e.printStackTrace()
            return Result.failure(e)
        }
    }

    /**
     * Intenta parsear un mensaje de error desde una respuesta JSON fallida.
     */
    private fun parseErrorMessage(errorBody: String, statusCode: Int): String {
        return try {
            val jsonElement = jsonParser.parseToJsonElement(errorBody)
            // Intenta extraer el campo 'error'
            jsonElement.jsonObject["error"]?.jsonPrimitive?.content ?: "Error del servidor (código $statusCode)"
        } catch (e: Exception) {
            "Error del servidor (código $statusCode)"
        }
    }

    /**
     * Determina el ContentType de Ktor basado en la extensión del archivo.
     */
    private fun determineContentType(fileName: String): ContentType {
        return when (fileName.substringAfterLast('.').lowercase()) {
            "png" -> ContentType.Image.PNG
            "jpg", "jpeg" -> ContentType.Image.JPEG
            "gif" -> ContentType.Image.GIF
            "pdf" -> ContentType.Application.Pdf
            else -> ContentType.Application.OctetStream // Tipo binario genérico
        }
    }
}