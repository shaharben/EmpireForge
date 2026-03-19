package com.unciv.ui.screens.worldscreen

import com.unciv.logic.civilization.Civilization

data class Achievement(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val checkCondition: (Civilization) -> Boolean
)

class AchievementManager {
    private val achievements = listOf(
        Achievement("first_city", "City Founder", "Found your first city", "\uD83C\uDFD9", { it.cities.size >= 1 }),
        Achievement("five_cities", "Empire Builder", "Control 5 cities", "\uD83C\uDFD7", { it.cities.size >= 5 }),
        Achievement("ten_cities", "Continental Power", "Control 10 cities", "\uD83C\uDF0D", { it.cities.size >= 10 }),
        Achievement("pop_10", "Growing Nation", "Reach 10 total population", "\uD83D\uDC65", { civ -> civ.cities.sumOf { it.population.population } >= 10 }),
        Achievement("pop_50", "Populous Empire", "Reach 50 total population", "\uD83C\uDFDB", { civ -> civ.cities.sumOf { it.population.population } >= 50 }),
        Achievement("pop_100", "Mega Nation", "Reach 100 total population", "\uD83C\uDF1F", { civ -> civ.cities.sumOf { it.population.population } >= 100 }),
        Achievement("first_wonder", "Wonder Builder", "Build your first wonder", "\u2B50", { civ -> civ.cities.any { city -> city.cityConstructions.builtBuildings.any { civ.gameInfo.ruleset.buildings[it]?.isWonder == true } } }),
        Achievement("military_100", "Military Might", "Reach 100 military strength", "\u2694", { it.getStatForRanking(com.unciv.ui.screens.victoryscreen.RankingType.Force) >= 100 }),
        Achievement("military_500", "War Machine", "Reach 500 military strength", "\uD83D\uDEE1", { it.getStatForRanking(com.unciv.ui.screens.victoryscreen.RankingType.Force) >= 500 }),
        Achievement("gold_500", "Wealthy Nation", "Accumulate 500 gold", "\uD83D\uDCB0", { it.gold >= 500 }),
        Achievement("gold_2000", "Golden Treasury", "Accumulate 2000 gold", "\uD83D\uDC8E", { it.gold >= 2000 }),
        Achievement("tech_10", "Scholar", "Research 10 technologies", "\uD83D\uDD2C", { it.getStatForRanking(com.unciv.ui.screens.victoryscreen.RankingType.Technologies) >= 10 }),
        Achievement("tech_30", "Enlightened", "Research 30 technologies", "\uD83D\uDCDA", { it.getStatForRanking(com.unciv.ui.screens.victoryscreen.RankingType.Technologies) >= 30 }),
        Achievement("score_500", "Rising Power", "Reach a score of 500", "\uD83C\uDFC6", { it.getStatForRanking(com.unciv.ui.screens.victoryscreen.RankingType.Score) >= 500 }),
        Achievement("score_1000", "Great Empire", "Reach a score of 1000", "\uD83D\uDC51", { it.getStatForRanking(com.unciv.ui.screens.victoryscreen.RankingType.Score) >= 1000 }),
        Achievement("golden_age", "Golden Age", "Enter a Golden Age", "\u2600", { it.goldenAges.isGoldenAge() })
    )

    private val unlockedAchievements = mutableSetOf<String>()

    fun checkAchievements(civInfo: Civilization): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        for (achievement in achievements) {
            if (achievement.id !in unlockedAchievements) {
                try {
                    if (achievement.checkCondition(civInfo)) {
                        unlockedAchievements.add(achievement.id)
                        newlyUnlocked.add(achievement)
                    }
                } catch (_: Exception) { }
            }
        }
        return newlyUnlocked
    }

    fun getAll(): List<Achievement> = achievements
    fun isUnlocked(id: String): Boolean = id in unlockedAchievements
    fun getUnlockedCount(): Int = unlockedAchievements.size
    fun getTotalCount(): Int = achievements.size
}
