package utils
import java.util.Base64

actual fun ByteArray.encodeBase64(): String {
    return Base64.getEncoder().encodeToString(this)
}