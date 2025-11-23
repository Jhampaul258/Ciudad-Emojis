package domain.model
import kotlinx.serialization.Serializable

@Serializable
data class Pelicula(
    val id: String = "",
    val directorId: String = "", // Para saber quién lo subió
    val directorName: String = "",
    val titulo: String = "",
    val anio: Int = 2024,
    val genero: String = "",
    val sinopsis: String = "",
    val videoUrl: String = "", // Link de YouTube/Vimeo
    val caratulaUrl: String = "", // URL de la imagen de portada
    val esSerie: Boolean = false,
    val nombreSerie: String = "", // Ej: "Stranger Things" (Agrupador)
    val numeroCapitulo: Int = 1   // Ej: 1
) {
}