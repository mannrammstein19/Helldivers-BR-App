package br.com.helldiversbr.app

import android.content.pm.ActivityInfo
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Toques físicos: performClick sozinho não detectaria uma camada cobrindo o botão. */
@RunWith(AndroidJUnit4::class)
class NavigationTouchTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Before fun dismissTutorial() {
        ui.waitForIdle()
        if (ui.onAllNodesWithText("ENTENDI").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithText("ENTENDI").performClick()
        }
        ui.onNodeWithTag("screen-inicio").assertIsDisplayed()
    }

    private fun tapTab(route: String, yFraction: Float = .5f) {
        ui.onNodeWithTag("tab-$route").performTouchInput {
            click(Offset(width / 2f, height * yFraction))
        }
        ui.waitForIdle()
    }

    private fun assertHome() {
        ui.onNodeWithTag("screen-inicio").assertIsDisplayed()
        ui.onNodeWithTag("tab-inicio").assertIsSelected()
    }

    @Test fun homeIconAndLabelReturnFromEveryTabRepeatedly() {
        repeat(2) {
            listOf("guerra", "ordem", "mapa", "arsenal", "configuracoes").forEach { route ->
                tapTab(route)
                ui.onNodeWithTag("screen-$route").assertIsDisplayed()
                tapTab("inicio", .25f)
                assertHome()
                tapTab(route)
                tapTab("inicio", .78f)
                assertHome()
            }
        }
    }

    @Test fun edgeSwipeOpensMenuAndHomeEntryReturns() {
        tapTab("guerra")
        ui.onNodeWithTag("screen-guerra").performTouchInput {
            swipe(Offset(2f, height * .5f), Offset(width * .65f, height * .5f), 500)
        }
        ui.onNodeWithTag("drawer-inicio").performScrollTo().assertIsDisplayed()
            .performTouchInput { click(center) }
        assertHome()
    }

    @Test fun edgeMenuReturnsAndAndroidBackClosesDrawerFirst() {
        tapTab("ordem")
        ui.onNodeWithTag("screen-ordem").performTouchInput {
            swipe(Offset(2f, height * .5f), Offset(width * .65f, height * .5f), 500)
        }
        ui.onNodeWithTag("drawer-inicio").assertIsDisplayed()
        pressBack()
        ui.onNodeWithTag("screen-ordem").assertIsDisplayed()
        ui.onNodeWithTag("screen-ordem").performTouchInput {
            swipe(Offset(2f, height * .5f), Offset(width * .65f, height * .5f), 500)
        }
        ui.onNodeWithTag("drawer-inicio").performScrollTo().performTouchInput { click(center) }
        assertHome()
        tapTab("guerra")
        pressBack()
        assertHome()
    }

    @Test fun homeWorksAfterLandscapeRotation() {
        tapTab("configuracoes")
        ui.runOnUiThread { ui.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ui.waitUntil(10_000) {
            val node = ui.onAllNodesWithTag("screen-configuracoes").fetchSemanticsNodes().singleOrNull()
            node != null && node.size.width > node.size.height
        }
        tapTab("inicio")
        assertHome()
    }

    @Test fun verticalEdgeScrollDoesNotOpenMenu() {
        tapTab("guerra")
        ui.onNodeWithTag("screen-guerra").performTouchInput {
            swipe(Offset(2f, height * .7f), Offset(2f, height * .3f), 500)
        }
        ui.onNodeWithTag("drawer-inicio").assertIsNotDisplayed()
        tapTab("inicio")
        assertHome()
    }

    @Test fun notificationsBackReturnsToSettingsThenHomeWorks() {
        tapTab("configuracoes")
        ui.onNodeWithTag("open-menu").assertDoesNotExist()
        ui.onNodeWithTag("settings-notifications").assertIsDisplayed()
        ui.onNodeWithText("NOTIFICAÇÕES").performScrollTo().performClick()
        ui.onNodeWithTag("screen-notificacoes").assertIsDisplayed()
        pressBack()
        ui.onNodeWithTag("screen-configuracoes").assertIsDisplayed()
        tapTab("inicio")
        assertHome()
    }
}
