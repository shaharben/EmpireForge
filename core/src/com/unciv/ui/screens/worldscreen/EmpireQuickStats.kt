package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.logic.civilization.Civilization
import com.unciv.ui.components.extensions.setSize
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.onClick
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.victoryscreen.RankingType

/**
 * A compact panel displayed on the left side of the WorldScreen showing
 * turn counter, era, and key empire statistics at a glance.
 */
class EmpireQuickStats(private val worldScreen: WorldScreen) : Table() {
    private var isExpanded = true

    // Gold-ish accent color for the panel
    private val goldAccent = Color(0.85f, 0.72f, 0.20f, 1f)
    private val dimLabel = Color(0.7f, 0.7f, 0.75f, 1f)
    private val brightValue = Color.WHITE

    init {
        background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/EmpireQuickStats",
            BaseScreen.skinStrings.roundedEdgeRectangleShape,
            Color(0.08f, 0.09f, 0.12f, 0.88f)
        )
        defaults().pad(3f)
        update(worldScreen.viewingCiv)
    }

    fun update(civInfo: Civilization) {
        try {
            clear()

            if (!isExpanded) {
                buildCollapsedView()
                pack()
                return
            }

            buildExpandedView(civInfo)
            pack()
        } catch (_: Exception) {
            // Prevent any crash from breaking the game
        }
    }

    private fun buildCollapsedView() {
        val expandLabel = "\u25B6".toLabel(goldAccent, 16)
        val btn = Table()
        btn.add(expandLabel).pad(6f)
        btn.touchable = Touchable.enabled
        btn.onClick {
            isExpanded = true
            update(worldScreen.viewingCiv)
        }
        add(btn)
    }

    private fun buildExpandedView(civInfo: Civilization) { try {
        // --- Turn Counter Section ---
        val turnLabelSmall = "Turn".toLabel(dimLabel, 12)
        turnLabelSmall.setAlignment(Align.center)
        add(turnLabelSmall).colspan(2).fillX().row()

        val turnNumber = civInfo.gameInfo.turns.toString()
        val turnValueLabel = turnNumber.toLabel(goldAccent, 28)
        turnValueLabel.setAlignment(Align.center)
        add(turnValueLabel).colspan(2).fillX().padBottom(1f).row()

        // Era name
        val eraName = civInfo.getEra().name
        val eraLabel = eraName.toLabel(Color(0.6f, 0.75f, 0.85f, 1f), 11)
        eraLabel.setAlignment(Align.center)
        add(eraLabel).colspan(2).fillX().padBottom(6f).row()

        // --- Separator ---
        addSeparator()

        // --- Stats Rows ---
        val score = civInfo.getStatForRanking(RankingType.Score)
        addStatRow("OtherIcons/Score", Color.FIREBRICK, "Score", score.toString())

        val military = civInfo.getStatForRanking(RankingType.Force)
        addStatRow("OtherIcons/Shield", Color.SCARLET, "Military", military.toString())

        val territory = civInfo.getStatForRanking(RankingType.Territory)
        addStatRow("OtherIcons/Hexagon", Color.TAN, "Territory", territory.toString())

        val population = civInfo.getStatForRanking(RankingType.Population)
        addStatRow("StatIcons/Population", Color.WHITE, "Pop", population.toString())

        val cities = civInfo.cities.size
        addStatRow("OtherIcons/Cities", Color.CYAN, "Cities", cities.toString())

        // --- Collapse button ---
        addSeparator()
        val collapseLabel = "\u25C0".toLabel(dimLabel, 12)
        val collapseBtn = Table()
        collapseBtn.add(collapseLabel).pad(3f)
        collapseBtn.touchable = Touchable.enabled
        collapseBtn.onClick {
            isExpanded = false
            update(worldScreen.viewingCiv)
        }
        add(collapseBtn).colspan(2).center().row()
    } catch (_: Exception) { /* Prevent crash */ } }

    private fun addStatRow(iconPath: String, iconColor: Color, label: String, value: String) {
        val rowTable = Table()

        if (ImageGetter.imageExists(iconPath)) {
            val icon: Image = ImageGetter.getImage(iconPath)
            icon.setSize(14f)
            icon.color = iconColor
            rowTable.add(icon).size(14f).padRight(4f)
        }

        val nameLabel = label.toLabel(dimLabel, 11)
        rowTable.add(nameLabel).left().expandX()

        val valueLabel = value.toLabel(brightValue, 13)
        rowTable.add(valueLabel).right().padLeft(6f)

        add(rowTable).width(110f).fillX().colspan(2).row()
    }

    private fun addSeparator() {
        val sep = Table()
        sep.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/EmpireQuickStats/Separator",
            tintColor = Color(goldAccent.r, goldAccent.g, goldAccent.b, 0.3f)
        )
        add(sep).height(1f).width(100f).colspan(2).padTop(3f).padBottom(3f).row()
    }
}
