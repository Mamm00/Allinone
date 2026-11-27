package com.hyperlauncher.ai

import android.appwidget.AppWidgetHost
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.hyperlauncher.ai.ui.workspace.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class WidgetResizeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun continuousResizeChangesAndPersistsBounds() {
        var saved=Placement(id="resizable",kind="clock",width=.5f,height=.3f)
        compose.setContent {
            val context=LocalContext.current
            val host=remember { AppWidgetHost(context,0x1234) }
            var layout by remember { mutableStateOf(WorkspaceLayout(items=listOf(saved))) }
            MaterialTheme {
                androidx.compose.foundation.layout.Box(Modifier.size(320.dp,500.dp)) {
                    WorkspacePage(layout,0,true,host,emptyList(),onChange={ saved=it; layout=layout.copy(items=listOf(it)) },
                        onRemove={},onStack={},onLaunch={})
                }
            }
        }
        compose.onNodeWithTag("resize_resizable").performTouchInput { swipe(center,center+androidx.compose.ui.geometry.Offset(35f,25f),500) }
        compose.runOnIdle { assertTrue(saved.width > .5f); assertTrue(saved.height > .3f) }
    }
}
