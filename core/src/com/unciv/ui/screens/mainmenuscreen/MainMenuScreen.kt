package com.unciv.ui.screens.mainmenuscreen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Align
import com.unciv.Constants
import com.unciv.GUI
import com.unciv.UncivGame
import com.unciv.logic.GameInfo
import com.unciv.logic.GameStarter
import com.unciv.logic.HolidayDates
import com.unciv.logic.UncivShowableException
import com.unciv.logic.map.MapParameters
import com.unciv.logic.map.MapShape
import com.unciv.logic.map.MapSize
import com.unciv.logic.map.MapType
import com.unciv.logic.map.mapgenerator.MapGenerator
import com.unciv.models.metadata.BaseRuleset
import com.unciv.models.metadata.GameSetupInfo
import com.unciv.models.ruleset.Ruleset
import com.unciv.models.ruleset.RulesetCache
import com.unciv.models.tilesets.TileSetCache
import com.unciv.ui.audio.SoundPlayer
import com.unciv.ui.components.UncivTooltip.Companion.addTooltip
import com.unciv.ui.components.extensions.addBorder
import com.unciv.ui.components.extensions.addSeparator
import com.unciv.ui.components.extensions.center
import com.unciv.ui.components.extensions.surroundWithCircle
import com.unciv.ui.components.extensions.surroundWithThinCircle
import com.unciv.ui.components.extensions.toLabel
import com.unciv.ui.components.fonts.Fonts
import com.unciv.ui.components.input.KeyShortcutDispatcherVeto
import com.unciv.ui.components.input.KeyboardBinding
import com.unciv.ui.components.input.keyShortcuts
import com.unciv.ui.components.input.onActivation
import com.unciv.ui.components.input.onClick
import com.unciv.ui.components.input.onLongPress
import com.unciv.ui.components.tilegroups.TileGroupMap
import com.unciv.ui.components.widgets.AutoScrollPane
import com.unciv.ui.images.ImageGetter
import com.unciv.ui.images.padTopDescent
import com.unciv.ui.popups.Popup
import com.unciv.ui.popups.ToastPopup
import com.unciv.ui.popups.closeAllPopups
import com.unciv.ui.popups.hasOpenPopups
import com.unciv.ui.popups.options.aboutTab
import com.unciv.ui.popups.popups
import com.unciv.ui.screens.basescreen.BaseScreen
import com.unciv.ui.screens.basescreen.RecreateOnResize
import com.unciv.ui.screens.mainmenuscreen.EasterEggRulesets.modifyForEasterEgg
import com.unciv.ui.screens.mapeditorscreen.EditorMapHolder
import com.unciv.ui.screens.mapeditorscreen.MapEditorScreen
import com.unciv.ui.screens.modmanager.ModManagementScreen
import com.unciv.ui.screens.multiplayerscreens.MultiplayerScreen
import com.unciv.ui.screens.newgamescreen.NewGameScreen
import com.unciv.ui.screens.savescreens.LoadGameScreen
import com.unciv.ui.screens.savescreens.QuickSave
import com.unciv.ui.screens.worldscreen.BackgroundActor
import com.unciv.ui.screens.worldscreen.LeaderboardPopup
import com.unciv.ui.screens.worldscreen.LeaderboardService
import com.unciv.ui.screens.worldscreen.WorldScreen
import com.unciv.ui.screens.worldscreen.mainmenu.WorldScreenMenuPopup
import com.unciv.utils.Concurrency
import com.unciv.utils.launchOnGLThread
import kotlinx.coroutines.Job
import yairm210.purity.annotations.Pure
import kotlin.math.ceil


class MainMenuScreen: BaseScreen(), RecreateOnResize {
    private val backgroundStack = Stack()
    private val singleColumn = isCrampedPortrait()

    private val backgroundMapRuleset: Ruleset
    private var easterEggRuleset: Ruleset? = null  // Cache it so the next 'egg' can be found in Civilopedia

    private var backgroundMapGenerationJob: Job? = null
    private var backgroundMapExists = false

