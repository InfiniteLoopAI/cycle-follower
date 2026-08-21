package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.Article
import com.infiniteloop.cyclefollower.domain.Block
import com.infiniteloop.cyclefollower.domain.CyclePhase
import com.infiniteloop.cyclefollower.domain.Library
import com.infiniteloop.cyclefollower.domain.NoteTone
import com.infiniteloop.cyclefollower.domain.PhaseGuide
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import com.infiniteloop.cyclefollower.ui.components.BulletList
import com.infiniteloop.cyclefollower.ui.components.Callout
import com.infiniteloop.cyclefollower.ui.components.CalloutTone
import com.infiniteloop.cyclefollower.ui.components.LevelMeter
import com.infiniteloop.cyclefollower.ui.components.SectionCard
import com.infiniteloop.cyclefollower.ui.theme.phasePalette

@Composable
fun LearnScreen(profile: UserProfile) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val current = open

    when {
        current == null -> LearnIndex(profile, onOpen = { open = it })
        current.startsWith("article:") ->
            Library.byId(current.removePrefix("article:"))?.let {
                ArticleDetail(it) { open = null }
            } ?: LearnIndex(profile, onOpen = { open = it })
        current.startsWith("phase:") ->
            runCatching { CyclePhase.valueOf(current.removePrefix("phase:")) }.getOrNull()?.let {
                PhaseDetail(PhaseGuides.of(it)) { open = null }
            } ?: LearnIndex(profile, onOpen = { open = it })
        else -> LearnIndex(profile, onOpen = { open = it })
    }
}

@Composable
private fun LearnIndex(profile: UserProfile, onOpen: (String) -> Unit) {
    val dark = isSystemInDarkTheme()
    val guides = if (profile.contraception.hasNaturalCycle) {
        PhaseGuides.naturalCycleOrder()
    } else {
        PhaseGuides.all().filterNot { it.phase.isNaturalCycle } + PhaseGuides.naturalCycleOrder()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Learn", style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The background that makes the daily hints make sense.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Text(
                "The phases",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(guides.size, key = { guides[it].phase.name }) { index ->
            val guide = guides[index]
            val palette = phasePalette(guide.phase, dark)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(palette.container, RoundedCornerShape(18.dp))
                    .clickable { onOpen("phase:${guide.phase.name}") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(guide.emoji, fontSize = 26.sp)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        guide.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.onContainer,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        guide.tagline,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.onContainer,
                    )
                }
            }
        }

        item {
            Text(
                "Read up",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        items(Library.articles.size, key = { Library.articles[it].id }) { index ->
            val article = Library.articles[index]
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        RoundedCornerShape(18.dp),
                    )
                    .clickable { onOpen("article:${article.id}") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(article.emoji, fontSize = 24.sp)
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(article.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${article.subtitle}  ·  ${article.minutes} min",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Callout(title = "Please read this bit", text = Library.DISCLAIMER, tone = CalloutTone.INFO)
        }
    }
}

@Composable
private fun DetailScaffold(onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack, modifier = Modifier.padding(bottom = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Back")
            }
        }
        content()
    }
}

@Composable
private fun ArticleDetail(article: Article, onBack: () -> Unit) {
    DetailScaffold(onBack) {
        item {
            Column {
                Text(article.emoji, fontSize = 34.sp)
                Spacer(Modifier.height(8.dp))
                Text(article.title, style = MaterialTheme.typography.displaySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    article.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(article.blocks.size) { index ->
            when (val block = article.blocks[index]) {
                is Block.Head -> Text(
                    block.text,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                is Block.Para -> Text(block.text, style = MaterialTheme.typography.bodyLarge)
                is Block.Bullets -> BulletList(block.items)
                is Block.Numbered -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    block.items.forEachIndexed { position, item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                "${position + 1}.",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(width = 28.dp, height = 24.dp),
                            )
                            Text(item, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                is Block.Note -> Callout(
                    text = block.text,
                    title = block.title,
                    tone = when (block.tone) {
                        NoteTone.INFO -> CalloutTone.INFO
                        NoteTone.WARNING -> CalloutTone.WARNING
                        NoteTone.GOLDEN -> CalloutTone.GOLDEN
                    },
                )
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text(
                Library.DISCLAIMER,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PhaseDetail(guide: PhaseGuide, onBack: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val palette = phasePalette(guide.phase, dark)

    DetailScaffold(onBack) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(palette.container, RoundedCornerShape(20.dp))
                    .padding(18.dp),
            ) {
                Column {
                    Text(guide.emoji, fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        guide.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.onContainer,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        guide.tagline,
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.onContainer,
                    )
                }
            }
        }
        item {
            SectionCard(title = "What is happening") {
                Text(guide.whatsHappening, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            SectionCard(title = "Typical levels") {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LevelMeter("Energy", guide.energy, palette.accent)
                    LevelMeter("Patience", guide.patience, palette.accent)
                    LevelMeter("Sex drive", guide.libido, palette.accent)
                    LevelMeter("Social battery", guide.socialBattery, palette.accent)
                }
            }
        }
        item {
            SectionCard(title = "In her body") { BulletList(guide.physical) }
        }
        item {
            SectionCard(title = "In her head") { BulletList(guide.emotional) }
        }
        item {
            SectionCard(
                title = "Do this",
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            ) { BulletList(guide.doThis, marker = "✓") }
        }
        item {
            SectionCard(
                title = "Avoid this",
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
            ) { BulletList(guide.avoidThis, marker = "✕", markerColor = MaterialTheme.colorScheme.error) }
        }
        item {
            SectionCard(title = "Good time for") { BulletList(guide.goodTimeFor, marker = "→") }
        }
    }
}
