package com.ev.ai.assistant.nlu

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The news regex is the one piece of parsing that can be verified without the network.
 * The sample mirrors the exact structure returned by Google News (a channel title and
 * a channel *image* title both appear before the `<item>` blocks).
 */
class EvWebNewsTest {

    private val sampleRss = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?><rss version="2.0"><channel>
        <generator>NFE/5.0</generator>
        <title>Top stories - Google News</title>
        <link>https://news.google.com/</link>
        <language>en-IN</language>
        <image><title>Google News</title><url>https://example.com/logo.png</url><link>https://news.google.com/</link></image>
        <item><title>Kolkata Durga Puja stampede - Telegraph India</title><link>https://news.google.com/rss/articles/ABC</link><guid isPermaLink="false">ABC</guid></item>
        <item><title><![CDATA[Markets close higher as tech rallies - Reuters]]></title><link>https://news.google.com/rss/articles/DEF</link></item>
        <item><title>Plain headline without cdata</title><link>https://news.google.com/rss/articles/GHI</link></item>
        </channel></rss>
    """.trimIndent()

    @Test
    fun extractsOnlyItemHeadlines() {
        val titles = EvWeb.parseNewsTitles(sampleRss)
        assertEquals(
            listOf(
                "Kolkata Durga Puja stampede - Telegraph India",
                "Markets close higher as tech rallies - Reuters",
                "Plain headline without cdata"
            ),
            titles
        )
    }

    @Test
    fun doesNotIncludeFeedOrImageTitles() {
        val titles = EvWeb.parseNewsTitles(sampleRss)
        assertEquals(false, titles.contains("Top stories - Google News"))
        assertEquals(false, titles.contains("Google News"))
    }

    @Test
    fun emptyDocumentYieldsNoTitles() {
        assertEquals(emptyList<String>(), EvWeb.parseNewsTitles("<rss><channel></channel></rss>"))
    }
}