    companion object {
        const val mapFadeTime = 1.3f
        const val mapFirstFadeTime = 0.3f
        const val mapReplaceDelay = 20f
        /** Inner size of the Civilopedia+Discord+Github buttons (effective size adds 2f for the thin circle) */
        const val buttonsSize = 70f
        /** Distance of the Civilopedia and Discord+Github buttons from the stage edges */
        const val buttonsPosFromEdge = 30f
        /** Gold accent color used throughout the EmpireForge menu */
        val goldColor = Color(0.85f, 0.65f, 0.13f, 1f)
    }

    /** Create one **Main Menu Button** including onClick/key binding
     *  @param text      The text to display on the button
     *  @param icon      The path of the icon to display on the button
     *  @param binding   keyboard binding
     *  @param isLeaderboard Whether this is the leaderboard button (gets special gold styling)
     *  @param function  Action to invoke when the button is activated
     */
    private fun getMenuButton(
        text: String,
        icon: String,
        binding: KeyboardBinding,
        isLeaderboard: Boolean = false,
        function: () -> Unit
    ): Table {
        val tintedBaseColor = if (isLeaderboard)
            goldColor.cpy().lerp(skinStrings.skinConfig.baseColor, 0.3f)
        else
            skinStrings.skinConfig.baseColor.cpy().lerp(goldColor, 0.15f)

        val innerTable = Table().pad(20f, 40f, 20f, 40f)
        innerTable.background = skinStrings.getUiBackground(
            "MainMenuScreen/MenuButton",
            skinStrings.roundedEdgeRectangleShape,
            tintedBaseColor
        )
        innerTable.add(ImageGetter.getImage(icon)).size(50f).padRight(20f)
        innerTable.add(text.toLabel(fontSize = 30, alignment = Align.left)).expand().left().minWidth(200f)
            .padTopDescent()

        // Wrap with a gold border
        val borderColor = if (isLeaderboard) goldColor else goldColor.cpy().apply { a = 0.6f }
        val bordered = innerTable.addBorder(2f, borderColor, expandCell = true)

        bordered.touchable = Touchable.enabled
        bordered.onActivation(binding = binding) {
            stopBackgroundMapGeneration()
            function()
        }

        bordered.pack()
        return bordered
    }

