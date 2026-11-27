package com.hyperlauncher.ai.feed

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="feed_sources")
data class FeedSource(@PrimaryKey val url: String,val name: String)
@Entity(tableName="feed_articles")
data class FeedArticle(@PrimaryKey val url: String,val source: String,val title: String,
    val published: String,val isRead: Boolean=false,val fetchedAt: Long=System.currentTimeMillis())
@Dao
interface FeedDao {
    @Query("SELECT * FROM feed_sources ORDER BY name") fun observeSources(): Flow<List<FeedSource>>
    @Query("SELECT * FROM feed_articles ORDER BY fetchedAt DESC, url LIMIT 1000") fun observeArticles(): Flow<List<FeedArticle>>
    @Query("SELECT * FROM feed_sources") suspend fun sources(): List<FeedSource>
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun addSource(source: FeedSource)
    @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun addArticles(articles: List<FeedArticle>)
    @Query("UPDATE feed_articles SET isRead = :read WHERE url = :url") suspend fun setRead(url: String,read: Boolean)
    @Query("DELETE FROM feed_sources WHERE url = :url") suspend fun deleteSource(url: String)
    @Query("DELETE FROM feed_articles WHERE source = :url") suspend fun deleteArticles(url: String)
    @Transaction suspend fun removeSource(url: String) {deleteSource(url);deleteArticles(url)}
    @Query("DELETE FROM feed_articles WHERE url NOT IN (SELECT url FROM feed_articles ORDER BY fetchedAt DESC LIMIT 1000)") suspend fun trim()
}
