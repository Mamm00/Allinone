package com.hyperlauncher.ai.feed

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.*
import com.hyperlauncher.ai.BuildConfig
import com.hyperlauncher.feed.FeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun FeedScreen(dao: FeedDao,onClose: ()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val sources by dao.observeSources().collectAsState(emptyList())
    val articles by dao.observeArticles().collectAsState(emptyList())
    val manager=remember {WorkManager.getInstance(context)}
    val jobs by manager.getWorkInfosForUniqueWorkFlow("feed-explicit-refresh").collectAsState(emptyList())
    var manage by remember {mutableStateOf(false)}
    var url by remember {mutableStateOf("")};var name by remember {mutableStateOf("")}
    var error by remember {mutableStateOf<String?>(null)}
    val active=jobs.any {!it.state.isFinished}
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(12.dp)) {
            Text("Your Feed",style=MaterialTheme.typography.headlineMedium)
            Row {
                TextButton(onClose) {Text("Home")}
                TextButton({manage=true}) {Text("Sources")}
                TextButton({
                    val request=OneTimeWorkRequestBuilder<FeedRefreshWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
                    manager.enqueueUniqueWork("feed-explicit-refresh",ExistingWorkPolicy.KEEP,request)
                },enabled=BuildConfig.FLAVOR=="network" && !active && sources.isNotEmpty()) {Text(if(active) "Refreshing…" else "Refresh")}
            }
            Text("No automatic network requests. Refresh contacts the sources you selected; opening an article uses your browser.",style=MaterialTheme.typography.bodySmall)
            if(BuildConfig.FLAVOR!="network") Text("Offline build: saved articles only; fetching requires the network build.")
            (error ?: jobs.firstOrNull {it.state==WorkInfo.State.FAILED}?.outputData?.getString("error"))?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            if(articles.isEmpty()) Text("No saved articles. Add sources, then explicitly refresh.",Modifier.padding(16.dp))
            LazyColumn(Modifier.weight(1f)) {items(articles,key={it.url}) {article ->
                ListItem(headlineContent={Text(article.title)},overlineContent={Text(if(article.isRead) "Read" else "NEW · Unread")},supportingContent={Text(article.published)})
                Row {
                    TextButton({
                        runCatching {context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(article.url)))}
                            .onSuccess {scope.launch(Dispatchers.IO) {dao.setRead(article.url,true)}}
                            .onFailure {error="No browser is available"}
                    }) {Text("Read article")}
                    TextButton({scope.launch(Dispatchers.IO) {dao.setRead(article.url,!article.isRead)}}) {Text(if(article.isRead) "Mark unread" else "Mark read")}
                }
            }}
        }
    }
    if(manage) AlertDialog(onDismissRequest={manage=false},title={Text("Feed sources")},text={
        LazyColumn(Modifier.heightIn(max=440.dp)) {
            item {
                Text("Editorial suggestions — not endorsements. Adding does not fetch until you press Refresh.")
                listOf("NASA" to "https://www.nasa.gov/feed/", "Android Developers" to "https://android-developers.googleblog.com/feeds/posts/default").forEach {(label,feed) ->
                    TextButton({scope.launch(Dispatchers.IO) {dao.addSource(FeedSource(feed,label))}}) {Text("+ $label")}
                }
                OutlinedTextField(name,{name=it.take(100)},label={Text("Name")})
                OutlinedTextField(url,{url=it.take(2000)},label={Text("HTTPS RSS/Atom URL")})
                TextButton({scope.launch {withContext(Dispatchers.IO) {dao.addSource(FeedSource(url.trim(),name.trim().ifBlank {url.trim()}))};url="";name=""}},
                    enabled=FeedParser.safeUrl(url.trim()) && sources.size<30) {Text("Add source")}
            }
            items(sources,key={it.url}) {source ->
                Text(source.name)
                TextButton({scope.launch(Dispatchers.IO) {dao.removeSource(source.url)}}) {Text("Remove source and its articles")}
            }
        }
    },confirmButton={TextButton({manage=false}){Text("Done")}})
}
