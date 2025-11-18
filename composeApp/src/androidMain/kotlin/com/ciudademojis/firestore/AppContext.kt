package com.ciudademojis.firestore

import android.content.Context

object AppContext {
    private var appContext: Context? = null

    /**
     * Inicializa el AppContext. Llama a esto desde tu MainActivity u Application class.
     */
    fun initialize(context: Context) {
        // Usa applicationContext para evitar fugas de memoria
        appContext = context.applicationContext
    }

    /**
     * Obtiene el contexto de la aplicación.
     * Lanza una excepción si no se ha inicializado.
     */
    fun get(): Context {
        return appContext ?: throw IllegalStateException("AppContext no inicializado. Llama a initialize() primero.")
    }
}