    init {
        SoundPlayer.initializeForMainMenu()

        val background = skinStrings.getUiBackground("MainMenuScreen/Background", tintColor = clearColor)
        backgroundStack.add(BackgroundActor(background, Align.center))
        stage.addActor(backgroundStack)
        backgroundStack.setFillParent(true)

        // If we were in a mod, some of the resource images for the background map we're creating
        // will not exist unless we reset the ruleset and images
        val baseRuleset = RulesetCache.getVanillaRuleset()
        ImageGetter.setNewRuleset(baseRuleset)

        if (game.settings.enableEasterEggs) {
            val holiday = HolidayDates.getHolidayByDate()
            if (holiday != null)
                EasterEggFloatingArt(stage, holiday.name)
            val easterEggMod = EasterEggRulesets.getTodayEasterEggRuleset()
            if (easterEggMod != null)
                easterEggRuleset = RulesetCache.getComplexRuleset(baseRuleset, listOf(easterEggMod))
        }
        backgroundMapRuleset = easterEggRuleset ?: baseRuleset

        // This is an extreme safeguard - should an invalid settings.tileSet ever make it past the
        // guard in UncivGame.create, simply omit the background so the user can at least get to options
        // (let him crash when loading a game but avoid locking him out entirely)
        if (game.settings.tileSet in TileSetCache)
            startBackgroundMapGeneration()

        // --- 2-column grid of menu buttons ---
        val allButtons = mutableListOf<Table>()

        if (game.files.autosaves.autosaveExists()) {
            allButtons += getMenuButton("Resume","OtherIcons/Resume", KeyboardBinding.Resume)
                { resumeGame() }
        }

        allButtons += getMenuButton("Quickstart", "OtherIcons/Quickstart", KeyboardBinding.Quickstart)
            { quickstartNewGame() }

        allButtons += getMenuButton("Start new game", "OtherIcons/New", KeyboardBinding.StartNewGame)
            { game.pushScreen(NewGameScreen()) }

        allButtons += getMenuButton("Load game", "OtherIcons/Load", KeyboardBinding.MainMenuLoad)
            { game.pushScreen(LoadGameScreen()) }

        allButtons += getMenuButton("Multiplayer", "OtherIcons/Multiplayer", KeyboardBinding.Multiplayer)
            { game.pushScreen(MultiplayerScreen()) }

        allButtons += getMenuButton("Map editor", "OtherIcons/MapEditor", KeyboardBinding.MapEditor)
            { game.pushScreen(MapEditorScreen()) }

        allButtons += getMenuButton("Mods", "OtherIcons/Mods", KeyboardBinding.ModManager)
            { game.pushScreen(ModManagementScreen()) }

        val leaderboardButton = getMenuButton("Leaderboard", "OtherIcons/Multiplayer", KeyboardBinding.None, isLeaderboard = true)
            { LeaderboardPopup(this, LeaderboardService()).open() }
        allButtons += leaderboardButton

        val optionsButton = getMenuButton("Options", "OtherIcons/Options", KeyboardBinding.MainMenuOptions)
            { openOptionsPopup() }
        optionsButton.onLongPress { openOptionsPopup(withDebug = true) }
        allButtons += optionsButton

        // Lay out in a 2-column grid (single column on cramped portrait)
        val gridTable = Table().apply { defaults().pad(10f).fillX(); padTop(140f) }
        for ((index, btn) in allButtons.withIndex()) {
            // Staggered fade-in animation for each button
            btn.addAction(Actions.sequence(
                Actions.alpha(0f),
                Actions.delay(index * 0.1f),
                Actions.fadeIn(0.4f)
            ))
            gridTable.add(btn).uniform()
            if (singleColumn || (index % 2 == 1)) gridTable.row()
        }
        gridTable.pack()

        val scrollPane = AutoScrollPane(gridTable)
        scrollPane.setFillParent(true)
        stage.addActor(scrollPane)
        gridTable.center(scrollPane)

        // --- EmpireForge branding - title at top with shadow effect ---
        val titleTable = Table()

        // Shadow label (drawn first, offset by 2px)
        val titleShadow = "EmpireForge".toLabel(Color(0.1f, 0.08f, 0.03f, 0.7f), 52)
        titleShadow.setAlignment(Align.center)
        // Foreground gold label
        val titleLabel = "EmpireForge".toLabel(goldColor, 52)
        titleLabel.setAlignment(Align.center)

        // Stack the shadow and foreground title
        val titleStack = Stack()
        val shadowContainer = Table()
        shadowContainer.add(titleShadow).padLeft(2f).padTop(2f)
        titleStack.add(shadowContainer)
        val foregroundContainer = Table()
        foregroundContainer.add(titleLabel)
        titleStack.add(foregroundContainer)
        titleTable.add(titleStack).row()

        val subtitleLabel = "Rebuild. Survive. Dominate.".toLabel(Color(0.8f, 0.8f, 0.85f, 0.9f), 18)
        subtitleLabel.setAlignment(Align.center)
        titleTable.add(subtitleLabel).padTop(5f).row()

        // Decorative gold separator line under subtitle
        titleTable.addSeparator(goldColor.cpy().apply { a = 0.6f }, height = 2f).width(300f).padTop(8f)

        titleTable.touchable = Touchable.disabled
        titleTable.pack()
        titleTable.setPosition(stage.width / 2, stage.height - titleTable.height - 15f, Align.center)
        stage.addActor(titleTable)

        globalShortcuts.add(KeyboardBinding.QuitMainMenu) {
            if (hasOpenPopups()) {
                closeAllPopups()
                return@add
            }
            game.popScreen()
        }

        // --- Corner icon buttons (larger, 70f) ---
        val civilopediaButton = "?".toLabel(fontSize = 48)
            .apply { setAlignment(Align.center) }
            .surroundWithCircle(buttonsSize, color = skinStrings.skinConfig.baseColor)
            .apply { actor.y -= Fonts.getDescenderHeight(48) / 2 } // compensate font baseline
            .surroundWithThinCircle(Color.WHITE)
        civilopediaButton.touchable = Touchable.enabled
        civilopediaButton.onActivation { openCivilopedia() }
        civilopediaButton.keyShortcuts.add(KeyboardBinding.Civilopedia)
        civilopediaButton.addTooltip(KeyboardBinding.Civilopedia, 30f)
        civilopediaButton.setPosition(buttonsPosFromEdge, buttonsPosFromEdge)
        // Subtle pulsing animation on the Civilopedia button
        civilopediaButton.addAction(Actions.forever(Actions.sequence(
            Actions.scaleTo(1.05f, 1.05f, 1f),
            Actions.scaleTo(1f, 1f, 1f)
        )))
        stage.addActor(civilopediaButton)

        val rightSideButtons = Table().apply { defaults().space(10f) }

        rightSideButtons.pack()
        rightSideButtons.setPosition(stage.width - buttonsPosFromEdge, buttonsPosFromEdge, Align.bottomRight)
        stage.addActor(rightSideButtons)

        // --- Footer: version label centered at bottom with "Powered by Open Source" ---
        val versionLabel = "{Version} ${UncivGame.VERSION.text}".toLabel()
        versionLabel.setAlignment(Align.center)
        val poweredByLabel = "Powered by Open Source".toLabel(Color.GRAY, 12)
        poweredByLabel.setAlignment(Align.center)

        val versionTable = Table()
        versionTable.background = skinStrings.getUiBackground("MainMenuScreen/Version",
            skinStrings.roundedEdgeRectangleShape, Color.DARK_GRAY.cpy().apply { a = 0.7f })
        versionTable.add(versionLabel).row()
        versionTable.add(poweredByLabel).padTop(2f)
        versionTable.pack()
        versionTable.setPosition(stage.width / 2, 10f, Align.bottom)
        versionTable.touchable = Touchable.enabled
        versionTable.onClick {
            val popup = Popup(stage)
            popup.add(aboutTab()).row()
            popup.addCloseButton()
            popup.open()
        }
        stage.addActor(versionTable)
    }

