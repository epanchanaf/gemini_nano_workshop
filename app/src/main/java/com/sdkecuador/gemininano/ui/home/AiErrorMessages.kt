package com.sdkecuador.gemininano.ui.home

import com.sdkecuador.gemininano.data.genai.model.AiException
import com.sdkecuador.gemininano.data.genai.model.InputProblem
import com.sdkecuador.gemininano.data.genai.model.ModelUnavailableReason

/** Los mensajes de presentación dependen del contrato de datos, no del SDK ni de su message. */
internal fun AiException.toUserMessage(): String = when (this) {
    is AiException.InvalidInput -> when (problem) {
        InputProblem.EMPTY_TEXT -> "El texto está vacío."
        InputProblem.TEXT_TOO_SHORT -> if (minimumCharacters > 0) {
            val missing = minimumCharacters - actualCharacters
            val unit = if (missing == 1) "carácter" else "caracteres"
            "El modo Artículo requiere al menos $minimumCharacters caracteres. " +
                "Agrega $missing $unit más."
        } else {
            "El texto es demasiado corto para esta función."
        }
        InputProblem.TEXT_TOO_LONG -> if (maximumTokens > 0) {
            "El límite es $maximumTokens tokens y el texto tiene $actualTokens. Reduce su longitud."
        } else {
            "El texto supera el tamaño admitido por el modelo. Reduce su longitud."
        }
        InputProblem.INVALID_IMAGE -> "La imagen supera el límite admitido. Selecciona otra imagen."
    }
    is AiException.ModelUnavailable -> when (reason) {
        ModelUnavailableReason.UNSUPPORTED -> "Esta función no es compatible con el dispositivo."
        ModelUnavailableReason.NOT_DOWNLOADED -> "El modelo aún no está descargado. Vuelve a intentarlo para descargarlo."
        ModelUnavailableReason.DOWNLOADING -> "El modelo se está descargando. Inténtalo de nuevo en unos momentos."
        ModelUnavailableReason.NEEDS_SYSTEM_UPDATE -> "El modelo requiere una actualización del sistema o de AICore."
        ModelUnavailableReason.NOT_READY -> "El modelo no está disponible en este momento. Inténtalo de nuevo."
    }
    is AiException.DownloadFailed -> "No se pudo descargar el modelo. Revisa la conexión y el espacio disponible, e inténtalo de nuevo."
    is AiException.EmptyResult -> "El modelo no devolvió un resultado. Prueba con otra entrada."
    is AiException.StatusCheckFailed -> "No se pudo consultar el estado del modelo. Inténtalo de nuevo."
    is AiException.GenerationFailed -> "No se pudo generar el resultado. Inténtalo de nuevo."
}
