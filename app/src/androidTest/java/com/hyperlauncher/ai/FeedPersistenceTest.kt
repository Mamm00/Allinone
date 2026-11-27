package com.hyperlauncher.ai

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.hyperlauncher.ai.data.local.AppDatabase
import com.hyperlauncher.ai.feed.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
class FeedPersistenceTest {
    @Test fun readStateSurvivesRefreshAndSourceRemoval() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val db=Room.inMemoryDatabaseBuilder(context,AppDatabase::class.java).build()
        try {
            val dao=db.feedDao()
            dao.addSource(FeedSource("https://example.com/feed","Test"))
            val article=FeedArticle("https://example.com/article","https://example.com/feed","Title","")
            dao.addArticles(listOf(article));dao.setRead(article.url,true)
            dao.addArticles(listOf(article.copy(title="Updated")))
            assertTrue(dao.observeArticles().first().single().isRead)
            dao.removeSource(article.source)
            assertTrue(dao.observeArticles().first().isEmpty())
            assertNotNull(WorkManager.getInstance(context))
        } finally {db.close()}
    }
}