    private fun startBackgroundMapGeneration() {
        stopBackgroundMapGeneration()  // shouldn't be necessary as resize re-instantiates this class
        backgroundMapGenerationJob = Concurrency.run("ShowMapBackground") {
            // MapSize.Small has easily enough tiles to fill the entire background - unless the user sized their window to some extreme aspect ratio
            val mapWidth = stage.width / TileGroupMap.groupHorizontalAdvance
            val mapHeight = stage.height / TileGroupMap.groupSize
            @Pure fun Float.scaleCoord(scale: Float) = ceil(this * scale).toInt().coerceAtLeast(6)
            // These scale values are chosen so that a common 4:3 screen minus taskbar gives the same as MapSize.Small
            val backgroundMapSize = MapSize(mapWidth.scaleCoord(.77f), mapHeight.scaleCoord(1f))

            val newMap = MapGenerator(backgroundMapRuleset, this)
                .generateMap(MapParameters().apply {
                    shape = MapShape.rectangular
                    mapSize = backgroundMapSize
                    type = MapType.pangaea
                    temperatureintensity = .7f
                    waterThreshold = -0.1f // mainly land, gets about 30% water
                    modifyForEasterEgg()
                })

            launchOnGLThread { // for GL context
                ImageGetter.setNewRuleset(backgroundMapRuleset, ignoreIfModsAreEqual = true)
                val mapHolder = EditorMapHolder(
                    this@MainMenuScreen,
                    newMap
                ) {}
                mapHolder.color = mapHolder.color.cpy()
                mapHolder.color.a = 0f
                backgroundStack.add(mapHolder)

                if (backgroundMapExists) {
                    mapHolder.addAction(Actions.sequence(
                        Actions.fadeIn(mapFadeTime),
                        Actions.run { backgroundStack.removeActorAt(1, false) }
                    ))
                } else {
                    backgroundMapExists = true
                    mapHolder.addAction(Actions.fadeIn(mapFirstFadeTime))
                }
            }
        }.apply {
            invokeOnCompletion {
                backgroundMapGenerationJob = null
                backgroundStack.addAction(Actions.sequence(
                    Actions.delay(mapReplaceDelay),
                    Actions.run { startBackgroundMapGeneration() }
                ))
            }
        }
    }

