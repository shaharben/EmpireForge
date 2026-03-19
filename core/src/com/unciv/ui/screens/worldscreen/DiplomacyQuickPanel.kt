package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.logic.civilization.Civilization
import com.unciv.logic.civilization.diplomacy.DiplomaticStatus
import com.unciv.logic.civilization.diplomacy.RelationshipLevel
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.input.onClick
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.screens.basescreen.BaseScreen

/**
 * A compact, collapsible panel displayed on the right side of the WorldScreen
 * showing diplomatic relations at a glance for all known civilizations.
 */
class DiplomacyQuickPanel(private val worldScreen: WorldScreen) : Table() {
    private var isExpanded = true

    private val panelWidth = 180f
    private val maxVisibleEntries = 8
    private val entryHeight = 30f

    private val headerColor = Color(0.85f, 0.72f, 0.20f, 1f)
    private val dimLabel = Color(0.7f, 0.7f, 0.75f, 1f)

    // Relationship indicator colors
    private val colorAllied = Color(0.2f, 1f, 0.2f, 1f)
    private val colorFriendly = Color(0.3f, 0.8f, 0.3f, 1f)
    private val colorNeutral = Color(0.9f, 0.9f, 0.3f, 1f)
    private val colorHostile = Color(0.9f, 0.5f, 0.1f, 1f)
    private val colorWar = Color(0.9f, 0.15f, 0.15f, 1f)
    private val colorPeaceTreaty = Color(0.3f, 0.6f, 0.9f, 1f)

    init {
        background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/DiplomacyQuickPanel",
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
        val expandLabel = "\u25C0 Diplomacy".toLabel(headerColor, 12)
        val btn = Table()
        btn.add(expandLabel).pad(4f)
        btn.touchable = Touchable.enabled
        btn.onClick {
            isExpanded = true
            update(worldScreen.viewingCiv)
        }
        add(btn).row()
    }

    private fun buildExpandedView(civInfo: Civilization) {
        // --- Header ---
        val headerRow = Table()
        val titleLabel = "Diplomacy".toLabel(headerColor, 14)
        headerRow.add(titleLabel).expandX().left().padLeft(4f)

        val collapseLabel = "\u25B6".toLabel(dimLabel, 12)
        val collapseBtn = Table()
        collapseBtn.add(collapseLabel).pad(2f)
        collapseBtn.touchable = Touchable.enabled
        collapseBtn.onClick {
            isExpanded = false
            update(worldScreen.viewingCiv)
        }
        headerRow.add(collapseBtn).right().padRight(4f)
        add(headerRow).width(panelWidth).fillX().row()

        // --- Separator ---
        addSeparator()

        // --- Civ entries in a scrollable list ---
        val knownCivs = civInfo.getKnownCivs()
            .filter { !it.isBarbarian && civInfo.diplomacy.containsKey(it.civName) }
            .sortedWith(compareBy<Civilization> { civInfo.diplomacy[it.civName]?.diplomaticStatus != DiplomaticStatus.War }
                .thenBy { it.isCityState }
                .thenBy { it.civName })
            .toList()

        if (knownCivs.isEmpty()) {
            val noInfo = "No contacts".toLabel(dimLabel, 11)
            noInfo.setAlignment(Align.center)
            add(noInfo).width(panelWidth).pad(6f).row()
            return
        }

        val entriesTable = Table()
        entriesTable.defaults().pad(1f)

        for (otherCiv in knownCivs) {
            val diplomacy = civInfo.diplomacy[otherCiv.civName] ?: continue
            val dipStatus = diplomacy.diplomaticStatus
            val relLevel = diplomacy.relationshipLevel()

            val statusInfo = getStatusInfo(dipStatus, relLevel, otherCiv.isCityState)

            val entryRow = Table()

            // Nation portrait
            val portrait = ImageGetter.getNationPortrait(otherCiv.nation, 24f)
            entryRow.add(portrait).size(24f).padRight(4f)

            // Civ name (truncated)
            val displayName = if (otherCiv.civName.length > 10)
                otherCiv.civName.take(9) + "\u2026" else otherCiv.civName
            val nameLabel = displayName.toLabel(Color.WHITE, 11)
            entryRow.add(nameLabel).left().expandX().padRight(2f)

            // Color indicator dot
            val dotLabel = "\u25CF".toLabel(statusInfo.color, 12)
            entryRow.add(dotLabel).padRight(3f)

            // Status text
            val statusLabel = statusInfo.text.toLabel(statusInfo.color, 10)
            entryRow.add(statusLabel).right().minWidth(40f)

            entriesTable.add(entryRow).width(panelWidth - 12f).fillX().row()
        }

        val scrollHeight = (minOf(knownCivs.size, maxVisibleEntries) * entryHeight)
            .coerceAtMost(maxVisibleEntries * entryHeight)
        val scrollPane = ScrollPane(entriesTable)
        scrollPane.setOverscroll(false, false)
        scrollPane.setScrollingDisabled(true, false)
        add(scrollPane).width(panelWidth).height(scrollHeight).row()
    }

    private data class StatusInfo(val text: String, val color: Color)

    private fun getStatusInfo(
        dipStatus: DiplomaticStatus,
        relLevel: RelationshipLevel,
        isCityState: Boolean
    ): StatusInfo {
        // War takes precedence
        if (dipStatus == DiplomaticStatus.War)
            return StatusInfo("War", colorWar)

        // Defensive pact
        if (dipStatus == DiplomaticStatus.DefensivePact)
            return StatusInfo("Pact", colorPeaceTreaty)

        // City-state ally
        if (isCityState && relLevel == RelationshipLevel.Ally)
            return StatusInfo("Allied", colorAllied)

        // Map relationship level to display
        return when (relLevel) {
            RelationshipLevel.Ally -> StatusInfo("Ally", colorAllied)
            RelationshipLevel.Friend -> StatusInfo("Friend", colorFriendly)
            RelationshipLevel.Favorable -> StatusInfo("Favorable", colorFriendly)
            RelationshipLevel.Neutral -> StatusInfo("Neutral", colorNeutral)
            RelationshipLevel.Competitor -> StatusInfo("Rival", colorNeutral)
            RelationshipLevel.Afraid -> StatusInfo("Afraid", colorHostile)
            RelationshipLevel.Enemy -> StatusInfo("Hostile", colorHostile)
            RelationshipLevel.Unforgivable -> StatusInfo("Hostile", colorWar)
        }
    }

    private fun addSeparator() {
        val sep = Table()
        sep.background = BaseScreen.skinStrings.getUiBackground(
            "WorldScreen/DiplomacyQuickPanel/Separator",
            tintColor = Color(headerColor.r, headerColor.g, headerColor.b, 0.3f)
        )
        add(sep).height(1f).width(panelWidth - 20f).padTop(2f).padBottom(2f).row()
    }
}
