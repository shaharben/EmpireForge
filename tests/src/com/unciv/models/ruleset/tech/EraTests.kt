package com.unciv.models.ruleset.tech

import com.unciv.logic.civilization.Civilization
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.unique.GameContext
import com.unciv.testing.GdxTestRunner
import com.unciv.testing.TestGame
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/** Tests [Era][com.unciv.models.ruleset.tech.Era] */
@RunWith(GdxTestRunner::class)
class EraTests {
    private lateinit var game: TestGame
    private lateinit var civ: Civilization

    @Before
    fun initTheWorld() {
        game = TestGame()
        civ = game.addCiv()
    }

    /** Tests [Era.matchesFilter][com.unciv.models.ruleset.tech.Era.matchesFilter] */
    @Test
    fun testMatchesFilter() {
        setupModdedGame()
        val eraTests = hashMapOf(
            "Ruins Era" to listOf(
                "any era" to true,
                "Ruins Era" to true,
                "Reconstruction Era" to false,
                "Starting Era" to true,
                "pre-[Reconstruction Era]" to true,
                "pre-[Ruins Era]" to false,
                "post-[Ruins Era]" to false,
                "Invalid Filter" to false
            ),
            "Reconstruction Era" to listOf(
                "any era" to true,
                "Ruins Era" to false,
                "Reconstruction Era" to true,
                "Starting Era" to false,
                "post-[Ruins Era]" to true,
                "post-[Organization Era]" to true,
                "post-[Reconstruction Era]" to false,
                "pre-[Reconstruction Era]" to false,
                "pre-[Connection Era]" to true,
                "pre-[Organization Era]" to false,
                "pre-[Invalid era]" to false,
                "post-[Invalid era]" to false
            )
        )

        val state = GameContext(civ)
        for ((eraName, tests) in eraTests) {
            val era = game.ruleset.eras[eraName]
            assertNotNull(era)
            if (era != null) {
                for ((filter, expected) in tests) {
                    val actual = era.matchesFilter(filter, state)
                    assertEquals("Testing that `$era` matchesFilter `$filter`:", expected, actual)
                }
            }
        }
    }

    private fun setupModdedGame(): Ruleset {
        game = TestGame()
        game.makeHexagonalMap(3)
        civ = game.addCiv()
        return game.ruleset
    }
}
