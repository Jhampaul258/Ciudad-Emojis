package domain.model
import kotlinx.serialization.Serializable

@Serializable
data class Director(
    val uid: String = "", // ID de Firebase Auth
    // Información Básica
    val name: String = "",
    val fotoPerfilUrl: String = "",

    val ciudadOrigen: String = "",
    val universidad: String = "",
    val carrera: String = "",
    // Presentación del director
    val biografia: String = "",
    val inspiraciones: String = "",
    val lema: String = "",
    // Reconocimientos
    val festivales: List<String> = emptyList(),
    val premios: List<String> = emptyList(),
    // Presencia Digital
    val canalYoutube: String = "",
    val email: String = "",
    val redesSociales: Map<String, String> = emptyMap(), // Ej: {"instagram": "url", "linkedin": "url"}
    // Apoyo
    val yapeQrUrl: String = "",
    val yapeNumero: String = "",
    val plinNumero: String = "",
)