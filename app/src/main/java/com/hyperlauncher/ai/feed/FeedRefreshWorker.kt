package com.hyperlauncher.ai.feed

import android.content.Context
import androidx.work.*
import com.hyperlauncher.ai.HyperLauncherApp
import com.hyperlauncher.ai.BuildConfig
import com.hyperlauncher.feed.FeedParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One-shot job only after a user presses Refresh; no periodic network polling. */
class FeedRefreshWorker(context: Context,params: WorkerParameters): CoroutineWorker(context,params) {
    override suspend fun doWork(): Result=withContext(Dispatchers.IO) {
        if(BuildConfig.FLAVOR!="network" || !BuildConfig.ENABLE_FEED) return@withContext Result.failure(workDataOf("error" to "Feed fetching is disabled in this build"))
        val dao=(applicationContext as HyperLauncherApp).container.database.feedDao()
        val failures=mutableListOf<String>()
        for(source in dao.sources()) {
            try {
                val articles=FeedParser.parse(FeedHttp.fetch(source.url),source.url)
                dao.addArticles(articles.map {FeedArticle(it.url,source.url,it.title,it.published)})
            } catch(e: Exception) {if(e is CancellationException) throw e;failures.add(source.name+": "+e.message.orEmpty().take(200))}
        }
        dao.trim()
        if(failures.isEmpty()) Result.success() else Result.failure(workDataOf("error" to failures.joinToString("\n").take(2000)))
    }
}
