package com.unciv.logic.civilization

import com.unciv.UncivGame

/**
 * Defines all available achievements with display name, description, and the condition check.
 */
enum class Achievement(
    val displayName: String,
    val description: String,
    val iconPath: String,
    val maxProgress: Int = 1
) {
    FIRST_CITY("Base Camp", "Established your first survivor settlement", "ImprovementIcons/City center", 1),
    FIVE_CITIES("Survival Network", "Established 5 settlements across the wasteland", "ImprovementIcons/City center", 5),
    FIRST_WAR("First Contact Gone Wrong", "Entered armed conflict with another faction", "OtherIcons/Pillage", 1),
    FIRST_WONDER("Landmark Restored", "Rebuilt a legendary structure from the old world", "OtherIcons/Wonders", 1),
    TECH_LEADER("Knowledge Recovered", "Rediscovered 20 lost technologies", "StatIcons/Science", 20),
    MILITARY_MIGHT("Armed and Ready", "Assembled a force of 10 combat units", "StatIcons/Strength", 10),
    TRADE_MASTER("Supply Chain", "Established 5 trade agreements with other factions", "StatIcons/Gold", 5),
    GOLDEN_AGE("Golden Era", "Your settlements entered a period of prosperity", "StatIcons/Happiness", 1),
    SURVIVE_100("Wasteland Veteran", "Survived 100 turns in the wasteland", "OtherIcons/Turn", 100),
    VICTORY("World Rebuilt", "Led your faction to ultimate victory", "OtherIcons/Checkmark", 1);
}

/**
 * Manages achievement tracking and persistence across games.
 * Achievements are stored in [GameSettings] so they persist independently of any single game save.
 */
class AchievementManager {

    /**
     * Checks all achievement conditions for the given civilization and returns
     * a list of newly unlocked achievements (achievements that were just earned this check).
     *
     * @param civ The civilization to check achievements for. Should be a human player.
     * @return List of [Achievement]s that were newly unlocked during this check.
     */
    fun checkAchievements(civ: Civilization): List<Achievement> {
        return try {
            val settings = UncivGame.Current.settings
            val unlocked = settings.unlockedAchievements
            val newlyUnlocked = mutableListOf<Achievement>()

            for (achievement in Achievement.entries) {
                if (achievement.name in unlocked) continue
                if (isAchieved(achievement, civ)) {
                    unlocked.add(achievement.name)
                    newlyUnlocked.add(achievement)
                }
            }

            if (newlyUnlocked.isNotEmpty()) {
                settings.save()
            }

            newlyUnlocked
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the current progress value for an achievement given the civilization state.
     */
    fun getProgress(achievement: Achievement, civ: Civilization): Int {
        return try {
            when (achievement) {
                Achievement.FIRST_CITY -> civ.cities.size.coerceAtMost(1)
                Achievement.FIVE_CITIES -> civ.cities.size.coerceAtMost(5)
                Achievement.FIRST_WAR -> if (civ.isAtWar()) 1 else 0
                Achievement.FIRST_WONDER -> getWonderCount(civ).coerceAtMost(1)
                Achievement.TECH_LEADER -> civ.tech.getNumberOfTechsResearched().coerceAtMost(20)
                Achievement.MILITARY_MIGHT -> civ.units.getCivUnits().count { it.isMilitary() }.coerceAtMost(10)
                Achievement.TRADE_MASTER -> civ.diplomacy.values.count { it.hasOpenBorders }.coerceAtMost(5)
                Achievement.GOLDEN_AGE -> if (civ.goldenAges.isGoldenAge()) 1 else 0
                Achievement.SURVIVE_100 -> civ.gameInfo.turns.coerceAtMost(100)
                Achievement.VICTORY -> if (civ.victoryManager.hasWon()) 1 else 0
            }
        } catch (_: Exception) {
            0
        }
    }

    private fun isAchieved(achievement: Achievement, civ: Civilization): Boolean {
        return getProgress(achievement, civ) >= achievement.maxProgress
    }

    private fun getWonderCount(civ: Civilization): Int {
        return civ.cities.sumOf { city ->
            city.cityConstructions.getBuiltBuildings().count { it.isWonder }
        }
    }

    companion object {
        fun isUnlocked(achievement: Achievement): Boolean {
            return achievement.name in UncivGame.Current.settings.unlockedAchievements
        }
    }
}
