package utils

fun translateError(originalMessage: String?): String {
    if (originalMessage.isNullOrBlank()) return "Ha ocurrido un error desconocido"

    val message = originalMessage.lowercase()

    return when {
        // Errores típicos de email
        "email address is already in use" in message -> "Ya existe un usuario con este correo"
        "already exists" in message -> "Ya existe un usuario con este correo"
        "email" in message && "invalid" in message -> "El correo electrónico no es válido"
        "email" in message && "badly formatted" in message -> "El correo electrónico tiene un formato incorrecto"

        // Errores típicos de llenado de datos
        "is empty or null" in message -> "Completa todos los campos"
        "credential" in message && ("expired" in message || "invalid" in message || "malformed" in message) ->
            "Tu sesión ha caducado o las credenciales no son válidas. Intenta iniciar sesión nuevamente"

        // Errores de red y conexión
        "network" in message -> "Error de conexión a internet"
        "timeout" in message -> "Tiempo de espera agotado"
        "unreachable" in message -> "Servidor no disponible"

        // Errores de autenticación
        "permission denied" in message -> "No tienes permisos para realizar esta acción"
        "unauthorized" in message -> "Acceso no autorizado"
        "invalid credentials" in message -> "Credenciales inválidas"
        "password" in message && "invalid" in message -> "La contraseña no es válida"

        // Errores de base de datos
        "not found" in message -> "El recurso solicitado no existe"
        "document" in message && "missing" in message -> "El documento no fue encontrado"
        "collection" in message && "not found" in message -> "La colección no existe"

        // Errores genéricos
        "invalid" in message -> "Datos inválidos"
        "failed" in message -> "La operación ha fallado"
        "exception" in message -> "Ocurrió una excepción inesperada"

        else -> originalMessage
    }
}
