package com.alderfall.game;

import com.alderfall.game.map.WorldMap;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class GameKeyboardController extends KeyAdapter {
    private final GamePanel panel;
    private final Set<Integer> pressedKeys = new HashSet<>();

    GameKeyboardController(GamePanel panel) {
        this.panel = panel;
    }

    @Override
    public void keyTyped(KeyEvent event) {
        if (panel.state.mode == GameMode.INVENTORY) panel.inventoryRenderer.browser.typed(event.getKeyChar());
        if (panel.state.mode == GameMode.SHOP) {
            panel.shopRenderer.stockBrowser.typed(event.getKeyChar());
            panel.shopRenderer.packBrowser.typed(event.getKeyChar());
        }
        panel.repaint();
    }

        @Override
        public void keyPressed(KeyEvent event) {
            if (panel.state.mode == GameMode.INVENTORY && panel.inventoryRenderer.browser.keyPressed(event)
                    || panel.state.mode == GameMode.SHOP && (panel.shopRenderer.stockBrowser.keyPressed(event)
                    || panel.shopRenderer.packBrowser.keyPressed(event))) { panel.repaint(); return; }
            int code = event.getKeyCode();
            boolean repeatedKeyPress = !pressedKeys.add(code);
            if (code == KeyEvent.VK_F4) {
                if (!repeatedKeyPress) panel.cyclePerformanceOverlay();
                return;
            }
            if (panel.state.mode == GameMode.MAIN_MENU) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_N) {
                    panel.state.openClassSelect();
                } else if ((code == KeyEvent.VK_L || code == KeyEvent.VK_F9) && panel.saves.exists()) {
                    panel.openLoadMenu();
                } else if (code == KeyEvent.VK_I && panel.saves.exists()) {
                    panel.openImportMenu();
                } else if (code == KeyEvent.VK_S) {
                    panel.state.openSettings();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    panel.exitGame();
                }
                panel.repaint();
                return;
            }
            if (panel.state.mode == GameMode.CLASS_SELECT) {
                if (handleClassSelectTextInput(event)) {
                    panel.repaint();
                    return;
                }
                if (code == KeyEvent.VK_1) {
                    panel.state.chooseClass("Knight");
                    panel.syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_2) {
                    panel.state.chooseClass("Mage");
                    panel.syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_3) {
                    panel.state.chooseClass("Ranger");
                    panel.syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_4) {
                    panel.state.chooseClass("Cleric");
                    panel.syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_5) {
                    panel.state.chooseClass("Rogue");
                    panel.syncPlayerAnimationToState();
                } else if (code == KeyEvent.VK_F9) {
                    panel.loadGame();
                } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    if (panel.state.pendingPlayerName.length() > 0 && code == KeyEvent.VK_BACK_SPACE) {
                        panel.state.setPendingPlayerName(panel.state.pendingPlayerName.substring(0, panel.state.pendingPlayerName.length() - 1));
                    } else {
                        panel.state.openMainMenu();
                    }
                } else if (code == KeyEvent.VK_L && panel.saves.exists()) {
                    panel.openLoadMenu();
                } else {
                    char ch = event.getKeyChar();
                    if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_') && panel.state.pendingPlayerName.length() < 24) {
                        panel.state.setPendingPlayerName(panel.state.pendingPlayerName + ch);
                    }
                }
                panel.repaint();
                return;
            }
            if (panel.state.mode == GameMode.STORY_INTRO) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) {
                    panel.state.advanceStoryIntro();
                } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    panel.state.openClassSelect();
                }
                panel.repaint();
                return;
            }
            if (panel.state.mode == GameMode.PAUSE_MENU) {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER) {
                    panel.state.resumeGame();
                } else if (code == KeyEvent.VK_F5 || code == KeyEvent.VK_S) {
                    panel.openSaveMenuForSaving();
                } else if ((code == KeyEvent.VK_F9 || code == KeyEvent.VK_L) && panel.saves.exists()) {
                    panel.openSaveMenuForSaving();
                } else if (code == KeyEvent.VK_T) {
                    panel.state.openSettings();
                } else if (code == KeyEvent.VK_N) {
                    panel.state.openClassSelect();
                } else if (code == KeyEvent.VK_M) {
                    panel.state.openMainMenu();
                }
                panel.repaint();
                return;
            }
            if (panel.state.mode == GameMode.SETTINGS) {
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    panel.state.closeSettings();
                }
                panel.repaint();
                return;
            }
            if (panel.state.mode == GameMode.SAVE_MENU) {
                if (panel.hasPendingOverwrite()) {
                    if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_F5) {
                        panel.confirmOverwriteSave();
                    } else if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                        panel.cancelOverwrite();
                    }
                    panel.repaint();
                    return;
                }
                if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_BACK_SPACE) {
                    if (panel.state.saveMenuCanSave && code == KeyEvent.VK_BACK_SPACE && !panel.state.pendingSaveName.isEmpty()) {
                        panel.state.setPendingSaveName(panel.state.pendingSaveName.substring(0, panel.state.pendingSaveName.length() - 1));
                    } else {
                        panel.closeSaveMenuOrLoadFolder();
                    }
                } else if ((code == KeyEvent.VK_ENTER || code == KeyEvent.VK_F5) && panel.state.saveMenuCanSave) {
                    panel.saveGame();
                } else if (panel.state.saveMenuCanSave && code == KeyEvent.VK_DELETE) {
                    panel.state.setPendingSaveName("");
                } else if (panel.state.saveMenuCanSave && code == KeyEvent.VK_TAB) {
                    panel.saveListCurrentCharacterOnly = !panel.saveListCurrentCharacterOnly;
                    panel.saveListScroll = 0;
                } else if (!panel.state.saveMenuCanSave && code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    int index = code - KeyEvent.VK_1;
                    if (panel.selectedLoadCharacterId.isBlank()) {
                        List<GamePanel.LoadFolder> folders = panel.loadFoldersByLatestSave();
                        if (index < folders.size()) {
                            panel.selectLoadFolder(folders.get(index).characterId);
                        }
                    } else {
                        List<SaveSystem.SaveSummary> summaries = panel.selectedLoadFolderSaves();
                        if (index < summaries.size()) {
                            panel.loadOrImportGame(summaries.get(index).saveId());
                        }
                    }
                } else if (code == KeyEvent.VK_UP) {
                    panel.saveListScroll = Math.max(0, panel.saveListScroll - 1);
                } else if (code == KeyEvent.VK_DOWN) {
                    panel.saveListScroll = Math.max(0, panel.saveListScroll + 1);
                } else if (panel.state.saveMenuCanSave) {
                    char ch = event.getKeyChar();
                    if ((Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_' || ch == '\'')
                            && panel.state.pendingSaveName.length() < 32) {
                        panel.state.setPendingSaveName(panel.state.pendingSaveName + ch);
                    }
                }
                panel.repaint();
                return;
            }

            if (panel.state.dialogueVideoActive()) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ESCAPE) {
                    panel.state.dismissDialogueVideo();
                    panel.repaint();
                    return;
                }
            }

            if (code == KeyEvent.VK_F3) {
                panel.battleVfxDebug = !panel.battleVfxDebug;
            } else if (code == KeyEvent.VK_F5) {
                panel.saveGame();
            } else if (code == KeyEvent.VK_F9) {
                panel.loadGame();
            } else if (code == KeyEvent.VK_ESCAPE) {
                if(panel.dismissVillageContextMenu()){panel.repaint();return;}
                if(panel.state.mode==GameMode.VILLAGE && panel.state.villageCatalogOpen){panel.state.villageCatalogOpen=false;panel.repaint();return;}
                if(panel.state.mode==GameMode.VILLAGE && !panel.state.pendingWorkplaceAlly.isBlank()){panel.state.cancelWorkplaceSelection();panel.repaint();return;}
                if (panel.state.mode == GameMode.EXPLORE || panel.state.mode == GameMode.BATTLE || panel.state.mode == GameMode.DIALOG || panel.state.mode == GameMode.DEFENSE) {
                    panel.state.openPauseMenu();
                } else if (panel.state.mode != GameMode.EXPLORE && panel.state.mode != GameMode.BATTLE) {
                    panel.state.closeOverlay();
                }
            } else if (code == KeyEvent.VK_P) {
                panel.state.openPauseMenu();
            } else if (panel.state.mode == GameMode.BATTLE) {
                handleBattleKey(event, repeatedKeyPress);
            } else if (panel.state.mode == GameMode.DEFENSE) {
                if (panel.state.defenseRaid != null && !panel.state.defenseRaid.pendingLevelChoices().isEmpty()
                        && code >= KeyEvent.VK_1 && code <= KeyEvent.VK_3) {
                    panel.state.chooseDefenseRaidLevelAbility(code - KeyEvent.VK_1);
                } else if (panel.state.defenseRaid != null && !panel.state.defenseRaid.started() && code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    panel.state.toggleDefenseRaidAbility(code - KeyEvent.VK_1);
                } else if (panel.state.defenseRaid != null && !panel.state.defenseRaid.started()
                        && (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE)) {
                    panel.state.beginDefenseRaid();
                } else if (code == KeyEvent.VK_Q && panel.state.defenseRaid != null && panel.state.defenseRaid.finished()) {
                    panel.state.closeDefenseRaid();
                } else if (code == KeyEvent.VK_Q) {
                    panel.state.status = "Finish the raid or press Esc to pause.";
                } else {
                    panel.handleDefenseMovementKeyPressed(code);
                }
            } else if (panel.state.mode == GameMode.DIALOG) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) {
                    panel.state.advanceDialog();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    panel.runDialogOption(panel.dialogOptionScroll + code - KeyEvent.VK_1);
                } else if (code == KeyEvent.VK_H) {
                    panel.state.hireActiveRecruit();
                }
            } else if (panel.state.mode == GameMode.QUEST_LOG) {
                if (code == KeyEvent.VK_Q) {
                    panel.state.toggleQuestLog();
                } else if (code == KeyEvent.VK_TAB) {
                    panel.questLogCompletedTab = !panel.questLogCompletedTab;
                    panel.questLogScroll = 0;
                    panel.selectedQuestLogId = "";
                } else if (code == KeyEvent.VK_1) {
                    panel.questLogCompletedTab = false;
                    panel.questLogScroll = 0;
                    panel.selectedQuestLogId = "";
                } else if (code == KeyEvent.VK_2) {
                    panel.questLogCompletedTab = true;
                    panel.questLogScroll = 0;
                    panel.selectedQuestLogId = "";
                } else if (code == KeyEvent.VK_T) {
                    Quest quest = panel.selectedQuestLogQuest(panel.questLogEntries(panel.questLogCompletedTab));
                    if (quest != null && !quest.completed) {
                        panel.toggleTrackedQuest(quest);
                    }
                }
            } else if (panel.state.mode == GameMode.SKILLS) {
                if (code == KeyEvent.VK_K || code == KeyEvent.VK_O || code == KeyEvent.VK_Q) {
                    panel.state.closeOverlay();
                }
            } else if (panel.state.mode == GameMode.WORLD_MAP) {
                if (code == KeyEvent.VK_M || code == KeyEvent.VK_Q) {
                    panel.state.toggleWorldMap();
                } else if (code == KeyEvent.VK_K) {
                    panel.state.toggleWorldMapKingdoms();
                } else if (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD) {
                    panel.adjustWorldMapZoom(1);
                } else if (code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT) {
                    panel.adjustWorldMapZoom(-1);
                } else if (code == KeyEvent.VK_0) {
                    panel.resetWorldMapZoom();
                } else if (code == KeyEvent.VK_HOME || code == KeyEvent.VK_Y) {
                    panel.focusWorldMapOnPlayer();
                } else if (code == KeyEvent.VK_TAB) {
                    panel.focusNextWorldMapQuest();
                } else if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
                    panel.panWorldMap(-WorldMap.COLS / (panel.worldMapZoom * 10.0), 0.0);
                } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
                    panel.panWorldMap(WorldMap.COLS / (panel.worldMapZoom * 10.0), 0.0);
                } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                    panel.panWorldMap(0.0, -WorldMap.ROWS / (panel.worldMapZoom * 10.0));
                } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                    panel.panWorldMap(0.0, WorldMap.ROWS / (panel.worldMapZoom * 10.0));
                }
            } else if (panel.state.mode == GameMode.INVENTORY) {
                if (code == KeyEvent.VK_I) {
                    panel.state.toggleInventory();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    panel.inventoryRenderer.useVisibleItem(code - KeyEvent.VK_1);
                }
            } else if (panel.state.mode == GameMode.CRAFTING) {
                if (code == KeyEvent.VK_C || code == KeyEvent.VK_Q) {
                    panel.state.toggleCrafting();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    List<CraftingSystem.Recipe> recipes = panel.displayedCraftingRecipes();
                    int index = panel.craftingRecipeScroll + code - KeyEvent.VK_1;
                    if (index >= 0 && index < recipes.size()) {
                        panel.state.craftRecipe(recipes.get(index).key());
                    }
                }
            } else if (panel.state.mode == GameMode.PARTY) {
                if (code == KeyEvent.VK_O || code == KeyEvent.VK_Q) {
                    panel.state.toggleParty();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    panel.state.selectPartyScreenActor(code - KeyEvent.VK_1);
                }
            } else if (panel.state.mode == GameMode.VILLAGE) {
                if (panel.villageSearchFocused() && panel.handleVillageSearchKey(event)) {
                    panel.repaint();
                    return;
                }
                if (code == KeyEvent.VK_V || code == KeyEvent.VK_Q) {
                    panel.state.toggleVillage();
                } else if (code == KeyEvent.VK_1) {
                    panel.state.setVillageTab(0);
                } else if (code == KeyEvent.VK_2) {
                    panel.state.setVillageTab(1);
                } else if (code == KeyEvent.VK_3) {
                    panel.state.setVillageTab(2);
                } else if (code == KeyEvent.VK_4) {
                    panel.state.setVillageTab(3);
                } else if (code == KeyEvent.VK_5) {
                    panel.state.setVillageTab(4);
                } else if (code == KeyEvent.VK_DELETE || code == KeyEvent.VK_BACK_SPACE) {
                    panel.state.setVillageEditAction("delete");
                } else if (code == KeyEvent.VK_M) {
                    panel.state.setVillageEditAction("move");
                } else if (code == KeyEvent.VK_U) {
                    panel.state.setVillageEditAction("upgrade");
                } else if (code == KeyEvent.VK_P) {
                    panel.state.setVillageEditAction("place");
                } else if (code == KeyEvent.VK_R) {
                    panel.state.startDefenseRaid();
                }
            } else if (panel.state.mode == GameMode.BUILDING_ASSIGNMENT) {
                if (code == KeyEvent.VK_Q || code == KeyEvent.VK_ESCAPE) {
                    panel.state.closeOverlay();
                } else if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) {
                    panel.state.enterActiveVillageBuilding();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    int index = code - KeyEvent.VK_1;
                    List<Actor> workers = panel.state.stationedAllies();
                    if (index >= 0 && index < workers.size()) {
                        panel.state.assignAllyToActiveBuilding(workers.get(index).name);
                    }
                }
            } else if (panel.state.mode == GameMode.SETTLEMENT_BOARD) {
                if (code == KeyEvent.VK_Q || code == KeyEvent.VK_E) {
                    panel.state.closeOverlay();
                } else if (code == KeyEvent.VK_1) {
                    panel.state.setSettlementBoardTab(0);
                } else if (code == KeyEvent.VK_2) {
                    panel.state.setSettlementBoardTab(1);
                } else if (code == KeyEvent.VK_R) {
                    panel.state.rerollSettlementRecruits();
                }
            } else if (panel.state.mode == GameMode.FAST_TRAVEL) {
                if (code == KeyEvent.VK_Q || code == KeyEvent.VK_E) {
                    panel.state.closeOverlay();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    panel.state.fastTravelTo(code - KeyEvent.VK_1);
                }
            } else if (panel.state.mode == GameMode.SHOP) {
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    panel.shopRenderer.useVisibleItem(code - KeyEvent.VK_1);
                } else if (code == KeyEvent.VK_H) {
                    panel.state.hireActiveRecruit();
                }
            } else if (panel.handleMovementKeyPressed(code)) {
            } else if (code == KeyEvent.VK_E) {
                panel.interactWithRoamingWorldEvent();
            } else if (code == KeyEvent.VK_Q) {
                panel.state.toggleQuestLog();
            } else if (code == KeyEvent.VK_M) {
                panel.state.toggleWorldMap();
            } else if (code == KeyEvent.VK_K) {
                panel.state.toggleSkills();
            } else if (code == KeyEvent.VK_I) {
                panel.state.toggleInventory();
            } else if (code == KeyEvent.VK_G) {
                panel.state.gatherNearby(panel.playerFacingDx, panel.playerFacingDy);
            } else if (code == KeyEvent.VK_C) {
                panel.state.toggleCrafting();
            } else if (code == KeyEvent.VK_O) {
                panel.state.toggleParty();
            } else if (code == KeyEvent.VK_V) {
                panel.state.toggleVillage();
            } else if (code == KeyEvent.VK_R) {
                panel.state.startDefenseRaid();
            } else if (code == KeyEvent.VK_H) {
                panel.state.useItem("potion_small");
            } else if (code == KeyEvent.VK_J) {
                panel.state.useItem("ether");
            }
            panel.repaint();
        }

        @Override
        public void keyReleased(KeyEvent event) {
            pressedKeys.remove(event.getKeyCode());
            boolean handled = panel.state.mode == GameMode.DEFENSE
                    ? panel.handleDefenseMovementKeyReleased(event.getKeyCode())
                    : panel.handleMovementKeyReleased(event.getKeyCode());
            if (handled) {
                panel.repaint();
            }
        }

        private void handleBattleKey(KeyEvent event, boolean repeatedKeyPress) {
            int code = event.getKeyCode();
            if (repeatedKeyPress) {
                return;
            }
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_SPACE) {
                panel.state.battleAttack();
            } else if (code == KeyEvent.VK_S) {
                panel.state.battleHeavyAttack();
            } else if (code == KeyEvent.VK_F) {
                panel.state.battleCleaveAttack();
            } else if (code == KeyEvent.VK_D) {
                panel.state.battleDodge();
            } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                panel.state.battleAbility(code - KeyEvent.VK_1);
            } else if (code == KeyEvent.VK_0) {
                panel.state.battleAbility(9);
            } else if (code == KeyEvent.VK_TAB) {
                if (event.isShiftDown()) {
                    panel.state.cycleBattlePartyTarget(1);
                } else {
                    panel.state.cycleBattleEnemyTarget(1);
                }
            } else if (code == KeyEvent.VK_E) {
                panel.state.cycleBattleEnemyTarget(1);
            } else if (code == KeyEvent.VK_Q) {
                panel.state.cycleBattlePartyTarget(1);
            } else if (code == KeyEvent.VK_ENTER) {
                panel.state.leaveFinishedBattle();
                if (panel.state.mode == GameMode.EXPLORE) panel.syncPlayerAnimationToState();
            } else if (code == KeyEvent.VK_R && panel.state.battle != null && panel.state.battle.finished && !panel.state.battle.victory) {
                panel.state.revive();
                panel.syncPlayerAnimationToState();
            } else if (code == KeyEvent.VK_R) {
                panel.state.battleRun();
            } else if (code == KeyEvent.VK_H) {
                panel.toggleBattleItemPicker();
            }
        }

        private boolean handleClassSelectTextInput(KeyEvent event) {
            int code = event.getKeyCode();
            if (code == KeyEvent.VK_BACK_SPACE) {
                if (!panel.state.pendingPlayerName.isEmpty()) {
                    panel.state.setPendingPlayerName(panel.state.pendingPlayerName.substring(0, panel.state.pendingPlayerName.length() - 1));
                }
                return true;
            }
            if (event.isControlDown() || event.isAltDown() || event.isMetaDown()) {
                return false;
            }
            char ch = event.getKeyChar();
            if (isNameCharacter(ch) && panel.state.pendingPlayerName.length() < 24) {
                panel.state.setPendingPlayerName(panel.state.pendingPlayerName + ch);
                return true;
            }
            return false;
        }

        private boolean isNameCharacter(char ch) {
            return Character.isLetterOrDigit(ch) || ch == ' ' || ch == '-' || ch == '_';
        }

    }


