package com.infiniteloop.cyclefollower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.infiniteloop.cyclefollower.data.UserProfile
import com.infiniteloop.cyclefollower.domain.CycleEngine
import com.infiniteloop.cyclefollower.domain.PhaseGuides
import java.time.LocalDate

private val Ink = Color(0xFF2A1A20)
private val Panel = Color(0xFF3B2731)
private val Faint = Color(0xFFC7A8B4)
private val Paper = Color(0xFFFFF3F6)
private val Body = Color(0xFFEBD7DE)

private val SayEdge = Color(0xFF6DBF8B)
private val SayBg = Color(0xFF24382C)
private val SayInk = Color(0xFFE6F5EB)
private val DontEdge = Color(0xFFD4736D)
private val DontBg = Color(0xFF3E2325)
private val DontInk = Color(0xFFF7E2E1)

/**
 * For the moment it has actually gone wrong, when scrolling the Today screen is not going to
 * happen. Deliberately dark, short, and free of anything that is not immediately usable.
 */
@Composable
fun RightNowScreen(profile: UserProfile, onBack: () -> Unit) {
    val today = remember { LocalDate.now() }
    val status = remember(profile, today) { CycleEngine.status(profile, today) }
    val guide = remember(status) { status?.let { PhaseGuides.of(it.phase) } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Faint)
                Spacer(Modifier.size(8.dp))
                Text("Back", color = Faint)
            }
        }

        item {
            Column {
                Text(
                    if (status == null) "Right now" else "Right now · day ${status.cycleDay}",
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = Faint,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "She is upset and you are not sure why",
                    fontSize = 27.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold,
                    color = Paper,
                )
            }
        }

        if (guide == null) {
            item {
                Panel("Add the date her last period started and this screen can tell you what is likely going on.")
            }
        } else {
            item { Panel(guide.whatsHappening) }

            item { Heading("Say", SayEdge) }
            items(guide.sayNow.size) { index ->
                Quote(guide.sayNow[index], SayBg, SayEdge, SayInk)
            }

            item { Heading("Do not say", DontEdge) }
            items(guide.dontSayNow.size) { index ->
                Quote(guide.dontSayNow[index], DontBg, DontEdge, DontInk)
            }

            item {
                Panel(guide.doThis.firstOrNull().orEmpty(), title = "Then")
            }
        }
    }
}

@Composable
private fun Heading(text: String, color: Color) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        letterSpacing = 1.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun Quote(text: String, background: Color, edge: Color, ink: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(top = 8.dp)
            .background(background, RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)),
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(edge))
        Text(
            text,
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = ink,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun Panel(text: String, title: String? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(15.dp),
    ) {
        if (title != null) {
            Text(
                title.uppercase(),
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold,
                color = Faint,
            )
            Spacer(Modifier.height(6.dp))
        }
        Text(text, fontSize = 14.5.sp, lineHeight = 21.sp, color = Body)
    }
}
