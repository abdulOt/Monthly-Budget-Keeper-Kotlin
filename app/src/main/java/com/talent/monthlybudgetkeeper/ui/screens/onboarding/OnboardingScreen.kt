package com.talent.monthlybudgetkeeper.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.talent.monthlybudgetkeeper.viewmodel.OnboardingViewModel
import kotlinx.coroutines.flow.collectLatest

private data class IntroPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val accent: List<Color>,
    val bulletOne: String,
    val bulletTwo: String
)

@Composable
fun OnboardingScreen(
    onContinue: (String) -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = remember {
        listOf(
            IntroPage(
                title = "Track your money",
                description = "See what is coming in, what is going out, and what is left with a calmer monthly view.",
                icon = Icons.Outlined.Wallet,
                accent = listOf(Color(0xFF173A5B), Color(0xFF2D5F84)),
                bulletOne = "Clean transaction history with privacy controls",
                bulletTwo = "Fast monthly visibility without clutter"
            ),
            IntroPage(
                title = "Plan budgets and bills",
                description = "Keep budgets, bills, subscriptions, and recurring items organized before they become real spending.",
                icon = Icons.Outlined.PieChartOutline,
                accent = listOf(Color(0xFF1B6D61), Color(0xFF2D8D7F)),
                bulletOne = "Separate planned items from finished transactions",
                bulletTwo = "Stay ahead of due dates with guided setup"
            ),
            IntroPage(
                title = "Keep your data secure",
                description = "Protect balances with private sync, app lock options, and a workflow that feels trustworthy from the first launch.",
                icon = Icons.Outlined.Lock,
                accent = listOf(Color(0xFF8B6A2B), Color(0xFFA98543)),
                bulletOne = "Private sync that works with your existing auth",
                bulletTwo = "Optional privacy mode for sensitive amounts"
            )
        )
    }
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = pages[pageIndex]
    val isLastPage = pageIndex == pages.lastIndex

    LaunchedEffect(Unit) {
        viewModel.completed.collectLatest(onContinue)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.30f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Budget Keeper",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (!isLastPage) {
                            TextButton(onClick = viewModel::completeIntro) {
                                Text("Skip")
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "A calmer way to manage money every month.",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
                item {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.linearGradient(page.accent),
                                        shape = RoundedCornerShape(28.dp)
                                    )
                                    .padding(22.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "0${pageIndex + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f)
                                        )
                                        Text(
                                            text = page.title,
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Clip
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(22.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = page.icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = page.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            IntroBullet(page.bulletOne)
                            IntroBullet(page.bulletTwo)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                pages.forEachIndexed { index, _ ->
                                    Box(
                                        modifier = Modifier
                                            .height(8.dp)
                                            .weight(if (index == pageIndex) 1.4f else 1f)
                                            .background(
                                                color = if (index == pageIndex) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                                                },
                                                shape = CircleShape
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pageIndex > 0) {
                            TextButton(onClick = { pageIndex -= 1 }) {
                                Text("Back")
                            }
                        } else {
                            Box(modifier = Modifier.size(64.dp))
                        }
                        Button(
                            onClick = {
                                if (isLastPage) {
                                    viewModel.completeIntro()
                                } else {
                                    pageIndex += 1
                                }
                            },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text(if (isLastPage) "Get Started" else "Next")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Clip
        )
    }
}
