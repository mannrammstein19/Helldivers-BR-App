package br.com.helldiversbr.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/** Toques físicos: performClick sozinho não detectaria uma camada cobrindo o botão. */
@RunWith(AndroidJUnit4::class)
class NavigationTouchTest {
    @get:Rule(order = 0) val ui = createAndroidComposeRule<MainActivity>()
    private var phase = "preparação"

    // Executa antes de a regra externa fechar a Activity, preservando a tela da falha.
    @get:Rule(order = 1) val evidence = object : TestWatcher() {
        override fun failed(error: Throwable, description: Description) {
            Log.e("NavigationTouchTest", "Falha em ${description.methodName}: $phase", error)
            runCatching { ui.onAllNodes(isRoot(), useUnmergedTree = true).printToLog("NavigationTouchTest") }
            runCatching {
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "navigation-failures")
                directory.mkdirs()
                File(directory, "${description.methodName}.txt").writeText(
                    "Etapa: $phase\n${error.stackTraceToString()}"
                )
                instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
                    File(directory, "${description.methodName}.png").outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    bitmap.recycle()
                }
            }
        }
    }

    private fun waitForOrientation(orientation: Int) {
        phase = "aguardando orientação $orientation e janela pronta"
        ui.waitUntil(15_000) {
            var ready = false
            // A rotação substitui a Activity; consulte a instância atual no thread da UI.
            ui.activityRule.scenario.onActivity { activity ->
                val window = activity.window.decorView
                ready = activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    activity.resources.configuration.orientation == orientation &&
                    window.hasWindowFocus() && window.width > 0 && window.height > 0 &&
                    (if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                        window.width > window.height else window.height > window.width)
            }
            ready
        }
        ui.waitForIdle()
    }

    private fun waitForVisible(tag: String) {
        phase = "aguardando componente $tag"
        ui.waitUntil(10_000) {
            runCatching { ui.onNodeWithTag(tag).isDisplayed() }.getOrDefault(false)
        }
        ui.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun waitForScreen(route: String) {
        phase = "aguardando tela $route"
        ui.waitUntil(10_000) {
            runCatching { ui.onNodeWithTag("screen-$route").isDisplayed() }.getOrDefault(false)
        }
        ui.onNodeWithTag("screen-$route").assertIsDisplayed()
    }

    @Before fun dismissTutorial() {
        // Primeiro aguarde a abertura: o tutorial só é composto depois dela.
        ui.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        waitForScreen("inicio")
        ui.waitForIdle()
        if (ui.onAllNodesWithText("ENTENDI").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithText("ENTENDI").performClick()
            ui.waitUntil(5_000) {
                ui.onAllNodesWithText("ENTENDI").fetchSemanticsNodes().isEmpty()
            }
        }
        waitForOrientation(Configuration.ORIENTATION_PORTRAIT)
        assertHome()
    }

    private fun tapTab(route: String, yFraction: Float = .5f) {
        phase = "toque físico na aba $route"
        ui.onNodeWithTag("tab-$route").assertIsDisplayed()
        Log.i("NavigationTouchTest", "$phase: ${ui.onNodeWithTag("tab-$route").fetchSemanticsNode().boundsInRoot}")
        ui.onNodeWithTag("tab-$route").performTouchInput {
            click(Offset(width / 2f, height * yFraction))
        }
        waitForScreen(route)
    }

    private fun assertHome() {
        waitForScreen("inicio")
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
        waitForVisible("navigation-drawer")
        ui.onNodeWithTag("drawer-inicio").performScrollTo().assertIsDisplayed()
            .performTouchInput { click(center) }
        assertHome()
    }

    @Test fun edgeMenuReturnsAndAndroidBackClosesDrawerFirst() {
        tapTab("ordem")
        ui.onNodeWithTag("screen-ordem").performTouchInput {
            swipe(Offset(2f, height * .5f), Offset(width * .65f, height * .5f), 500)
        }
        waitForVisible("navigation-drawer")
        ui.onNodeWithTag("drawer-inicio").performScrollTo().assertIsDisplayed()
        pressBack()
        ui.waitUntil(5_000) { !ui.onNodeWithTag("navigation-drawer").isDisplayed() }
        ui.onNodeWithTag("screen-ordem").assertIsDisplayed()
        ui.onNodeWithTag("screen-ordem").performTouchInput {
            swipe(Offset(2f, height * .5f), Offset(width * .65f, height * .5f), 500)
        }
        waitForVisible("navigation-drawer")
        ui.onNodeWithTag("drawer-inicio").performScrollTo().performTouchInput { click(center) }
        assertHome()
        tapTab("guerra")
        pressBack()
        assertHome()
    }

    @Test fun homeWorksAfterLandscapeRotation() {
        tapTab("configuracoes")
        waitForScreen("configuracoes")
        var beforeRotation: MainActivity? = null
        ui.activityRule.scenario.onActivity { beforeRotation = it }
        phase = "recriação da Activity após rotação"
        ui.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ui.waitUntil(15_000) {
            var recreated = false
            ui.activityRule.scenario.onActivity { recreated = it !== beforeRotation }
            recreated
        }
        waitForOrientation(Configuration.ORIENTATION_LANDSCAPE)
        waitForScreen("configuracoes")
        ui.onNodeWithTag("navigation-drawer").assertIsNotDisplayed()
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
        waitForScreen("notificacoes")
        pressBack()
        waitForScreen("configuracoes")
        tapTab("inicio")
        assertHome()
    }
}
