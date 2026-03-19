package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.UncivGame
import com.unciv.logic.civilization.Achievement
import com.unciv.logic.civilization.AchievementManager
import com.unciv.logic.civilization.Civilization
import com.unciv.ui.components.extensions.addSeparator
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.popups.Popup
import com.unciv.ui.screens.basescreen.BaseScreen

/**
 * Popup that displays all achievements, showing unlocked ones highlighted in gold
 * and locked ones grayed out, with progress indicators where applicable.
 */
class AchievementPopup(
    worldScreen: WorldScreen,
    private val civ: Civilization
) : Popup(worldScreen) {

    private val achievementManager = AchievementManager()
    private val unlockedSet = UncivGame.Current.settings.unlockedAchievements

    init {
        val unlockedCount = Achievement.entries.count { it.name in unlockedSet }
        val totalCount = Achievement.entries.size

        addGoodSizedLabel("Achievements ($unlockedCount/$totalCount)").colspan(4).center().row()
        addSeparator()

        for (achievement in Achievement.entries) {
            val isUnlocked = achievement.name in unlockedSet
            addAchievementRow(achievement, isUnlocked)
        }

        addSeparator()
        addCloseButton()
    }

    private fun addAchievementRow(achievement: Achievement, isUnlocked: Boolean) {
        val rowTable = Table()
        val tintColor = if (isUnlocked) Color.WHITE else Color.GRAY

        // Icon
        val icon = if (ImageGetter.imageExists(achievement.iconPath))
            ImageGetter.getImage(achievement.iconPath)
        else
            ImageGetter.getCircle(tintColor, 30f)
        icon.color = tintColor
        rowTable.add(icon).size(30f).padRight(10f)

        // Name and description column
        val textTable = Table()
        val nameColor = if (isUnlocked) Color.GOLD else Color.GRAY
        val nameLabel = achievement.displayName.toLabel(nameColor, 18)
        nameLabel.setAlignment(Align.left)
        textTable.add(nameLabel).left().row()

        val descColor = if (isUnlocked) Color.WHITE else Color.DARK_GRAY
        val descLabel = achievement.description.toLabel(descColor, 14)
        descLabel.setAlignment(Align.left)
        textTable.add(descLabel).left()

        rowTable.add(textTable).expandX().fillX().padRight(10f)

        // Progress indicator
        val progress = achievementManager.getProgress(achievement, civ)
        val maxProgress = achievement.maxProgress
        val progressText = if (isUnlocked) {
            "Complete"
        } else {
            "$progress/$maxProgress"
        }
        val progressColor = if (isUnlocked) Color.GREEN else Color.LIGHT_GRAY
        val progressLabel = progressText.toLabel(progressColor, 14)
        progressLabel.setAlignment(Align.right)
        rowTable.add(progressLabel).right().padRight(5f)

        // Checkmark for unlocked
        if (isUnlocked) {
            val check = ImageGetter.getImage("OtherIcons/Checkmark")
            check.color = Color.GREEN
            rowTable.add(check).size(20f).padLeft(5f)
        } else {
            rowTable.add().size(20f).padLeft(5f)
        }

        addGoodSizedLabel("").row() // spacer
        add(rowTable).width(stageToShowOn.width * 0.7f).pad(4f).row()
    }
}
