package com.sdkecuador.gemininano.data.journal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JournalDeleteTargetTest {
    private val tired = JournalEntry(id = 1, spokenText = "cansado", title = "Un día agotador", body = "Cansado", mood = "CANSADO")
    private val learning = JournalEntry(id = 2, spokenText = "aprendí", title = "Aprendizaje nuevo", body = "Aprendí", mood = "APRENDIZAJE")

    @Test fun singleEntryIsUnambiguousWhenNanoOmitsIndex() {
        assertEquals(tired, resolveDeleteTarget(0, "elimina la entrada", listOf(tired)))
    }

    @Test fun spokenTitleMatchesWithoutAccents() {
        assertEquals(tired, resolveDeleteTarget(0, "elimina un dia agotador", listOf(learning, tired)))
    }

    @Test fun ambiguousCommandDoesNotSelectAmongMultipleEntries() {
        assertNull(resolveDeleteTarget(0, "elimina una entrada", listOf(learning, tired)))
    }
}
