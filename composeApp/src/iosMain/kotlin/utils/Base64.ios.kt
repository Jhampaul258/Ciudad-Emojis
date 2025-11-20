package utils

actual fun ByteArray.encodeBase64(): String {
    TODO("Not yet implemented")
}
//package utils
//import platform.Foundation.*
//
//actual fun ByteArray.encodeBase64(): String {
//    val data = NSData.dataWithBytes(this.refTo(0), this.size.toULong())
//    return data.base64EncodedStringWithOptions(0.toULong())
//}