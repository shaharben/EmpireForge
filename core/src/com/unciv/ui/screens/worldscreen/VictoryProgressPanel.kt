package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.MilestoneType
import com.unciv.models.ruleset.Victory
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.onClick
import com.unciv.ui.screens.basescreen.BaseScreen

/**
 * A toggleable panel displayed on the WorldScreen that shows the player's progress
 * toward each enabled victory type, using milestone-based tracking from the ruleset.
 */
class VictoryProgressPanel(private val worldScreen: WorldScreen) : Table() {
    private var isExpanded = false

    // Colors for each victory focus type
    private val scienceColor = Color(0.3f, 0.55f, 0.59f, 1f)
    private val cultureColor = Color(0.82f, 0.37f, 0.82f, 1f)
    private val dominationColor = Color(0.91f, 0.3f, 0.24f, 1f)
    private val diplomaticColor = Color(0.4f, 0.7f, 0.9f, 1f)
    private val defaultColor = Color(0.85f, 0.65f, 0.13f, 1f)
    private val headerColor = Color(0.85f, 0.65f, 0.13f, 1f)

    init {
        background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/VictoryProgressPanel",
            BaseScreen.skinStrings.roundedEdgeRectangleShape,
            Color(0.1f, 0.10f, 0.14f, 0.88f)
        )
        defaults().pad(4f)
        update(worldScreen.viewingCiv)
    }

    fun update(civInfo: Civilization) {
        clear()

        if (!isExpanded) {
            // Collapsed state: show a small trophy icon button
            val headerLabel = "\u2691".toLabel(headerColor, 18)
            val toggleButton = Table()
            toggleButton.add(headerLabel).pad(5f)
            toggleButton.touchable = Touchable.enabled
            toggleButton.onClick {
                isExpanded = true
                update(civInfo)
            }
            add(toggleButton)
            pack()
            return
        }

        // Header
        val headerLabel = "Victory Progress".toLabel(headerColor, 16)
        add(headerLabel).colspan(3).padBottom(6f).row()

        val enabledVictories = civInfo.gameInfo.getEnabledVictories()

        for ((victoryName, victory) in enabledVictories) {
            addVictorySection(victoryName, victory, civInfo)
        }

        // Collapse button at the bottom
        val collapseButton = Table()
        collapseButton.add("\u25B6".toLabel(Color.WHITE, 14)).pad(3f)
        collapseButton.touchable = Touchable.enabled
        collapseButton.onClick {
            isExpanded = false
            update(civInfo)
        }
        add(collapseButton).colspan(3).row()

        pack()
    }

    private fun addVictorySection(victoryName: String, victory: Victory, civInfo: Civilization) {
        val totalMilestones = victory.milestoneObjects.size
        if (totalMilestones == 0) return

        val completedMilestones = civInfo.victoryManager.amountMilestonesCompleted(victory)
        val overallProgress = completedMilestones.toFloat() / totalMilestones.toFloat()
        val barColor = getColorForVictory(victory, civInfo)

        // Victory name row with overall milestone progress
        addProgressRow(victoryName, completedMilestones, totalMilestones, barColor)

        // Show detail for the current (next incomplete) milestone
        val nextMilestone = civInfo.victoryManager.getNextMilestone(victory)
        if (nextMilestone != null) {
            val detailInfo = getMilestoneDetailProgress(nextMilestone.type, nextMilestone, victory, civInfo)
            if (detailInfo != null) {
                val (detailLabel, current, target) = detailInfo
                addDetailRow(detailLabel, current, target, barColor.cpy().lerp(Color.WHITE, 0.3f))
            }
        }
    }

    /** Returns a triple of (label, current, target) for the detailed sub-progress of a milestone, or null. */
    private fun getMilestoneDetailProgress(
        type: MilestoneType?,
        milestone: com.unciv.models.ruleset.Milestone,
        victory: Victory,
        civInfo: Civilization
    ): Triple<String, Int, Int>? {
        return when (type) {
            MilestoneType.AddedSSPartsInCapital -> {
                val required = victory.requiredSpaceshipPartsAsCounter.sumValues()
                val built = civInfo.victoryManager.currentsSpaceshipParts.sumValues()
                Triple("  Spaceship Parts", built, required)
            }
            MilestoneType.CaptureAllCapitals -> {
                val owned = civInfo.cities.count {
                    it.isOriginalCapital && (it.foundingCivObject?.isMajorCiv() == true)
                }
                val totalCapitals = civInfo.gameInfo.civilizations.count { it.isMajorCiv() }
                Triple("  Capitals", owned, totalCapitals)
            }
            MilestoneType.DestroyAllPlayers -> {
                val totalMajorCivs = civInfo.gameInfo.civilizations.count { it.isMajorCiv() } - 1
                val destroyed = totalMajorCivs - civInfo.gameInfo.getAliveMajorCivs().count { it != civInfo }
                Triple("  Civs Destroyed", destroyed, totalMajorCivs)
            }
            MilestoneType.CompletePolicyBranches -> {
                val required = milestone.params[0].toIntOrNull() ?: 0
                val completed = civInfo.getCompletedPolicyBranchesCount()
                Triple("  Branches", completed, required)
            }
            MilestoneType.WinDiplomaticVote -> {
                val won = if (civInfo.victoryManager.hasEverWonDiplomaticVote) 1 else 0
                Triple("  Diplomatic Vote", won, 1)
            }
            MilestoneType.ScoreAfterTimeOut -> {
                val currentTurn = civInfo.gameInfo.turns
                val maxTurns = civInfo.gameInfo.gameParameters.maxTurns
                Triple("  Turns", currentTurn, maxTurns)
            }
            MilestoneType.BuiltBuilding, MilestoneType.BuildingBuiltGlobally -> {
                val built = if (civInfo.cities.any { it.cityConstructions.isBuilt(milestone.params[0]) }) 1 else 0
                Triple("  ${milestone.params[0]}", built, 1)
            }
            MilestoneType.MoreCountableThanEachPlayer -> {
                val relevantCivs = civInfo.gameInfo.civilizations.filter {
                    milestone.getMoreCountableThanOtherCivRelevant(civInfo, it)
                }
                val beatCount = relevantCivs.count {
                    milestone.getMoreCountableThanOtherCivPercent(civInfo, it) > 100f
                }
                Triple("  Surpass Civs", beatCount, relevantCivs.size)
            }
            null -> null
        }
    }

    /** Determines a bar color based on the first milestone's focus type. */
    private fun getColorForVictory(victory: Victory, civInfo: Civilization): Color {
        val focus = victory.milestoneObjects.firstOrNull()?.getFocus(civInfo) ?: return defaultColor
        return when (focus) {
            Victory.Focus.Science -> scienceColor
            Victory.Focus.Culture -> cultureColor
            Victory.Focus.Military -> dominationColor
            Victory.Focus.CityStates -> diplomaticColor
            Victory.Focus.Production -> scienceColor
            Victory.Focus.Gold -> defaultColor
            Victory.Focus.Score -> defaultColor
        }
    }

    private fun addProgressRow(label: String, current: Int, target: Int, barColor: Color) {
        val pct = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
        val fractionText = "$current/$target"

        add(label.toLabel(Color(0.7f, 0.7f, 0.75f, 1f), 13)).left().padRight(8f)

        val barBg = Table()
        barBg.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/VictoryProgressBar",
            BaseScreen.skinStrings.roundedEdgeRectangleSmallShape,
            Color(0.2f, 0.2f, 0.25f, 1f)
        )
        val barFill = Table()
        barFill.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/VictoryProgressBarFill",
            BaseScreen.skinStrings.roundedEdgeRectangleSmallShape,
            barColor
        )
        barBg.add(barFill).width(80f * pct).height(10f).left()
        barBg.add().width(80f * (1f - pct)).height(10f)
        add(barBg).width(80f).height(10f).padRight(4f)

        add(fractionText.toLabel(Color.WHITE, 12)).right().row()
    }

    /** A smaller detail row indented under the main victory row, showing sub-milestone progress. */
    private fun addDetailRow(label: String, current: Int, target: Int, barColor: Color) {
        val pct = (current.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
        val fractionText = "$current/$target"

        add(label.toLabel(Color(0.55f, 0.55f, 0.6f, 1f), 11)).left().padRight(6f)

        val barBg = Table()
        barBg.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/VictoryProgressBar",
            BaseScreen.skinStrings.roundedEdgeRectangleSmallShape,
            Color(0.2f, 0.2f, 0.25f, 1f)
        )
        val barFill = Table()
        barFill.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/VictoryProgressBarFill",
            BaseScreen.skinStrings.roundedEdgeRectangleSmallShape,
            barColor
        )
        barBg.add(barFill).width(60f * pct).height(7f).left()
        barBg.add().width(60f * (1f - pct)).height(7f)
        add(barBg).width(60f).height(7f).padRight(4f)

        add(fractionText.toLabel(Color(0.8f, 0.8f, 0.8f, 1f), 10)).right().row()
    }
}
