package com.guicarneirodev.gympro.ui

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.guicarneirodev.gympro.presentation.navigation.GymProNavHost
import com.guicarneirodev.gympro.presentation.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var navController: TestNavHostController

    @Before
    fun setupNavHost() {
        composeTestRule.setContent {
            navController = TestNavHostController(LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            GymProNavHost(navController = navController)
        }
    }

    @Test
    fun navHost_verifyStartDestination() {
        composeTestRule.apply {
            val route = navController.currentBackStackEntry?.destination?.route
            assertTrue(
                "Should start at Login screen",
                route?.contains("Login") == true
            )
        }
    }

    @Test
    fun navHost_navigateToRegisterFromLogin() {
        composeTestRule.apply {
            runOnIdle {
                navController.navigate(Screen.Register)
            }

            val route = navController.currentBackStackEntry?.destination?.route
            assertTrue(
                "Should be on Register screen",
                route?.contains("Register") == true
            )
        }
    }

    @Test
    fun navHost_navigateToSettings() {
        composeTestRule.runOnIdle {
            navController.navigate(Screen.WorkoutList)
        }

        composeTestRule.runOnIdle {
            navController.navigate(Screen.Settings)
        }

        composeTestRule.runOnIdle {
            val route = navController.currentBackStackEntry?.destination?.route
            assertTrue(
                "Should be on Settings screen",
                route?.contains("Settings") == true
            )
        }
    }

}