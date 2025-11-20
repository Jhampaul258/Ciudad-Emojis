package utils

fun getYoutubeThumbnail(url: String): String {
    if (url.isBlank()) return ""
    // Regex simple para sacar el ID
    val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*"
    val regex = Regex(pattern)
    val id = regex.find(url)?.value

    return if (id != null) {
        "https://img.youtube.com/vi/$id/maxresdefault.jpg"
    } else {
        ""
    }
}