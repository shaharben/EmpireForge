package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.graphics.Color
import com.unciv.Constants
import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.PerpetualConstruction
import com.unciv.models.translations.tr
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.fonts.Fonts
import com.unciv.ui.popups.Popup
import kotlin.math.roundToInt

/**
 * A popup that shows a summary of what will happen when the player ends their turn.
 * Displays income, city production, military status, and warnings.
 */
class TurnSummaryPopup(worldScreen: WorldScreen) : Popup(worldScreen, Scrollability.WithoutButtons) {

    init {
        val civ = worldScreen.viewingCiv

        addGoodSizedLabel("Turn Summary Preview", Constants.headingFontSize).colspan(2).center()
        row()
        addSeparator(Color.GOLD)

        addIncomeSection(civ)
        addSeparator(Color.GOLD)

        addCityProductionSection(civ)
        addSeparator(Color.GOLD)

        addMilitarySection(civ)
        addSeparator(Color.GOLD)

        addWarningsSection(civ)

        addCloseButton()
    }

    private fun addIncomeSection(civ: Civilization) {
        addSectionHeader("Income Summary")

        val stats = civ.stats.statsForNextTurn
        val goldPerTurn = stats.gold.roundToInt()
        val currentGold = civ.gold
        val nextTurnGold = currentGold + goldPerTurn
        val goldSign = if (goldPerTurn >= 0) "+" else ""
        addStatLine(
            "${Fonts.gold} Gold: $goldSign$goldPerTurn/turn ($currentGold → $nextTurnGold)"
            , if (goldPerTurn >= 0) Color.WHITE else Color.RED
        )

        // Science
        val sciencePerTurn = stats.science.roundToInt()
        val techName = civ.tech.currentTechnologyName()
        if (techName != null) {
            val currentProgress = civ.tech.researchOfTech(techName)
            val totalCost = civ.tech.costOfTech(techName)
            val turnsRemaining = civ.tech.turnsToTech(techName)
            addStatLine("${Fonts.science} Science: +$sciencePerTurn/turn — [$techName] ($currentProgress/$totalCost${Fonts.science}, $turnsRemaining${Fonts.turn})")
        } else {
            addStatLine("${Fonts.science} Science: +$sciencePerTurn/turn — No research selected", Color.LIGHT_GRAY)
        }

        // Culture
        val culturePerTurn = stats.culture.roundToInt()
        val storedCulture = civ.policies.storedCulture
        val cultureNeeded = civ.policies.getCultureNeededForNextPolicy()
        addStatLine("${Fonts.culture} Culture: +$culturePerTurn/turn ($storedCulture/$cultureNeeded)")

    }

    private fun addCityProductionSection(civ: Civilization) {
        addSectionHeader("City Production")

        var hasContent = false

        // Cities completing production this turn
        for (city in civ.cities) {
            val constructionName = city.cityConstructions.currentConstructionName()
            if (constructionName.isEmpty()) continue
            val construction = city.cityConstructions.getCurrentConstruction()
            if (construction is PerpetualConstruction) continue

            val turnsLeft = city.cityConstructions.turnsToConstruction(constructionName)
            if (turnsLeft <= 1) {
                addStatLine("${Fonts.production} [${city.name}]: [$constructionName] completes!", Color.GREEN)
                hasContent = true
            }
        }

        // Cities that will grow this turn
        for (city in civ.cities) {
            val turnsToGrow = city.population.getNumTurnsToNewPopulation()
            if (turnsToGrow != null && turnsToGrow <= 1) {
                val newPop = city.population.population + 1
                addStatLine("${Fonts.food} [${city.name}]: grows to population $newPop", Color.GREEN)
                hasContent = true
            }
            // Starvation warning
            val turnsToStarve = city.population.getNumTurnsToStarvation()
            if (turnsToStarve != null && turnsToStarve <= 1) {
                addStatLine("${Fonts.food} [${city.name}]: loses population from starvation!", Color.RED)
                hasContent = true
            }
        }

        if (!hasContent) {
            addStatLine("No cities completing production or growing this turn.", Color.LIGHT_GRAY)
        }
    }

    private fun addMilitarySection(civ: Civilization) {
        addSectionHeader("Military")

        val idleUnits = civ.units.getDueUnits().count()
        if (idleUnits > 0) {
            addStatLine("$idleUnits unit(s) awaiting orders", Color.YELLOW)
        }

        var healingCount = 0
        for (unit in civ.units.getCivUnits()) {
            if (unit.health < 100) {
                try {
                    val tile = unit.getTile()
                    val owner = tile.getOwner()
                    if (owner == civ) {
                        healingCount++
                    }
                } catch (_: Exception) {
                    // Unit may not have a valid tile (e.g. embarking, being transported)
                }
            }
        }
        if (healingCount > 0) {
            addStatLine("$healingCount damaged unit(s) healing in friendly territory")
        }

        if (idleUnits == 0 && healingCount == 0) {
            addStatLine("All units have orders.", Color.LIGHT_GRAY)
        }
    }

    private fun addWarningsSection(civ: Civilization) {
        addSectionHeader("Warnings")

        var hasWarnings = false

        // Negative gold
        val goldPerTurn = civ.stats.statsForNextTurn.gold.roundToInt()
        val nextTurnGold = civ.gold + goldPerTurn
        if (nextTurnGold < 0) {
            addStatLine("Negative gold balance next turn ($nextTurnGold${Fonts.gold})!", Color.RED)
            hasWarnings = true
        }

        // Cities without production
        val citiesNoProduction = civ.cities.filter {
            !it.isPuppet && it.cityConstructions.currentConstructionName().isEmpty()
        }
        if (citiesNoProduction.isNotEmpty()) {
            for (city in citiesNoProduction) {
                addStatLine("[${city.name}] has no production queued!", Color.CORAL)
            }
            hasWarnings = true
        }

        // No research
        if (civ.tech.currentTechnologyName() == null && civ.cities.isNotEmpty()) {
            addStatLine("No technology being researched!", Color.CORAL)
            hasWarnings = true
        }

        if (!hasWarnings) {
            addStatLine("No warnings.", Color.LIGHT_GRAY)
        }
    }

    // Helper: adds a gold-colored section header
    private fun addSectionHeader(text: String) {
        row()
        add(text.toLabel(fontColor = Color.GOLD, fontSize = Constants.defaultFontSize + 2).apply {
            wrap = true
        }).colspan(2).pad(8f, 0f, 4f, 0f).fillX().left()
        row()
    }

    // Helper: adds an indented stat line
    private fun addStatLine(text: String, color: Color = Color.WHITE) {
        row()
        add(text.tr().toLabel(fontColor = color).apply {
            wrap = true
        }).colspan(2).pad(2f, 16f, 2f, 0f).width(stageToShowOn.width * 0.55f).left()
        row()
    }
}
