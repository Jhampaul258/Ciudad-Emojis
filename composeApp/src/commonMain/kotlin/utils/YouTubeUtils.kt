package utils

object YouTubeUtils {
    fun extractVideoId(url: String): String? {
        // Patrones comunes de YouTube
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
        val regex = Regex(pattern)
        val match = regex.find(url)
        return match?.value
    }
}