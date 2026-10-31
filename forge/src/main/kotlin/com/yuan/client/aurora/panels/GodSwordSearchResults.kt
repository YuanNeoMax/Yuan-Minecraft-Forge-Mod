package com.yuan.client.aurora.panels

import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yuan.client.aurora.LocalLayoutRevision
import com.yuan.client.aurora.theme.auroraFont
import com.yuan.client.aurora.scaffold.auroraScroll
import com.yuan.client.aurora.scaffold.handCursor
import com.yuan.client.aurora.theme.Star
import com.yuan.client.aurora.theme.TypeScale

/**
 * The results of a search that spanned every feature.
 *
 * <p>A RESULT LIST RATHER THAN A FILTER. Filtering the current feature's rows would only rearrange
 * what is already on screen; the reason to search is that the setting is somewhere else. So each hit
 * names the feature and the group it belongs to and, when chosen, takes the player there.
 *
 * <p>Grouped by feature, because that is the unit the rail selects: three hits under 回溯 are one
 * decision ("I want 回溯"), while three hits in three features are three. Predictable ordering also
 * keeps the list stable as the player types -- a list that reorders underneath the pointer is the
 * one thing that makes search results unusable.
 */
@Composable
internal fun GodSwordSearchResults(
    query: String,
    hits: List<GodSwordModules.SearchHit>,
    onOpen: (GodSwordModules.SearchHit) -> Unit
) {
    // Subscribe: the rows below read values through the binding, so a write without a subscription
    // changes the model and never repaints.
    @Suppress("UNUSED_EXPRESSION")
    LocalLayoutRevision.current

    Column(
        Modifier
            .fillMaxSize()
            .auroraScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (hits.isEmpty()) {
            // An empty result must SAY SO. A blank area reads as a failure to load, and the player
            // cannot tell whether the search ran.
            Text(
                "没有匹配「$query」的设置",
                fontSize = TypeScale.row,
                color = Star.textTertiary,
                fontFamily = auroraFont,
                modifier = Modifier.padding(vertical = 14.dp)
            )
            return@Column
        }

        Text(
            "${hits.size} 项匹配「$query」",
            fontSize = TypeScale.hint,
            color = Star.textTertiary,
            fontFamily = auroraFont,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        var lastModule: String? = null
        hits.forEach { hit ->
            if (hit.moduleId != lastModule) {
                if (lastModule != null) Spacer(Modifier.height(6.dp))
                Text(
                    hit.moduleLabel,
                    fontSize = TypeScale.hint,
                    color = Star.textTertiary,
                    fontFamily = auroraFont,
                    modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                )
                lastModule = hit.moduleId
            }
            SearchHitRow(hit, onOpen)
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** One hit: the setting's name, where it lives, and the whole row is the target. */
@Composable
private fun SearchHitRow(
    hit: GodSwordModules.SearchHit,
    onOpen: (GodSwordModules.SearchHit) -> Unit
) {
    val interaction = remember(hit.rowId) { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .clip(RoundedCornerShape(Star.controlRadius))
            .background(if (hovered) Star.hover else Color.Transparent)
            .handCursor()
            .clickable(
                interactionSource = interaction,
                indication = null
            ) { onOpen(hit) }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            hit.rowName,
            fontSize = TypeScale.value,
            color = Star.textPrimary,
            fontFamily = auroraFont,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.weight(1f))
        // Where it lives, so choosing it is an informed move rather than a jump.
        Text(
            hit.groupHeading,
            fontSize = TypeScale.hint,
            color = Star.textTertiary,
            fontFamily = auroraFont
        )
    }
}