    private fun stopBackgroundMapGeneration() {
        backgroundStack.clearActions()
        val currentJob = backgroundMapGenerationJob
            ?: return
        backgroundMapGenerationJob = null
        if (currentJob.isCancelled) return
        currentJob.cancel()
    }

    private fun resumeGame() {
        if (GUI.isWorldLoaded()) {
            val currentTileSet = GUI.getMap().currentTileSetStrings
            val currentGameSetting = GUI.getSettings()
            if (currentTileSet.tileSetName != currentGameSetting.tileSet ||
                    currentTileSet.unitSetName != currentGameSetting.unitSet) {
                game.removeScreensOfType(WorldScreen::class)
                QuickSave.autoLoadGame(this)
            } else {
                GUI.resetToWorldScreen()
                GUI.getWorldScreen().popups.filterIsInstance<WorldScreenMenuPopup>().forEach(Popup::close)
            }
        } else {
            QuickSave.autoLoadGame(this)
        }
    }

    private fun quickstartNewGame() {
        ToastPopup(Constants.working, this)
        val errorText = "Cannot start game with the default new game parameters!"
        Concurrency.run("QuickStart") {
            val newGame: GameInfo
            // Can fail when starting the game...
            try {
                val gameInfo = GameSetupInfo.fromSettings("Chieftain")
                if (gameInfo.gameParameters.victoryTypes.isEmpty()) {
                    val ruleSet = RulesetCache.getComplexRuleset(gameInfo.gameParameters)
                    gameInfo.gameParameters.victoryTypes.addAll(ruleSet.victories.keys)
                }
                newGame = GameStarter.startNewGame(gameInfo)

            } catch (notAPlayer: UncivShowableException) {
                val (message) = LoadGameScreen.getLoadExceptionMessage(notAPlayer)
                launchOnGLThread { ToastPopup(message, this@MainMenuScreen) }
                return@run
            } catch (_: Exception) {
                launchOnGLThread { ToastPopup(errorText, this@MainMenuScreen) }
                return@run
            }

            // ...or when loading the game
            try {
                game.loadGame(newGame)
            } catch (_: OutOfMemoryError) {
                launchOnGLThread {
                    ToastPopup("Not enough memory on phone to load game!", this@MainMenuScreen)
                }
            } catch (notAPlayer: UncivShowableException) {
                val (message) = LoadGameScreen.getLoadExceptionMessage(notAPlayer)
                launchOnGLThread {
                    ToastPopup(message, this@MainMenuScreen)
                }
            } catch (_: Exception) {
                launchOnGLThread {
                    ToastPopup(errorText, this@MainMenuScreen)
                }
            }
        }
    }

    override fun getCivilopediaRuleset(): Ruleset {
        if (easterEggRuleset != null) return easterEggRuleset!!
        val rulesetParameters = game.settings.lastGameSetup?.gameParameters
        if (rulesetParameters != null) return RulesetCache.getComplexRuleset(rulesetParameters)
        return RulesetCache[BaseRuleset.Civ_V_GnK.fullName]
            ?: throw IllegalStateException("No ruleset found")
    }

    override fun openCivilopedia(link: String) {
        stopBackgroundMapGeneration()
        val ruleset = getCivilopediaRuleset()
        UncivGame.Current.translations.translationActiveMods = ruleset.mods
        ImageGetter.setNewRuleset(ruleset)
        setSkin()
        openCivilopedia(ruleset, link = link)
    }

    override fun recreate(): BaseScreen {
        stopBackgroundMapGeneration()
        return MainMenuScreen()
    }

    override fun resume() {
        startBackgroundMapGeneration()
    }

    // We contain a map...
    override fun getShortcutDispatcherVetoer() = KeyShortcutDispatcherVeto.createTileGroupMapDispatcherVetoer()
}
