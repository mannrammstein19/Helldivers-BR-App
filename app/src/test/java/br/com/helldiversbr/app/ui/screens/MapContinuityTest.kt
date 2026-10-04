package br.com.helldiversbr.app.ui.screens

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class MapContinuityTest {
    @Test fun framesAdvanceForFiveMinutesWithoutAnyTelemetryUpdate() {
        val clock = MapAnimationClock()
        val origin = 8_000_000_000L
        var previous = clock.secondsAt(origin)
        repeat(18_000) { index ->
            val seconds = clock.secondsAt(origin + (index + 1) * 16_666_667L)
            assertTrue(seconds > previous)
            previous = seconds
        }
        assertEquals(300f, previous, .001f)
    }

    @Test fun telemetryChangesDoNotRestartMapTime() {
        val clock = MapAnimationClock()
        val origin = 12_000_000_000L
        clock.secondsAt(origin)
        listOf("cache", "community", "direct", "cache", "mixed").forEachIndexed { index, source ->
            mapReadingSource(source)
            assertEquals((index + 1).toFloat(), clock.secondsAt(origin + (index + 1) * 1_000_000_000L), 0f)
        }
    }

    @Test fun resumingOrAnEarlierFrameNeverRewindsTime() {
        val clock = MapAnimationClock()
        clock.secondsAt(1_000_000_000L)
        assertEquals(2f, clock.secondsAt(3_000_000_000L), 0f)
        assertEquals(2f, clock.secondsAt(2_000_000_000L), 0f)
        assertEquals(62f, clock.secondsAt(63_000_000_000L), 0f)
    }

    @Test fun readingTimeUsesLocalZoneAndKeepsTheOriginalTimestamp() {
        val reading = Instant.parse("2026-10-04T20:34:47Z").toEpochMilli()
        assertEquals("04/10/2026 às 17:34:47", mapReadingTime(reading, ZoneId.of("America/Sao_Paulo")))
        assertEquals("04/10/2026 às 20:34:47", mapReadingTime(reading, ZoneId.of("UTC")))
        assertEquals("Sem leitura disponível", mapReadingTime(null))
        assertEquals("Sem leitura disponível", mapReadingTime(0L))
    }

    @Test fun sourcesHaveReadableNamesAndCacheRemainsIdentified() {
        assertEquals("API da comunidade", mapReadingSource("community"))
        assertEquals("API direta do jogo", mapReadingSource("direct"))
        assertEquals("Fontes combinadas", mapReadingSource("mixed"))
        assertEquals("Última leitura salva", mapReadingSource("cache"))
        assertEquals("Fonte indisponível", mapReadingSource("unexpected"))
    }
}
