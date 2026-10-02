package br.com.helldiversbr.app

import br.com.helldiversbr.app.data.SteamNewsRepository
import org.junit.Assert.*
import org.junit.Test

class SteamNewsTest {
    @Test fun directSteamResponseParsesActualShapeAndPreservesPublicationTime() {
        val body = """{"appnews":{"appid":553850,"newsitems":[{"gid":"42","title":"Patch","url":"https://steamstore-a.akamaihd.net/news/externalpost/steam_community_announcements/42","date":1790337661,"feedname":"steam_community_announcements"}]}}"""
        val item=SteamNewsRepository.parse(body).single()
        assertEquals("42",item.id)
        assertEquals(1790337661000L,item.publishedAtMillis)
    }
    @Test fun communityShapeAcceptsIsoDateAndSortsNewestFirst() {
        val body = """[{"id":1,"title":"Older","url":"https://steamcommunity.com/announcements/a","publishedAt":"2026-09-01T00:00:00Z"},{"id":2,"title":"Newer","url":"https://store.steampowered.com/news/app/553850/view/2","date":"2026-09-02T00:00:00Z"}]"""
        val items=SteamNewsRepository.parse(body)
        assertEquals("Newer",items.first().title)
        assertTrue(items.all { it.publishedAtMillis > 0L })
    }
    @Test fun newsFromAnotherGameAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { SteamNewsRepository.parse("""{"appnews":{"appid":730,"newsitems":[]}}""") }
    }
    @Test fun malformedFeedDoesNotBecomeAnEmptySuccessfulFeed() {
        assertThrows(IllegalStateException::class.java) { SteamNewsRepository.parse("{}") }
    }
    @Test fun emptyTitleAndUntrustedLinksAreExcluded() {
        val body="""[{"title":"","url":"https://steamcommunity.com/a"},{"title":"Unknown","url":"https://example.com/a"},{"title":"Script","url":"javascript:alert(1)"}]"""
        assertTrue(SteamNewsRepository.parse(body).isEmpty())
    }
    @Test fun unknownDateRemainsUnknownInsteadOfUsingCurrentTime() {
        val item=SteamNewsRepository.parse("""[{"title":"Patch","url":"https://steamcommunity.com/a"}]""").single()
        assertEquals(0L,item.publishedAtMillis)
    }
    @Test fun duplicateArticlesDoNotOccupyTheThreeSlots() {
        val body="""[{"id":1,"title":"Patch","url":"https://steamcommunity.com/a"},{"id":1,"title":"Patch","url":"https://steamcommunity.com/a"}]"""
        assertEquals(1,SteamNewsRepository.parse(body).size)
    }
}
