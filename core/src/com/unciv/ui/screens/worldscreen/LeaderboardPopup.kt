package com.unciv.ui.screens.worldscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.unciv.GUI
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.extensions.toTextButton
import com.unciv.ui.components.input.onClick
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.popups.Popup
import com.unciv.ui.popups.ToastPopup
import java.util.UUID

class LeaderboardPopup(
    private val screen: BaseScreen,
    private val leaderboardService: LeaderboardService
) : Popup(screen) {

    private val goldColor = Color(0.85f, 0.65f, 0.13f, 1f)
    private val silverColor = Color(0.75f, 0.75f, 0.78f, 1f)
    private val bronzeColor = Color(0.80f, 0.50f, 0.20f, 1f)
    private val headerColor = Color(0.7f, 0.7f, 0.75f, 1f)

    init {
        add("Leaderboard".toLabel(goldColor, 22)).colspan(4).padBottom(15f).row()

        val loadingLabel = "Loading...".toLabel(Color.WHITE, 16)
        add(loadingLabel).colspan(4).row()

        val headerTable = Table()
        headerTable.add("#".toLabel(headerColor, 14)).width(40f).left()
        headerTable.add("Player".toLabel(headerColor, 14)).width(200f).left()
        headerTable.add("Score".toLabel(headerColor, 14)).width(80f).right()
        headerTable.add("Games".toLabel(headerColor, 14)).width(80f).right()

        val contentTable = Table()
        val scrollPane = ScrollPane(contentTable)
        scrollPane.setScrollingDisabled(true, false)

        try {
            leaderboardService.getLeaderboard { entries ->
                Gdx.app.postRunnable {
                    try {
                        loadingLabel.remove()

                        add(headerTable).colspan(4).padBottom(8f).row()
                        add(scrollPane).colspan(4).width(400f).maxHeight(350f).row()

                        for (entry in entries.take(50)) {
                            val rankColor = when (entry.rank) {
                                1 -> goldColor
                                2 -> silverColor
                                3 -> bronzeColor
                                else -> Color.WHITE
                            }
                            contentTable.add(entry.rank.toString().toLabel(rankColor, 14)).width(40f).left()
                            contentTable.add(entry.displayName.toLabel(Color.WHITE, 14)).width(200f).left()
                            contentTable.add(entry.score.toString().toLabel(goldColor, 14)).width(80f).right()
                            contentTable.add(entry.totalGames.toString().toLabel(headerColor, 14)).width(80f).right()
                            contentTable.row().padTop(4f)
                        }

                        if (entries.isEmpty()) {
                            contentTable.add("No entries yet".toLabel(headerColor, 14)).colspan(4)
                        }

                        pack()
                    } catch (_: Exception) {
                        loadingLabel.setText("Error displaying leaderboard")
                    }
                }
            }
        } catch (_: Exception) {
            Gdx.app.postRunnable {
                loadingLabel.setText("Failed to load leaderboard")
            }
        }

        // Submit Score button - only enabled if a game is loaded
        if (GUI.isWorldLoaded()) {
            val submitButton = "Submit Score".toTextButton()
            submitButton.onClick {
                submitCurrentGameScore()
            }
            add(submitButton).colspan(4).padTop(15f).padBottom(5f).row()
        }

        addCloseButton()
        pack()
    }

    /** Calculate score: turns survived * difficulty multiplier + cities founded * 100 */
    private fun calculateScore(): Int {
        val worldScreen = GUI.getWorldScreen()
        val gameInfo = worldScreen.gameInfo
        val playerCiv = worldScreen.viewingCiv

        val turns = gameInfo.turns
        val citiesFounded = playerCiv.citiesCreated

        val difficultyMultiplier = when (gameInfo.gameParameters.difficulty) {
            "Settler" -> 1
            "Chieftain" -> 2
            "Warlord" -> 3
            "Prince" -> 4
            "King" -> 5
            "Emperor" -> 6
            "Immortal" -> 7
            "Deity" -> 8
            else -> 4 // default to Prince-level
        }

        return turns * difficultyMultiplier + citiesFounded * 100
    }

    private fun submitCurrentGameScore() {
        val worldScreen = GUI.getWorldScreen()
        val playerCiv = worldScreen.viewingCiv
        val score = calculateScore()
        val displayName = playerCiv.civName
        // Generate a stable user ID from the civ name for consistency
        val userId = UUID.nameUUIDFromBytes(displayName.toByteArray()).toString()

        try {
            leaderboardService.submitScore(userId, displayName, score) { success ->
                Gdx.app.postRunnable {
                    try {
                        if (success) {
                            ToastPopup("Score submitted: $score", screen)
                            close()
                            LeaderboardPopup(screen, leaderboardService).open()
                        } else {
                            ToastPopup("Failed to submit score", screen)
                        }
                    } catch (_: Exception) {
                        ToastPopup("Error processing score submission", screen)
                    }
                }
            }
        } catch (_: Exception) {
            Gdx.app.postRunnable {
                ToastPopup("Network error: could not submit score", screen)
            }
        }
    }
}
