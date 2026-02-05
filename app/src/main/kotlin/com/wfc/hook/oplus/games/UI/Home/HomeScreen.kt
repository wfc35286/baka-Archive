package com.wfc.hook.oplus.games.UI.Home

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// ✨ 导入两个搬家后的界面 ✨
import com.wfc.hook.oplus.games.UI.Home.AutoCombe.AutoCombeScreen
import com.wfc.hook.oplus.games.UI.Home.Skin.SkinHackScreen
//import androidx.compose.material.icons.filled.HomeKt

// 数据类
data class FeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "list",
        enterTransition = { 
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400)
            )
        },
        exitTransition = { 
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(400)
            )
        },
        popEnterTransition = { 
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400)
            )
        },
        popExitTransition = { 
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(400)
            )
        }
    ) {
        composable("list") {
            MainListContent(navController)
        }
        
        composable("feature_number") {
            // 这里现在引用的是 UI.AutoCombe 包下的类
            AutoCombeScreen(onBack = { navController.popBackStack() })
        }
        
        composable("feature_skin") {
            // 这里现在引用的是 UI.Skin 包下的类
            SkinHackScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
fun MainListContent(navController: NavHostController) {
    val themePrimary = MaterialTheme.colorScheme.primary

    val features = remember(themePrimary) {
        listOf(
            FeatureItem(
                id = "number_hack",
                title = "自动连招",
                description = "修改一键连招执行次数上限",
                icon = Icons.Filled.OfflineBolt,
                route = "feature_number",
                color = themePrimary
            ),
            FeatureItem(
                id = "skin_hack",
                title = "修改皮肤",
                description = "自定义切换游戏助手皮肤",
                icon = Icons.Filled.AutoAwesome,
                route = "feature_skin",
                color = themePrimary
            )
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // --- 顶部精致文字标题 ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape, 
                    color = themePrimary, 
                    modifier = Modifier.size(6.dp)
                ) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    "控制中心", 
                    style = MaterialTheme.typography.labelLarge, 
                    color = themePrimary, 
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "游戏助手增强", 
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "针对 ColorOS 游戏助手的定制化功能扩展", 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(Modifier.height(8.dp))

        Text(
            "核心插件", 
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.outline, 
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(features, key = { it.id }) { feature ->
                FeatureCard(
                    feature = feature,
                    onClick = { navController.navigate(feature.route) }
                )
            }
        }
    }
}

@Composable
fun FeatureCard(feature: FeatureItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(feature.color, feature.color.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(feature.icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(feature.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
        }
    }
}
