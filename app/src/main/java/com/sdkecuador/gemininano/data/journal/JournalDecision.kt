package com.sdkecuador.gemininano.data.journal

import com.google.mlkit.genai.schema.annotations.Generable
import com.google.mlkit.genai.schema.annotations.Guide

/** Salida pequeña y tipada: la app decide qué operaciones permite ejecutar. */
@Generable
data class JournalDecision(
    @Guide(description = "Acción solicitada", enumValues = ["CREATE", "DELETE", "THEME"])
    var action: String = "CREATE",
    @Guide(description = "Título breve en español para una entrada nueva")
    var title: String = "",
    @Guide(description = "Entrada de diario en primera persona, fiel al dictado del usuario")
    var body: String = "",
    @Guide(description = "Emoción o tema dominante", enumValues = ["CANSADO", "ALEGRE", "APRENDIZAJE", "TRANQUILO", "TRISTE", "NEUTRO"])
    var mood: String = "NEUTRO",
    @Guide(description = "Número de la entrada a eliminar; cero si no aplica", minimum = 0.0, maximum = 20.0)
    var targetIndex: Int = 0,
    @Guide(description = "Paleta solicitada", enumValues = ["DEFAULT", "FOREST", "DOMINICAN_REPUBLIC", "RAIN", "SUNSET", "OCEAN", "LAVENDER"])
    var palette: String = "DEFAULT"
)
