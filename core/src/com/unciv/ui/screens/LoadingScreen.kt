package com.unciv.ui.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.images.ImageWithCustomSize
import com.unciv.ui.popups.popups
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.popups.LoadingPopup

/** A loading screen that creates a screenshot of the current screen and adds a "Loading..." popup on top of that */
class LoadingScreen(
    previousScreen: BaseScreen? = null
) : BaseScreen() {
    private val screenshot: Texture

    companion object {
        val gameplayTips = arrayOf(
            "Clean water is rare after the Fall. Settle near rivers for survival bonuses.",
            "Mountains make natural fortresses. Build observatories to boost research by 50%.",
            "Explore old world ruins early! Pre-war bunkers contain lost tech and supplies.",
            "Trade surplus resources with other factions to keep morale high.",
            "Supply roads between settlements reduce maintenance costs. Build networks.",
            "Fortified units gain a 25% defensive bonus. Dig in when raiders approach.",
            "Allied outposts provide food to your capital. Diplomacy feeds your people.",
            "Exceptional scientists can instantly recover a lost technology.",
            "Weak factions attract raiders and hostile takeovers. Stay armed.",
            "Cultural dominance requires completing 5 ideology trees and building the Utopia Project.",
            "Hills provide extra salvage materials and a defensive combat bonus.",
            "Archers are strong early defenders. Build them before the raiders find you."
        )
    }

    init {
        screenshot = takeScreenshot(previousScreen)
        val image = ImageWithCustomSize(
            TextureRegion(
                screenshot,
                0,
                screenshot.height,
                screenshot.width,
                -screenshot.height
            )
        )
        image.width = stage.width
        image.height = stage.height
        stage.addActor(image)

        // Dark gradient overlay for better text readability
        val overlay = Table()
        overlay.setFillParent(true)
        overlay.background = skinStrings.getUiBackground(
            "LoadingScreen/Overlay",
            tintColor = Color(0f, 0f, 0f, 0.45f)
        )
        stage.addActor(overlay)

        // EmpireForge branding label
        val brandLabel = "EmpireForge".toLabel(Color(1f, 0.82f, 0.2f, 1f), 28)
        brandLabel.setAlignment(Align.center)
        val brandTable = Table()
        brandTable.add(brandLabel)
        brandTable.pack()
        brandTable.setPosition(stage.width / 2, stage.height / 2 + 60f, Align.center)
        stage.addActor(brandTable)

        // Rotating loading indicator
        val spinner = ImageGetter.getCircle(Color(0.85f, 0.65f, 0.13f, 1f), 40f)
        spinner.setOrigin(Align.center)
        spinner.addAction(Actions.forever(Actions.rotateBy(-360f, 2f)))
        val spinnerTable = Table()
        spinnerTable.add(spinner).size(40f)
        spinnerTable.pack()
        spinnerTable.setPosition(stage.width / 2, stage.height / 2 - 10f, Align.center)
        stage.addActor(spinnerTable)

        // Random gameplay tip with cycling fade transition
        val tip = gameplayTips.random()
        val tipLabel = "Tip: $tip".toLabel(Color(0.9f, 0.9f, 0.8f, 0.95f), 16)
        tipLabel.setAlignment(Align.center)
        tipLabel.wrap = true
        val tipTable = Table()
        tipTable.add(tipLabel).width(stage.width * 0.8f)
        tipTable.pack()
        tipTable.setPosition(stage.width / 2, 80f, Align.center)
        stage.addActor(tipTable)

        // Cycle tips with fade transition every 3 seconds
        tipTable.addAction(Actions.forever(Actions.sequence(
            Actions.delay(3f),
            Actions.fadeOut(0.4f),
            Actions.run {
                val newTip = gameplayTips.random()
                tipLabel.setText("Tip: $newTip")
                tipTable.pack()
                tipTable.setPosition(stage.width / 2, 80f, Align.center)
            },
            Actions.fadeIn(0.4f)
        )))

        // Progress bar at the bottom
        val progressBg = Table()
        progressBg.background = skinStrings.getUiBackground(
            "LoadingScreen/ProgressBar",
            tintColor = Color(0.15f, 0.15f, 0.2f, 0.8f)
        )
        progressBg.setSize(stage.width * 0.6f, 6f)
        progressBg.setPosition(stage.width / 2 - stage.width * 0.3f, 50f)
        stage.addActor(progressBg)

        val progressFill = Table()
        progressFill.background = skinStrings.getUiBackground(
            "LoadingScreen/ProgressFill",
            tintColor = Color(0.85f, 0.65f, 0.13f, 0.9f)
        )
        progressFill.setSize(0f, 6f)
        progressFill.setPosition(stage.width / 2 - stage.width * 0.3f, 50f)
        stage.addActor(progressFill)

        // Animate the progress fill bar from left to right
        progressFill.addAction(Actions.forever(Actions.sequence(
            Actions.sizeTo(stage.width * 0.6f, 6f, 2.5f),
            Actions.run { progressFill.setSize(0f, 6f) }
        )))

        stage.addAction(Actions.sequence(
            Actions.delay(1000f),
            Actions.run {
                LoadingPopup(this)
            }
        ))
    }

    private fun takeScreenshot(previousScreen: BaseScreen?): Texture {
        if (previousScreen != null) {
            for (popup in previousScreen.popups) popup.isVisible = false
            previousScreen.render(Gdx.graphics.deltaTime)
        }
        val pixmap = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.backBufferWidth, Gdx.graphics.backBufferHeight)
        val screenshot = Texture(pixmap)
        pixmap.dispose()

        if (previousScreen != null) {
            for (popup in previousScreen.popups) popup.isVisible = true
        }
        return screenshot
    }


    override fun dispose() {
        screenshot.dispose()
        super.dispose()
    }
}
