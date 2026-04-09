package com.dreason.frame.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreason.frame.core.model.Route
import com.dreason.frame.ui.theme.ChinaRoute
import com.dreason.frame.ui.theme.DirectRoute
import com.dreason.frame.ui.theme.RejectRoute
import com.dreason.frame.ui.theme.TaiwanRoute

@Composable
fun RouteChip(
    route: Route,
    modifier: Modifier = Modifier,
) {
    val color = when (route) {
        Route.CHINA_PROXY -> ChinaRoute
        Route.TAIWAN_PROXY -> TaiwanRoute
        Route.DIRECT -> DirectRoute
        Route.REJECT -> RejectRoute
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = route.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
