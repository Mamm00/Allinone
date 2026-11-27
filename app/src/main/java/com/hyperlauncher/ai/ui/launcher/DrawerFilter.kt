package com.hyperlauncher.ai.ui.launcher

import com.hyperlauncher.ai.data.model.AppInfo
import java.text.Collator
import java.util.Locale

/** One filter pipeline: switching categories never drops an active search. */
fun filterDrawerApps(apps: List<AppInfo>, query: String, categoryId: String, alphabetical: Boolean): List<AppInfo> {
    val search = query.trim()
    val collator = Collator.getInstance(Locale.getDefault())
    return apps.filter {
        (alphabetical || categoryId == "all" || it.categoryId == categoryId) &&
            (search.isEmpty() || it.label.contains(search, true) || it.packageName.contains(search, true))
    }.sortedWith { a, b -> collator.compare(a.label, b.label).takeIf { it != 0 }
        ?: a.packageName.compareTo(b.packageName) }
}
