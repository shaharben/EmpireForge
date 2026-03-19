package com.unciv.ui.screens.worldscreen.status

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable
import com.unciv.ui.components.extensions.setSize
import com.unciv.ui.components.input.onActivation
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.worldscreen.TurnSummaryPopup
import com.unciv.ui.screens.worldscreen.WorldScreen

class StatusButtons(
    val nextTurnButton: NextTurnButton
) : Table(), Disposable {
    var autoPlayStatusButton: AutoPlayStatusButton? = null
    var multiplayerStatusButton: MultiplayerStatusButton? = null
    var smallUnitButton: SmallUnitButton? = null
    var turnSummaryButton: Table? = null
    private val padXSpace = 10f
    private val padYSpace = 5f

    init {
        add(nextTurnButton)
    }

    /** Creates the small info button (ⓘ) that opens the [TurnSummaryPopup] */
    fun createTurnSummaryButton(worldScreen: WorldScreen) {
        val button = Table(BaseScreen.skin)
        val circle = ImageGetter.getCircle(Color.DARK_GRAY, 30f)
        button.addActor(circle)
        val icon = ImageGetter.getImage("OtherIcons/Question")
        icon.setSize(20f)
        icon.color = Color.GOLD
        icon.setPosition(5f, 5f)
        button.addActor(icon)
        button.setSize(30f, 30f)
        button.onActivation {
            TurnSummaryPopup(worldScreen).open()
        }
        turnSummaryButton = button
    }

    fun update(verticalWrap: Boolean) {
        clear()
        if(verticalWrap) {
            // In vertical mode, place info button in a row with the next turn button
            val nextTurnRow = Table()
            turnSummaryButton?.let { nextTurnRow.add(it).padRight(padXSpace / 2).top() }
            nextTurnRow.add(nextTurnButton)
            add(nextTurnRow)
            smallUnitButton?.let {
                row()
                add(it).padTop(padYSpace).right()
            }
            autoPlayStatusButton?.let {
                row()
                add(it).padTop(padYSpace).right()
            }
            multiplayerStatusButton?.let {
                row()
                add(it).padTop(padYSpace).right()
            }
        } else {
            multiplayerStatusButton?.let { add(it).padRight(padXSpace).top() }
            autoPlayStatusButton?.let { add(it).padRight(padXSpace).top() }
            smallUnitButton?.let { add(it).padRight(padXSpace).top() }
            turnSummaryButton?.let { add(it).padRight(padXSpace / 2).top() }
            add(nextTurnButton)
        }
        pack()
    }

    override fun dispose() {
        autoPlayStatusButton?.dispose()
        multiplayerStatusButton?.dispose()
    }
}
