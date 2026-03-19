package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.city.City
import com.unciv.models.ruleset.PerpetualConstruction
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.widgets.AutoScrollPane as ScrollPane
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.basescreen.BaseScreen

/**
 * A collapsible panel at the bottom-left of the WorldScreen showing
 * each city's current production, turns remaining, and a progress bar.
 * Idle cities (empty queue) are highlighted and sorted first.
 */
class CityProductionPanel(private val worldScreen: WorldScreen) : Table() {

    private var isExpanded = true

    private val panelWidth = 220f
    private val maxVisibleCities = 6
    private val rowHeight = 38f

    // Colors
    private val headerColor = Color(0.85f, 0.72f, 0.20f, 1f)
    private val dimLabel = Color(0.7f, 0.7f, 0.75f, 1f)
    private val idleColor = Color(1f, 0.45f, 0.2f, 1f)
    private val progressBg = Color(0.2f, 0.2f, 0.25f, 1f)
    private val progressFill = Color(0.3f, 0.75f, 0.35f, 1f)

    init {
        background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/CityProductionPanel",
            BaseScreen.skinStrings.roundedEdgeRectangleShape,
            Color(0.08f, 0.09f, 0.12f, 0.88f)
        )
        defaults().pad(2f)
        update(worldScreen.viewingCiv)
    }

    fun update(civInfo: Civilization) {
        clear()

        if (!isExpanded) {
            buildCollapsedView()
            pack()
            return
        }

        buildExpandedView(civInfo)
        pack()
    }

    private fun buildCollapsedView() {
        val expandLabel = "\u25B2 Cities".toLabel(headerColor, 12)
        val btn = Table()
        btn.add(expandLabel).pad(4f)
        btn.touchable = Touchable.enabled
        btn.onClick {
            isExpanded = true
            update(worldScreen.selectedCiv)
        }
        add(btn).width(panelWidth).row()
    }

    private fun buildExpandedView(civInfo: Civilization) {
        // Header row with collapse toggle
        val headerTable = Table()
        val headerLabel = "Cities".toLabel(headerColor, 14)
        headerTable.add(headerLabel).expandX().left().padLeft(4f)

        val collapseLabel = "\u25BC".toLabel(dimLabel, 12)
        val collapseBtn = Table()
        collapseBtn.add(collapseLabel).pad(2f)
        collapseBtn.touchable = Touchable.enabled
        collapseBtn.onClick {
            isExpanded = false
            update(worldScreen.selectedCiv)
        }
        headerTable.add(collapseBtn).right().padRight(4f)
        add(headerTable).width(panelWidth).fillX().row()

        // Separator
        addSeparator()

        val cities = civInfo.cities.toList()
        if (cities.isEmpty()) {
            val noCities = "No cities".toLabel(dimLabel, 11)
            noCities.setAlignment(Align.center)
            add(noCities).width(panelWidth).pad(6f).row()
            return
        }

        // Sort: idle first, then by turns remaining ascending
        val sorted = cities.sortedWith(compareBy<City> { city ->
            val name = city.cityConstructions.currentConstructionName()
            if (name.isEmpty()) 0 else 1
        }.thenBy { city ->
            val name = city.cityConstructions.currentConstructionName()
            if (name.isEmpty()) 0
            else try { city.cityConstructions.turnsToConstruction(name) } catch (_: Exception) { Int.MAX_VALUE }
        })

        // Build city rows inside a scrollable container
        val cityListTable = Table()
        cityListTable.defaults().pad(1f)

        for (city in sorted) {
            cityListTable.add(buildCityRow(city)).width(panelWidth - 10f).fillX().row()
        }

        val scrollHeight = (minOf(sorted.size, maxVisibleCities) * rowHeight).coerceAtLeast(rowHeight)
        val scrollPane = ScrollPane(cityListTable)
        scrollPane.setScrollingDisabled(true, false)
        scrollPane.setOverscroll(false, false)
        add(scrollPane).width(panelWidth).height(scrollHeight).row()
    }

    private fun buildCityRow(city: City): Table {
        val row = Table()
        row.defaults().pad(1f)

        val constructions = city.cityConstructions
        val currentName = constructions.currentConstructionName()
        val isIdle = currentName.isEmpty()

        // City name - clickable to center map
        val cityNameLabel = city.name.toLabel(Color.WHITE, 11)
        cityNameLabel.setAlignment(Align.left)
        val cityNameBtn = Table()
        cityNameBtn.add(cityNameLabel).left()
        cityNameBtn.touchable = Touchable.enabled
        cityNameBtn.onClick {
            worldScreen.mapHolder.setCenterPosition(city.location)
        }

        row.add(cityNameBtn).left().expandX().fillX().colspan(3).row()

        if (isIdle) {
            val idleLabel = "Idle".toLabel(idleColor, 11)
            row.add(idleLabel).left().colspan(3).padLeft(2f).row()
        } else {
            // Production icon + name + turns
            val infoRow = Table()
            infoRow.defaults().pad(1f)

            // Construction icon
            try {
                val icon = ImageGetter.getConstructionPortrait(currentName, 20f)
                infoRow.add(icon).size(20f).padRight(3f)
            } catch (_: Exception) {
                // If icon not found, skip it
            }

            // Construction name (truncated if needed)
            val displayName = if (currentName.length > 14) currentName.take(13) + ".." else currentName
            val nameLabel = displayName.toLabel(dimLabel, 10)
            infoRow.add(nameLabel).left().expandX()

            // Turns remaining
            val isPerpetual = PerpetualConstruction.isNamePerpetual(currentName)
            val turnsText = if (isPerpetual) {
                "\u221E"
            } else {
                val turns = try { constructions.turnsToConstruction(currentName) } catch (_: Exception) { -1 }
                if (turns >= 0) "${turns}t" else "?t"
            }
            val turnsLabel = turnsText.toLabel(headerColor, 10)
            infoRow.add(turnsLabel).right().padLeft(3f)

            row.add(infoRow).fillX().expandX().colspan(3).row()

            // Progress bar (only for non-perpetual constructions)
            if (!isPerpetual) {
                val workDone = constructions.getWorkDone(currentName).toFloat()
                val totalWork = (workDone + constructions.getRemainingWork(currentName).toFloat()).coerceAtLeast(1f)
                val progress = (workDone / totalWork).coerceIn(0f, 1f)

                val barTable = Table()
                // Background bar
                val bgBar = Table()
                bgBar.background = BaseScreen.skinStrings.getUiBackground(
                    "WorldScreen/CityProductionPanel/ProgressBg",
                    tintColor = progressBg
                )

                // Fill bar
                val fillBar = Table()
                fillBar.background = BaseScreen.skinStrings.getUiBackground(
                    "WorldScreen/CityProductionPanel/ProgressFill",
                    tintColor = progressFill
                )

                val barWidth = panelWidth - 20f
                bgBar.add(fillBar).width(barWidth * progress).height(4f).left()
                // Fill remaining space
                if (progress < 1f) {
                    bgBar.add().width(barWidth * (1f - progress)).height(4f)
                }

                barTable.add(bgBar).width(barWidth).height(4f).left()
                row.add(barTable).fillX().colspan(3).padTop(0f).padBottom(2f).row()
            }
        }

        return row
    }

    private fun addSeparator() {
        val sep = Table()
        sep.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/CityProductionPanel/Separator",
            tintColor = Color(headerColor.r, headerColor.g, headerColor.b, 0.3f)
        )
        add(sep).height(1f).width(panelWidth - 20f).padTop(2f).padBottom(2f).row()
    }
}
