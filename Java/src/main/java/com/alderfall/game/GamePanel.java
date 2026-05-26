package com.alderfall.game;

import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import javax.swing.JPanel;
import javax.swing.Timer;

public final class GamePanel extends JPanel {
    private final GameState state;
    private final AssetStore assets;
    private final SaveSystem saves;
    private final Timer timer;
    private final List<UiButton> buttons = new ArrayList<>();
    private int frame;
    private int lastCamX;
    private int lastCamY;
    private int lastVisibleCols;
    private int lastVisibleRows;

    public GamePanel(Path javaRoot, Path pythonRoot) {
        this.state = new GameState(GameConfig.load(pythonRoot));
        this.assets = new AssetStore(javaRoot.resolve("assets"));
        this.saves = new SaveSystem(javaRoot);
        setPreferredSize(new Dimension(GameConfig.WIDTH, GameConfig.HEIGHT));
        setBackground(new Color(15, 17, 24));
        setFocusable(true);
        addKeyListener(new Keys());
        Mouse mouse = new Mouse();
        addMouseListener(mouse);
        addMouseWheelListener(mouse);
        timer = new Timer(GameConfig.FPS_MS, event -> {
            frame++;
            if (state.mode == GameMode.BATTLE && state.battle != null) {
                state.battle.tick();
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        buttons.clear();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (state.mode == GameMode.CLASS_SELECT) {
            drawClassSelect(g);
        } else {
            drawWorld(g);
            drawSidebar(g);
            if (state.mode == GameMode.BATTLE) {
                drawBattle(g);
            } else if (state.mode == GameMode.DIALOG) {
                drawDialog(g);
            } else if (state.mode == GameMode.QUEST_LOG) {
                drawQuestLog(g);
            } else if (state.mode == GameMode.SKILLS) {
                drawSkillTree(g);
            } else if (state.mode == GameMode.INVENTORY) {
                drawInventory(g);
            } else if (state.mode == GameMode.SHOP) {
                drawShop(g);
            } else if (state.mode == GameMode.WORLD_MAP) {
                drawWorldMap(g);
            }
        }
        g.dispose();
    }

    private void drawClassSelect(Graphics2D g) {
        g.setColor(new Color(15, 17, 24));
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setFont(new Font("SansSerif", Font.BOLD, 44));
        g.setColor(new Color(238, 232, 210));
        drawCentered(g, "Echoes of Alderfall", 130);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        drawCentered(g, "Java migration build", 168);

        drawClassCard(g, 230, 260, "1", "Knight", "Durable front-line fighter", new Color(115, 138, 184));
        drawClassCard(g, 560, 260, "2", "Mage", "High MP and ranged spells", new Color(154, 117, 190));
        drawClassCard(g, 890, 260, "3", "Ranger", "Balanced damage and field healing", new Color(111, 165, 119));
        buttons.add(new UiButton(new java.awt.Rectangle(230, 260, 250, 230), "class:Knight", () -> {
            state.chooseClass("Knight");
            repaint();
        }));
        buttons.add(new UiButton(new java.awt.Rectangle(560, 260, 250, 230), "class:Mage", () -> {
            state.chooseClass("Mage");
            repaint();
        }));
        buttons.add(new UiButton(new java.awt.Rectangle(890, 260, 250, 230), "class:Ranger", () -> {
            state.chooseClass("Ranger");
            repaint();
        }));

        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g.setColor(new Color(196, 198, 205));
        drawCentered(g, "Press 1, 2, or 3 to start. F9 loads a Java save.", 610);
    }

    private void drawClassCard(Graphics2D g, int x, int y, String key, String title, String subtitle, Color color) {
        g.setColor(new Color(28, 31, 43));
        g.fillRoundRect(x, y, 250, 230, 8, 8);
        g.setColor(color);
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, 250, 230, 8, 8);
        g.drawImage(assets.sprite("class_" + title.toLowerCase(), 96), x + 77, y + 28, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        drawCenteredIn(g, key + " - " + title, x, y + 145, 250);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(205, 208, 216));
        drawCenteredIn(g, subtitle, x, y + 178, 250);
    }

    private void drawWorld(Graphics2D g) {
        int mapWidth = state.world.width(state.currentMapId);
        int mapHeight = state.world.height(state.currentMapId);
        int tileSize = tileSize();
        int visibleCols = Math.max(1, (GameConfig.MAP_COLS * GameConfig.TILE + tileSize - 1) / tileSize);
        int visibleRows = Math.max(1, (GameConfig.MAP_ROWS * GameConfig.TILE + tileSize - 1) / tileSize);
        int camX = clamp(state.playerX - visibleCols / 2, 0, Math.max(0, mapWidth - visibleCols));
        int camY = clamp(state.playerY - visibleRows / 2, 0, Math.max(0, mapHeight - visibleRows));
        lastCamX = camX;
        lastCamY = camY;
        lastVisibleCols = visibleCols;
        lastVisibleRows = visibleRows;
        for (int sy = 0; sy < visibleRows; sy++) {
            for (int sx = 0; sx < visibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                int px = sx * tileSize;
                int py = sy * tileSize;
                g.drawImage(assets.image(terrainImageName(tile, wx, wy), tileSize, tileSize), px, py, null);
                drawTerrainEdges(g, tile, wx, wy, px, py);
                if ("interior".equals(state.world.kind(state.currentMapId))) {
                    drawHouseTile(g, tile, wx, wy, px, py);
                } else if (tile == 'h') {
                    g.setColor(new Color(72, 47, 36, 96));
                    g.fillRect(px + scaled(6), py + scaled(8), tileSize - scaled(12), tileSize - scaled(10));
                } else if (tile == 'x' || tile == 'o') {
                    g.setColor(new Color(40, 38, 45, 120));
                    g.fillRect(px, py, tileSize, tileSize);
                }
                String landmark = state.world.landmarkAt(state.currentMapId, wx, wy);
                if (landmark != null) {
                    g.setColor(new Color(255, 245, 174));
                    g.fillOval(px + scaled(19), py + scaled(4), scaled(10), scaled(10));
                }
            }
        }

        drawRoadConnectors(g, camX, camY);
        drawCityBuildingEntities(g, camX, camY);
        drawSettlementOverlays(g, camX, camY);
        drawCloudLayer(g, camX, camY);

        for (WorldProp prop : state.world.props(state.currentMapId)) {
            if (prop.x() < camX || prop.y() < camY || prop.x() >= camX + visibleCols || prop.y() >= camY + visibleRows) {
                continue;
            }
            int size = scaled(prop.size());
            int px = (prop.x() - camX) * tileSize + (tileSize - size) / 2;
            int py = (prop.y() - camY) * tileSize + tileSize - size;
            g.drawImage(assets.sprite(prop.asset(), size), px, py, null);
        }

        for (Npc npc : GameData.NPCS) {
            if (!npc.mapId().equals(state.currentMapId)) {
                continue;
            }
            if (npc.x() < camX || npc.y() < camY || npc.x() >= camX + visibleCols || npc.y() >= camY + visibleRows) {
                continue;
            }
            int px = (npc.x() - camX) * tileSize;
            int py = (npc.y() - camY) * tileSize;
            drawShadow(g, px + scaled(8), py + scaled(36), scaled(32), scaled(8));
            g.drawImage(assets.spriteFit(npc.sprite() + "_model", scaled(34), scaled(46)), px + scaled(7), py - scaled(2), null);
            g.setColor(new Color(255, 245, 174));
            g.fillOval(px + scaled(31), py + scaled(2), scaled(7), scaled(7));
        }

        int playerPx = (state.playerX - camX) * tileSize;
        int playerPy = (state.playerY - camY) * tileSize;
        int bob = (int) Math.round(Math.sin(frame * 0.25) * 2.0);
        drawShadow(g, playerPx + scaled(8), playerPy + scaled(39), scaled(34), scaled(9));
        g.drawImage(assets.spriteFit(state.player.worldSprite, scaled(36), scaled(50)), playerPx + scaled(6), playerPy - scaled(5) + bob, null);
    }

    private void drawCityBuildingEntities(Graphics2D g, int camX, int camY) {
        if (!"city".equals(state.world.kind(state.currentMapId))) {
            return;
        }
        int ts = tileSize();
        for (CityBuilding building : state.world.cityBuildings(state.currentMapId)) {
            if (building.x2() < camX - 3 || building.y2() < camY - 3 || building.x1() >= camX + lastVisibleCols + 3 || building.y1() >= camY + lastVisibleRows + 3) {
                continue;
            }
            int lotX = (building.x1() - camX) * ts;
            int lotY = (building.y1() - camY) * ts;
            int lotW = building.width() * ts;
            int frontY = (building.y2() - camY + 1) * ts;
            int seed = Math.abs((building.x1() + building.x2()) * 928371 + (building.y1() + building.y2()) * 364479 + building.palette() * 811);

            g.setColor(new Color(17, 20, 22, 140));
            g.fillOval(lotX + scaled(4), frontY - scaled(14), Math.max(scaled(18), lotW - scaled(8)), scaled(14));
            g.setColor(new Color(45, 54, 55));
            g.fillRect(lotX + scaled(4), frontY - scaled(8), Math.max(scaled(10), lotW - scaled(8)), scaled(6));
            g.setColor(new Color(115, 122, 116));
            g.drawLine(lotX + scaled(4), frontY - scaled(9), lotX + Math.max(scaled(4), lotW - scaled(4)), frontY - scaled(9));

            int modules = buildingModuleCount(building);
            for (int index = 0; index < modules; index++) {
                String asset = buildingSprite(building, seed, index);
                int targetH = Math.max(scaled(78), Math.min(scaled(112), building.depth() * ts + scaled(24)));
                int targetW = Math.max(scaled(46), Math.min(lotW / Math.max(1, modules) + scaled(22), scaled(84)));
                int centerX = lotX + (int) Math.round((index + 0.5) * lotW / modules);
                int jitter = ((seed >> (index * 4)) & 7) - 3;
                int drawX = centerX - targetW / 2 + jitter * ts / GameConfig.TILE;
                int drawY = frontY - targetH - scaled(3);
                g.drawImage(assets.spriteFit(asset, targetW, targetH), drawX, drawY, null);
            }

            for (TilePoint door : state.world.cityBuildingDoorTiles(building)) {
                if (door.x() < camX || door.x() >= camX + lastVisibleCols || door.y() < camY || door.y() >= camY + lastVisibleRows) {
                    continue;
                }
                int markerX = (door.x() - camX) * ts;
                int markerY = (door.y() - camY + 1) * ts;
                g.setColor(new Color(45, 36, 29));
                g.fillRect(markerX + scaled(11), markerY - scaled(9), ts - scaled(22), scaled(7));
                g.setColor(new Color(122, 96, 65));
                g.drawRect(markerX + scaled(11), markerY - scaled(9), ts - scaled(22), scaled(7));
                g.setColor(new Color(202, 162, 98));
                g.fillRect(markerX + scaled(14), markerY - scaled(3), ts - scaled(28), scaled(2));
            }

            if (building.width() >= 4) {
                int lanternH = scaled(18);
                int lanternW = scaled(10);
                g.drawImage(assets.spriteFit("city_lantern", lanternW, lanternH), lotX + scaled(4), frontY - lanternH - scaled(20), null);
                g.drawImage(assets.spriteFit("city_lantern", lanternW, lanternH), lotX + lotW - lanternW - scaled(4), frontY - lanternH - scaled(20), null);
            }
        }
    }

    private int buildingModuleCount(CityBuilding building) {
        boolean civic = List.of("hall", "guild", "barracks", "warehouse").contains(building.style());
        if (civic) {
            return Math.max(1, Math.min(4, (building.width() + 2) / 3));
        }
        int modules = Math.max(1, Math.min(building.width(), (building.width() + 1) / 2));
        if ("row".equals(building.style()) && building.width() >= 6) {
            modules = Math.min(building.width(), modules + 1);
        }
        return modules;
    }

    private String buildingSprite(CityBuilding building, int seed, int index) {
        String[] sprites = switch (building.style()) {
            case "hall" -> new String[]{"city_building_stone_hall", "city_building_stone_shop"};
            case "guild" -> new String[]{"city_building_stone_shop", "city_building_stone_hall"};
            case "barracks" -> new String[]{"city_building_stone_tower", "city_building_stone_shop"};
            case "warehouse" -> new String[]{"city_building_stone_shop", "city_building_town_shop"};
            case "shop" -> new String[]{"city_building_town_shop", "city_building_town_gabled", "city_building_town_small"};
            case "inn" -> new String[]{"city_building_town_gabled", "city_building_town_shop", "city_building_town_tall"};
            case "row" -> new String[]{"city_building_town_narrow", "city_building_town_gabled", "city_building_town_tall", "city_building_town_small", "city_building_town_shop"};
            default -> new String[]{"city_building_town_gabled", "city_building_town_small", "city_building_town_tall"};
        };
        return sprites[Math.floorMod(seed + building.palette() + index * 3, sprites.length)];
    }

    private void drawHouseTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (tile == 'o') {
            drawHouseWallTile(g, wx, wy, px, py);
        } else if (tile == 'k') {
            drawHouseFurnitureTile(g, wx, wy, px, py);
        } else if (tile == 'i' || tile == 'e' || tile == 'z') {
            drawHouseFloorTile(g, tile, wx, wy, px, py);
        }
    }

    private void drawHouseFloorTile(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + tile * 71);
        g.setColor(((wx + wy) & 1) == 0 ? new Color(139, 101, 66) : new Color(124, 89, 58));
        g.fillRect(px, py, ts, ts);
        g.setColor(new Color(95, 63, 42));
        for (int y = py + scaled(7); y < py + ts; y += scaled(10)) {
            g.drawLine(px, y, px + ts, y + ((seed >> (y & 7)) & 1));
        }
        g.setColor(new Color(164, 122, 80, 120));
        for (int x = px + scaled(8); x < px + ts; x += scaled(16)) {
            g.drawLine(x, py + scaled(2), x, py + ts - scaled(2));
        }
        if (tile == 'z') {
            g.setColor((seed & 1) == 0 ? new Color(63, 107, 118) : new Color(155, 77, 68));
            g.fillRect(px + scaled(4), py + scaled(7), ts - scaled(8), ts - scaled(14));
            g.setColor(new Color(208, 180, 110));
            g.drawRect(px + scaled(9), py + scaled(13), ts - scaled(18), ts - scaled(26));
        } else if (tile == 'e') {
            g.setColor(new Color(81, 51, 31));
            g.fillRect(px + scaled(8), py + scaled(10), ts - scaled(16), ts - scaled(14));
            g.setColor(new Color(214, 189, 114));
            g.drawLine(px + scaled(12), py + scaled(15), px + ts - scaled(12), py + scaled(15));
            g.fillOval(px + ts - scaled(17), py + scaled(29), scaled(4), scaled(4));
        }
    }

    private void drawHouseWallTile(Graphics2D g, int wx, int wy, int px, int py) {
        int ts = tileSize();
        int seed = Math.abs(wx * 928371 + wy * 364479 + 79);
        g.setColor(new Color(111, 87, 64));
        g.fillRect(px, py, ts, ts);
        g.setColor(new Color(141, 116, 80));
        g.fillRect(px + scaled(2), py + scaled(2), ts - scaled(4), ts - scaled(4));
        g.setColor(new Color(79, 56, 40));
        g.drawRect(px + scaled(2), py + scaled(2), ts - scaled(4), ts - scaled(4));
        g.setColor(new Color(178, 154, 109));
        for (int y = py + scaled(9); y < py + ts; y += scaled(13)) {
            g.drawLine(px + scaled(4), y, px + ts - scaled(4), y);
        }
        if (seed % 5 == 0) {
            g.setColor(new Color(78, 52, 36));
            g.fillRect(px + scaled(17), py + scaled(15), scaled(14), scaled(13));
            g.setColor(new Color(209, 154, 67));
            g.fillRect(px + scaled(19), py + scaled(17), scaled(10), scaled(9));
        } else if (seed % 5 == 1) {
            g.setColor(new Color(90, 56, 34));
            g.fillRect(px + scaled(10), py + scaled(11), ts - scaled(20), scaled(5));
            g.setColor(new Color(184, 138, 79));
            for (int x = px + scaled(13); x < px + ts - scaled(12); x += scaled(7)) {
                g.fillRect(x, py + scaled(17), scaled(3), scaled(11));
            }
        }
    }

    private void drawHouseFurnitureTile(Graphics2D g, int wx, int wy, int px, int py) {
        drawHouseFloorTile(g, 'i', wx, wy, px, py);
        int kind = Math.abs(wx * 928371 + wy * 364479 + 83) % 4;
        if (kind == 0) {
            g.setColor(new Color(109, 67, 41));
            g.fillRect(px + scaled(7), py + scaled(12), scaled(32), scaled(21));
            g.setColor(new Color(217, 193, 133));
            g.fillRect(px + scaled(11), py + scaled(15), scaled(24), scaled(6));
            g.setColor(new Color(143, 77, 63));
            g.fillRect(px + scaled(11), py + scaled(22), scaled(24), scaled(8));
        } else if (kind == 1) {
            g.setColor(new Color(123, 77, 45));
            g.fillOval(px + scaled(12), py + scaled(13), scaled(24), scaled(24));
            g.setColor(new Color(183, 139, 81));
            g.fillOval(px + scaled(17), py + scaled(18), scaled(14), scaled(14));
        } else if (kind == 2) {
            g.setColor(new Color(91, 58, 39));
            g.fillRect(px + scaled(9), py + scaled(8), scaled(29), scaled(31));
            g.setColor(new Color(162, 120, 72));
            for (int y = py + scaled(13); y < py + scaled(36); y += scaled(6)) {
                g.drawLine(px + scaled(11), y, px + scaled(36), y);
            }
            g.setColor(new Color(201, 157, 94));
            for (int x = px + scaled(13); x < px + scaled(35); x += scaled(7)) {
                g.fillRect(x, py + scaled(15), scaled(3), scaled(19));
            }
        } else {
            g.setColor(new Color(116, 69, 43));
            g.fillRect(px + scaled(8), py + scaled(18), scaled(32), scaled(16));
            g.setColor(new Color(91, 58, 39));
            g.fillRect(px + scaled(13), py + scaled(11), scaled(22), scaled(10));
            g.setColor(new Color(106, 141, 85));
            g.fillOval(px + scaled(18), py + scaled(20), scaled(6), scaled(6));
        }
    }

    private void drawTerrainEdges(Graphics2D g, char tile, int wx, int wy, int px, int py) {
        if (!"overworld".equals(state.world.kind(state.currentMapId))) {
            return;
        }
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        for (int i = 0; i < dirs.length; i++) {
            int nx = wx + dirs[i][0];
            int ny = wy + dirs[i][1];
            char other = state.world.tileAt(state.currentMapId, nx, ny);
            if (other == tile || other == 'r' || tile == 'r' || other == 'c' || other == 'u' || tile == 'c' || tile == 'u') {
                continue;
            }
            if (!shouldBlend(tile, other)) {
                continue;
            }
            Color blend = edgeColor(tile, other);
            g.setColor(blend);
            int wobble = Math.abs((wx * 31 + wy * 17 + i * 11) % 7) - 3;
            int ts = tileSize();
            switch (i) {
                case 0 -> g.fillPolygon(new int[]{px, px + ts, px + ts, px},
                        new int[]{py, py, py + scaled(5) + wobble, py + scaled(9) - wobble}, 4);
                case 1 -> g.fillPolygon(new int[]{px + ts, px + ts, px + ts - scaled(8) + wobble, px + ts - scaled(5) - wobble},
                        new int[]{py, py + ts, py + ts, py}, 4);
                case 2 -> g.fillPolygon(new int[]{px, px + ts, px + ts, px},
                        new int[]{py + ts - scaled(7) + wobble, py + ts - scaled(10) - wobble, py + ts, py + ts}, 4);
                case 3 -> g.fillPolygon(new int[]{px, px + scaled(8) - wobble, px + scaled(5) + wobble, px},
                        new int[]{py, py, py + ts, py + ts}, 4);
                default -> {
                }
            }
        }
    }

    private boolean shouldBlend(char tile, char other) {
        return isNatural(tile) && isNatural(other) && tile != other;
    }

    private boolean isNatural(char tile) {
        return "gfsnvbmqw".indexOf(tile) >= 0;
    }

    private Color edgeColor(char tile, char other) {
        if (tile == 'w' || other == 'w') {
            return new Color(122, 191, 139, 170);
        }
        Color a = Terrain.color(tile);
        Color b = Terrain.color(other);
        return new Color((a.getRed() + b.getRed()) / 2, (a.getGreen() + b.getGreen()) / 2, (a.getBlue() + b.getBlue()) / 2, 112);
    }

    private void drawRoadConnectors(Graphics2D g, int camX, int camY) {
        int ts = tileSize();
        for (int sy = 0; sy < lastVisibleRows; sy++) {
            for (int sx = 0; sx < lastVisibleCols; sx++) {
                int wx = camX + sx;
                int wy = camY + sy;
                char tile = state.world.tileAt(state.currentMapId, wx, wy);
                if (tile != 'r' && tile != 'q') {
                    continue;
                }
                int px = sx * ts;
                int py = sy * ts;
                Color road = state.world.kind(state.currentMapId).equals("dungeon")
                        ? new Color(93, 82, 115)
                        : new Color(176, 130, 76);
                g.setColor(new Color(52, 38, 22, 150));
                drawRoadShape(g, px + 1, py + 1, wx, wy, scaled(18));
                g.setColor(road);
                drawRoadShape(g, px, py, wx, wy, scaled(16));
                g.setColor(new Color(232, 195, 124, 80));
                if (connectsRoad(wx - 1, wy) || connectsRoad(wx + 1, wy)) {
                    g.drawLine(px + scaled(4), py + ts / 2 - scaled(5), px + ts - scaled(4), py + ts / 2 - scaled(5));
                } else {
                    g.drawLine(px + ts / 2 - scaled(5), py + scaled(4), px + ts / 2 - scaled(5), py + ts - scaled(4));
                }
            }
        }
    }

    private void drawRoadShape(Graphics2D g, int px, int py, int wx, int wy, int half) {
        int c = tileSize() / 2;
        g.fillRect(px + c - half / 2, py + c - half / 2, half, half);
        if (connectsRoad(wx, wy - 1)) {
            g.fillRect(px + c - half / 2, py, half, c);
        }
        if (connectsRoad(wx + 1, wy)) {
            g.fillRect(px + c, py + c - half / 2, c, half);
        }
        if (connectsRoad(wx, wy + 1)) {
            g.fillRect(px + c - half / 2, py + c, half, c);
        }
        if (connectsRoad(wx - 1, wy)) {
            g.fillRect(px, py + c - half / 2, c, half);
        }
    }

    private boolean connectsRoad(int x, int y) {
        char tile = state.world.tileAt(state.currentMapId, x, y);
        return tile == 'r' || tile == 'q' || tile == 'c' || tile == 'u' || tile == 'd';
    }

    private void drawSettlementOverlays(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        drawSettlement(g, camX, camY, 82, 105, "city_overworld_cluster_large", 150);
        drawSettlement(g, camX, camY, 152, 145, "city_overworld_cluster_large", 150);
        drawSettlement(g, camX, camY, 205, 78, "city_overworld_cluster_large", 150);
        drawSettlement(g, camX, camY, 228, 185, "city_overworld_cluster_large", 150);
        drawSettlement(g, camX, camY, 150, 230, "city_overworld_cluster_large", 150);
        drawSettlement(g, camX, camY, 112, 158, "city_overworld_village_green", 112);
        drawSettlement(g, camX, camY, 83, 62, "city_overworld_village_snow", 112);
        drawSettlement(g, camX, camY, 102, 245, "city_overworld_village_desert", 112);
        drawSettlement(g, camX, camY, 240, 153, "city_overworld_village_marsh", 112);
    }

    private void drawSettlement(Graphics2D g, int camX, int camY, int wx, int wy, String asset, int size) {
        if (wx < camX - 3 || wy < camY - 3 || wx >= camX + lastVisibleCols + 3 || wy >= camY + lastVisibleRows + 3) {
            return;
        }
        int ts = tileSize();
        int drawSize = scaled(size);
        int px = (wx - camX) * ts + ts / 2 - drawSize / 2;
        int py = (wy - camY) * ts + ts / 2 - drawSize / 2;
        drawShadow(g, px + drawSize / 5, py + drawSize - scaled(18), drawSize * 3 / 5, scaled(16));
        g.drawImage(assets.spriteFit(asset, drawSize, drawSize), px, py, null);
    }

    private void drawShadow(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 85));
        g.fillOval(x, y, w, h);
    }

    private void drawCloudLayer(Graphics2D g, int camX, int camY) {
        if (!WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            return;
        }
        Graphics2D cloudG = (Graphics2D) g.create();
        cloudG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
        for (int i = 0; i < 4; i++) {
            int sizeW = scaled(190 + i * 18);
            int sizeH = scaled(92 + i * 8);
            int drift = (frame * (1 + i) / 2 + i * 173) % (GameConfig.MAP_COLS * GameConfig.TILE + 260);
            int x = drift - 220;
            int y = scaled(18 + i * 38) + (int) Math.round(Math.sin((frame + i * 37) * 0.025) * scaled(5));
            if (i == 2) {
                x = GameConfig.MAP_COLS * GameConfig.TILE - drift / 2;
                y += scaled(40);
            }
            cloudG.drawImage(assets.image("cloud_billow_" + (i % 3), sizeW, sizeH), x, y, null);
        }
        cloudG.dispose();
    }

    private String terrainImageName(char tile, int wx, int wy) {
        String base = Terrain.assetName(tile);
        int count = switch (base) {
            case "grass", "forest", "tundra" -> 8;
            case "water", "road", "desert", "marsh", "badlands", "mountain", "mountain_massif_tile" -> 4;
            default -> 1;
        };
        if (count <= 1) {
            return base;
        }
        int variant = Math.abs((wx * 928371 + wy * 364479 + base.hashCode()) % count);
        if (variant == 0) {
            return base;
        }
        return base + "_variant_" + variant;
    }

    private void drawSidebar(Graphics2D g) {
        int left = GameConfig.MAP_COLS * GameConfig.TILE;
        g.setColor(new Color(18, 20, 29));
        g.fillRect(left, 0, GameConfig.SIDEBAR_WIDTH, getHeight());
        g.setColor(new Color(59, 64, 82));
        g.drawLine(left, 0, left, getHeight());

        Actor p = state.player;
        int x = left + 28;
        int y = 42;
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(243, 238, 219));
        g.drawString(p.className, x, y);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("Level " + p.level + "   Gold " + p.gold, x, y + 28);
        bar(g, x, y + 52, p.hp, p.maxHp, new Color(190, 76, 82), "HP");
        bar(g, x, y + 86, p.mp, p.maxMp, new Color(84, 129, 205), "MP");
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(198, 202, 211));
        String partyLine = state.allies.isEmpty()
                ? "Party: no allies"
                : "Party: " + String.join(", ", state.allies.stream().map(ally -> ally.name).toList());
        wrap(g, partyLine, x, y + 122, GameConfig.SIDEBAR_WIDTH - 56, 17);

        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Position", x, y + 158);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString(state.world.label(state.currentMapId), x, y + 183);
        g.drawString(state.playerX + ", " + state.playerY + "  " + Terrain.name(state.world.tileAt(state.currentMapId, state.playerX, state.playerY)), x, y + 205);
        wrap(g, state.status, x, y + 234, GameConfig.SIDEBAR_WIDTH - 56, 20);

        int moveY = 302;
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Move", x, moveY);
        sidebarButton(g, left + 78, moveY + 14, 78, 28, "Up", () -> state.move(0, -1));
        sidebarButton(g, left + 28, moveY + 48, 78, 28, "Left", () -> state.move(-1, 0));
        sidebarButton(g, left + 112, moveY + 48, 78, 28, "Down", () -> state.move(0, 1));
        sidebarButton(g, left + 196, moveY + 48, 78, 28, "Right", () -> state.move(1, 0));

        int actionY = moveY + 100;
        sidebarButton(g, left + 28, actionY, 246, 30, "Talk / Enter", state::interact);
        sidebarButton(g, left + 28, actionY + 38, 246, 30, "Quest Log", state::toggleQuestLog);
        sidebarButton(g, left + 28, actionY + 76, 246, 30, "World Map", state::toggleWorldMap);
        sidebarButton(g, left + 28, actionY + 114, 116, 30, "Skills (" + state.player.skillPoints + ")", state::toggleSkills);
        sidebarButton(g, left + 158, actionY + 114, 116, 30, "Inventory", state::toggleInventory);

        int miniY = actionY + 166;
        drawMiniMap(g, left + 28, miniY, 112, 70);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Zoom", left + 158, miniY + 12);
        sidebarButton(g, left + 158, miniY + 24, 34, 26, "-", () -> state.adjustZoom(-10));
        sidebarButton(g, left + 240, miniY + 24, 34, 26, "+", () -> state.adjustZoom(10));
        g.setColor(new Color(70, 76, 94));
        g.drawRect(left + 196, miniY + 35, 38, 5);
        g.setColor(new Color(235, 211, 132));
        int knobX = left + 196 + (int) Math.round((state.zoom - 70) / 80.0 * 38);
        g.fillOval(knobX - 4, miniY + 31, 9, 9);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        g.drawString(state.zoom + "%", left + 158, miniY + 66);

        int controlsY = getHeight() - 96;
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.setColor(new Color(230, 225, 206));
        g.drawString("Log", x, controlsY);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(new Color(198, 202, 211));
        wrap(g, state.status, x, controlsY + 24, GameConfig.SIDEBAR_WIDTH - 56, 18);
    }

    private void sidebarButton(Graphics2D g, int x, int y, int w, int h, String label, Runnable action) {
        actionButton(g, x, y, w, h, label, action, new Color(35, 39, 54), new Color(86, 98, 128), true);
    }

    private void actionButton(
            Graphics2D g,
            int x,
            int y,
            int w,
            int h,
            String label,
            Runnable action,
            Color fill,
            Color border,
            boolean enabled
    ) {
        if (enabled) {
            buttons.add(new UiButton(new Rectangle(x, y, w, h), label, () -> {
                action.run();
                repaint();
            }));
        }
        g.setColor(enabled ? fill : new Color(42, 44, 52));
        g.fillRoundRect(x, y, w, h, 6, 6);
        g.setColor(enabled ? border : new Color(73, 75, 84));
        g.drawRoundRect(x, y, w, h, 6, 6);
        g.setFont(new Font("SansSerif", Font.BOLD, h >= 34 ? 13 : 12));
        g.setColor(enabled ? new Color(238, 239, 244) : new Color(142, 146, 156));
        FontMetrics metrics = g.getFontMetrics();
        String display = label;
        while (metrics.stringWidth(display) > w - 12 && display.length() > 4) {
            display = display.substring(0, display.length() - 4) + "...";
        }
        g.drawString(display, x + (w - metrics.stringWidth(display)) / 2, y + h / 2 + 5);
    }

    private void drawMiniMap(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(11, 13, 20));
        g.fillRect(x, y, w, h);
        int stepX = Math.max(1, WorldMap.COLS / w);
        int stepY = Math.max(1, WorldMap.ROWS / h);
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                char tile = state.world.tileAt(WorldMap.OVERWORLD_ID, px * stepX, py * stepY);
                g.setColor(Terrain.color(tile));
                g.fillRect(x + px, y + py, 1, 1);
            }
        }
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            int playerX = x + Math.min(w - 1, state.playerX * w / WorldMap.COLS);
            int playerY = y + Math.min(h - 1, state.playerY * h / WorldMap.ROWS);
            g.setColor(new Color(255, 245, 174));
            g.fillOval(playerX - 2, playerY - 2, 5, 5);
        }
        g.setColor(new Color(81, 88, 108));
        g.drawRect(x, y, w, h);
    }

    private void drawBattle(Graphics2D g) {
        Battle battle = state.battle;
        if (battle == null) {
            return;
        }
        int x = 130;
        int y = 90;
        int w = 890;
        int h = 620;
        drawBattleBackdrop(g, x, y, w, h);
        g.setColor(new Color(124, 112, 83));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 8, 8);

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Battle", x + 34, y + 52);
        Actor active = battle.activeActor();
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString(battle.enemy.name, x + 590, y + 96);
        g.drawImage(assets.sprite(battle.enemy.sprite, 144), x + 610, y + 130, null);
        bar(g, x + 560, y + 300, battle.enemy.hp, battle.enemy.maxHp, new Color(190, 76, 82), "Enemy HP");
        drawStatusIcons(g, battle, battle.enemy, x + 560, y + 328);

        List<Actor> party = battle.partyMembers();
        int visibleParty = Math.min(4, party.size());
        for (int i = 0; i < visibleParty; i++) {
            drawBattlePartyMember(g, party.get(i), party.get(i) == active, x + 64 + i * 120, y + 108);
        }
        if (party.size() > visibleParty) {
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.setColor(new Color(196, 198, 205));
            g.drawString("+" + (party.size() - visibleParty) + " waiting", x + 66, y + 346);
        }
        if (active != null) {
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(246, 224, 151));
            g.drawString(active.name + "'s turn", x + 66, y + 374);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(218, 220, 226));
            String abilityText = active.abilities.isEmpty()
                    ? "No abilities"
                    : "1-" + active.abilities.size() + " " + active.abilities.stream().map(Ability::name).toList();
            wrap(g, abilityText, x + 66, y + 398, 380, 18);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(218, 220, 226));
        int logY = y + 450;
        for (String message : battle.log) {
            g.drawString(message, x + 54, logY);
            logY += 24;
        }
        drawBattleActionBar(g, battle, x + 38, y + h - 124, w - 76, 92);
    }

    private void drawBattleBackdrop(Graphics2D g, int x, int y, int w, int h) {
        g.drawImage(assets.cover(battleBackdropName(), w, h), x, y, null);
        g.setPaint(new GradientPaint(x, y, new Color(8, 10, 16, 70), x, y + h, new Color(8, 10, 16, 218)));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setPaint(null);
        g.setColor(new Color(20, 18, 18, 92));
        g.fillRoundRect(x + 22, y + h - 170, w - 44, 138, 8, 8);
    }

    private String battleBackdropName() {
        char terrain = state.world.tileAt(state.currentMapId, state.playerX, state.playerY);
        if ("dungeon".equals(state.world.kind(state.currentMapId)) || terrain == 'd') {
            if (state.currentMapId.contains("blackvault")) {
                return "battle_dungeon_crypt_backdrop";
            }
            if (state.currentMapId.contains("miredepth") || state.currentMapId.contains("frosthollow")) {
                return "battle_dungeon_cavern_backdrop";
            }
            return "battle_dungeon_hall_backdrop";
        }
        return switch (terrain) {
            case 'n' -> "battle_snow_backdrop";
            case 's', 'b' -> "battle_desert_backdrop";
            case 'm', 'q' -> "battle_mountain_backdrop";
            case 'g', 'r' -> "battle_plains_backdrop";
            default -> "battle_forest_backdrop";
        };
    }

    private void drawBattleActionBar(Graphics2D g, Battle battle, int x, int y, int w, int h) {
        g.setColor(new Color(12, 14, 22, 224));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(94, 101, 126));
        g.drawRoundRect(x, y, w, h, 8, 8);
        if (battle.finished) {
            String label = battle.victory ? "Continue" : "Revive";
            Runnable action = battle.victory ? state::leaveFinishedBattle : state::revive;
            Color fill = battle.victory ? new Color(66, 93, 49) : new Color(106, 61, 61);
            actionButton(g, x + 18, y + 26, 140, 40, label, action, fill, new Color(151, 177, 112), true);
            g.setFont(new Font("SansSerif", Font.BOLD, 16));
            g.setColor(new Color(238, 232, 210));
            g.drawString(battle.victory ? "Victory rewards are ready." : "Return to Oakhaven and recover.", x + 180, y + 51);
            return;
        }
        Actor active = battle.activeActor();
        boolean canAct = active != null && active.alive();
        actionButton(g, x + 18, y + 14, 112, 34, "Attack", state::battleAttack, new Color(92, 79, 50), new Color(156, 132, 76), canAct);
        actionButton(g, x + 138, y + 14, 104, 34, "Potion", () -> state.useItem("potion_small"), new Color(53, 82, 70), new Color(98, 151, 117), state.player.hasItem("potion_small"));
        actionButton(g, x + 250, y + 14, 104, 34, "Ether", () -> state.useItem("ether"), new Color(54, 74, 103), new Color(104, 132, 176), state.player.hasItem("ether"));
        int abilityX = x + 372;
        List<Ability> abilities = active == null ? List.of() : active.abilities;
        for (int i = 0; i < Math.min(3, abilities.size()); i++) {
            Ability ability = abilities.get(i);
            int buttonX = abilityX + i * 138;
            boolean enabled = canAct && active.mp >= ability.cost();
            String label = (i + 1) + " " + ability.name() + " " + ability.cost() + "MP";
            int index = i;
            actionButton(g, buttonX, y + 14, 130, 34, label, () -> state.battleAbility(index), new Color(76, 53, 93), new Color(139, 107, 168), enabled);
        }
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(new Color(186, 190, 202));
        g.drawString("Keyboard: A/Space attack, 1-3 abilities, H potion, J ether.", x + 20, y + 72);
    }

    private void drawBattlePartyMember(Graphics2D g, Actor actor, boolean active, int x, int y) {
        g.setColor(active ? new Color(78, 88, 116) : new Color(30, 34, 45));
        g.fillRoundRect(x - 8, y - 10, 104, 220, 8, 8);
        g.setColor(active ? new Color(246, 224, 151) : new Color(86, 98, 128));
        g.drawRoundRect(x - 8, y - 10, 104, 220, 8, 8);
        g.drawImage(assets.sprite(actor.sprite, 72), x + 8, y + 10, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(238, 239, 244));
        drawCenteredIn(g, actor.name, x - 8, y + 105, 104);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(198, 202, 211));
        drawCenteredIn(g, actor.className, x - 8, y + 124, 104);
        smallBar(g, x, y + 142, actor.hp, actor.maxHp, new Color(190, 76, 82), "HP");
        smallBar(g, x, y + 166, actor.mp, actor.maxMp, new Color(84, 129, 205), "MP");
        if (state.battle != null) {
            drawStatusIcons(g, state.battle, actor, x, y + 190);
        }
        if (!actor.alive()) {
            g.setColor(new Color(9, 10, 16, 150));
            g.fillRoundRect(x - 8, y - 10, 104, 220, 8, 8);
            g.setColor(new Color(238, 239, 244));
            drawCenteredIn(g, "Down", x - 8, y + 92, 104);
        }
    }

    private void drawStatusIcons(Graphics2D g, Battle battle, Actor actor, int x, int y) {
        int offset = 0;
        for (EffectStack stack : battle.statusesFor(actor)) {
            g.setColor("buff".equals(stack.effect.affectType()) ? new Color(63, 91, 69) : new Color(95, 57, 61));
            g.fillRoundRect(x + offset, y, 28, 20, 5, 5);
            g.setColor(new Color(226, 229, 236));
            g.setFont(new Font("SansSerif", Font.BOLD, 10));
            g.drawString(StatusEffects.iconLabel(stack.effect.key()), x + offset + 5, y + 14);
            offset += 32;
            if (offset > 160) {
                break;
            }
        }
    }

    private void drawDialog(Graphics2D g) {
        Npc npc = state.activeNpc;
        if (npc == null) {
            return;
        }
        drawOverlayBase(g, 170, 560, 810, 220);
        g.drawImage(assets.sprite(npc.sprite(), 86), 210, 610, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.setColor(new Color(244, 239, 220));
        g.drawString(npc.name(), 320, 620);
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.setColor(new Color(218, 220, 226));
        String line = npc.dialog().isEmpty() ? "..." : npc.dialog().get(Math.min(state.dialogIndex, npc.dialog().size() - 1));
        wrap(g, line, 320, 654, 600, 24);
        Quest quest = npc.questId() == null ? null : state.quests.get(npc.questId());
        if (quest != null) {
            g.setColor(new Color(246, 224, 151));
            String questText = quest.completed ? "Quest complete" : quest.accepted ? quest.title + " " + quest.progress + "/" + quest.needed : "Quest available: " + quest.title;
            g.drawString(questText, 320, 728);
        }
        if (npc.recruitId() != null && npc.recruitCost() > 0 && !state.isRecruited(npc.recruitId())) {
            sidebarButton(g, 760, 724, 150, 30, "Hire " + npc.recruitCost() + "g", state::hireActiveRecruit);
        } else if (npc.recruitId() != null && state.isRecruited(npc.recruitId())) {
            g.setColor(new Color(144, 215, 150));
            g.drawString("Travels with you", 760, 746);
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Enter/E continues. H hires when available.", 320, 756);
        String continueLabel = activeDialogWillOpenShop(npc) ? "Open Shop" : "Continue";
        actionButton(g, 600, 724, 140, 30, continueLabel, state::advanceDialog, new Color(58, 72, 100), new Color(107, 126, 166), true);
        actionButton(g, 918, 724, 46, 30, "Esc", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private boolean activeDialogWillOpenShop(Npc npc) {
        return state.dialogIndex >= npc.dialog().size() - 1 && state.activeShop != null;
    }

    private void drawQuestLog(Graphics2D g) {
        drawOverlayBase(g, 210, 110, 740, 650);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Quest Log", 250, 160);
        int y = 210;
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        for (Quest quest : state.quests.values()) {
            if (!quest.accepted && !quest.completed) {
                continue;
            }
            g.setColor(quest.completed ? new Color(144, 215, 150) : new Color(246, 224, 151));
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.drawString(quest.title + "  " + quest.progress + "/" + quest.needed, 250, y);
            y += 24;
            g.setFont(new Font("SansSerif", Font.PLAIN, 15));
            g.setColor(new Color(210, 213, 222));
            wrap(g, quest.description, 250, y, 620, 20);
            y += 54;
        }
        if (y == 210) {
            g.setColor(new Color(210, 213, 222));
            g.drawString("No accepted quests yet.", 250, y);
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Q or Esc close", 250, 725);
    }

    private void drawSkillTree(Graphics2D g) {
        int x = 170;
        int y = 72;
        int w = 920;
        int h = 760;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Skills", x + 36, y + 48);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(210, 213, 222));
        g.drawString(state.player.className + "  Level " + state.player.level + "  Points " + state.player.skillPoints, x + 36, y + 78);

        drawSkillColumn(g, "Common", SkillTrees.COMMON_SKILL_TREE, x + 36, y + 118, 404);
        drawSkillColumn(g, state.player.className, SkillTrees.skillTreeForClass(state.player.className), x + 480, y + 118, 404);

        int spent = SkillTrees.spent(state.player.skillAllocations);
        int respecCost = SkillTrees.respecCost(state.player.level, spent);
        boolean canRespec = spent > 0 && state.player.gold >= respecCost;
        actionButton(g, x + w - 330, y + h - 54, 154, 34, "Respec " + respecCost + "g", state::respecSkills, new Color(96, 66, 59), new Color(165, 103, 88), canRespec);
        actionButton(g, x + w - 160, y + h - 54, 120, 34, "Close", state::toggleSkills, new Color(58, 72, 100), new Color(107, 126, 166), true);
    }

    private void drawSkillColumn(Graphics2D g, String title, Map<String, SkillNode> tree, int x, int y, int w) {
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.setColor(new Color(246, 224, 151));
        g.drawString(title, x, y);
        int rowY = y + 22;
        for (SkillNode node : tree.values()) {
            drawSkillNode(g, node, x, rowY, w);
            rowY += 60;
        }
    }

    private void drawSkillNode(Graphics2D g, SkillNode node, int x, int y, int w) {
        int rank = state.player.skillRank(node.id());
        boolean learned = rank > 0;
        boolean canLearn = canShowAllocate(node);
        g.setColor(learned ? new Color(38, 50, 45, 232) : new Color(28, 32, 43, 232));
        g.fillRoundRect(x, y, w, 52, 8, 8);
        g.setColor(learned ? new Color(103, 151, 117) : canLearn ? new Color(96, 106, 133) : new Color(65, 68, 82));
        g.drawRoundRect(x, y, w, 52, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(new Color(235, 236, 240));
        g.drawString(node.name() + "  " + rank + "/" + node.maxRank(), x + 12, y + 19);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(176, 182, 196));
        g.drawString(shortText(node.description(), 42), x + 12, y + 38);
        actionButton(g, x + w - 74, y + 12, 58, 28, "+", () -> state.allocateSkill(node.id()), new Color(68, 90, 53), new Color(110, 139, 92), canLearn);
    }

    private boolean canShowAllocate(SkillNode node) {
        if (state.player.skillPoints <= 0 || state.player.skillRank(node.id()) >= node.maxRank()) {
            return false;
        }
        if (!SkillTrees.availableSkillTree(state.player.className).containsKey(node.id())) {
            return false;
        }
        for (String required : node.requires()) {
            if (state.player.skillRank(required) <= 0) {
                return false;
            }
        }
        return true;
    }

    private String shortText(String text, int maxChars) {
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private void drawInventory(Graphics2D g) {
        drawOverlayBase(g, 190, 105, 820, 650);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("Inventory", 230, 155);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(198, 202, 211));
        g.drawString("ATK " + state.player.attack + "   DEF " + state.player.defense + "   Skill Points " + state.player.skillPoints, 230, 184);
        int slotY = 225;
        for (String slot : List.of("weapon", "armor", "accessory")) {
            Equipment equipment = state.player.equippedItem(slot);
            g.setColor(new Color(28, 32, 43));
            g.fillRoundRect(230, slotY - 28, 320, 42, 8, 8);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(230, slotY - 28, 320, 42, 8, 8);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            g.setColor(new Color(246, 224, 151));
            g.drawString(slot.toUpperCase(), 246, slotY - 3);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(222, 225, 232));
            g.drawString(equipment == null ? "None" : equipment.name(), 352, slotY - 3);
            if (equipment != null) {
                actionButton(g, 456, slotY - 22, 76, 30, "Unequip", () -> state.unequipSlot(slot), new Color(88, 56, 56), new Color(149, 96, 88), true);
            }
            slotY += 52;
        }

        int y = 225;
        int index = 1;
        g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        for (var entry : state.player.inventory.entrySet()) {
            int buttonIndex = index - 1;
            Item item = GameData.ITEMS.get(entry.getKey());
            Equipment equipment = GameData.EQUIPMENT.get(entry.getKey());
            String action = equipment == null ? "Use" : "Equip";
            g.setColor(new Color(30, 34, 45));
            g.fillRoundRect(590, y - 28, 360, 46, 6, 6);
            g.setColor(new Color(82, 92, 116));
            g.drawRoundRect(590, y - 28, 360, 46, 6, 6);
            g.drawImage(assets.sprite(GameData.itemIcon(entry.getKey()), 34), 600, y - 22, null);
            g.setColor(new Color(222, 225, 232));
            g.drawString(index + ". " + GameData.itemName(entry.getKey()) + " x" + entry.getValue(), 642, y - 2);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.setColor(new Color(176, 182, 196));
            String detail = equipment != null ? String.join("  ", equipment.statLines()) : item == null ? "" : itemBenefit(item);
            g.drawString(detail, 642, y + 14);
            g.setFont(new Font("SansSerif", Font.PLAIN, 17));
            actionButton(g, 860, y - 20, 72, 30, action, () -> state.useInventoryItem(buttonIndex), new Color(68, 90, 53), new Color(110, 139, 92), true);
            y += 54;
            index++;
        }
        if (index == 1) {
            g.setColor(new Color(210, 213, 222));
            g.drawString("Your pack is empty.", 590, y);
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Press item number to use/equip. I or Esc close.", 230, 714);
        actionButton(g, 846, 694, 124, 34, "Close", state::toggleInventory, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private void drawShop(Graphics2D g) {
        Shop shop = state.activeShop;
        if (shop == null) {
            return;
        }
        int panelX = 220;
        int panelY = 110;
        int panelW = 760;
        int panelH = 610;
        drawOverlayBase(g, panelX, panelY, panelW, panelH);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString(shop.name(), panelX + 40, panelY + 50);
        g.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g.setColor(new Color(210, 213, 222));
        g.drawString("Gold: " + state.player.gold, panelX + 40, panelY + 84);
        if (state.activeNpc != null) {
            g.drawImage(assets.spriteFit(state.activeNpc.sprite() + "_model", 74, 96), panelX + panelW - 126, panelY + 24, null);
        }
        int y = panelY + 136;
        for (int i = 0; i < shop.stock().size(); i++) {
            int buttonIndex = i;
            String itemKey = shop.stock().get(i);
            Item item = GameData.ITEMS.get(itemKey);
            Equipment equipment = GameData.EQUIPMENT.get(itemKey);
            int cost = GameData.itemCost(itemKey);
            if (cost <= 0) {
                continue;
            }
            boolean affordable = state.player.gold >= cost;
            g.setColor(new Color(28, 32, 43, affordable ? 238 : 168));
            g.fillRoundRect(panelX + 36, y, panelW - 72, 72, 8, 8);
            g.setColor(affordable ? new Color(82, 92, 116) : new Color(70, 64, 70));
            g.drawRoundRect(panelX + 36, y, panelW - 72, 72, 8, 8);
            g.drawImage(assets.sprite(GameData.itemIcon(itemKey), 42), panelX + 54, y + 15, null);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(new Color(235, 236, 240));
            g.drawString((i + 1) + ". " + GameData.itemName(itemKey), panelX + 112, y + 28);
            g.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g.setColor(new Color(176, 182, 196));
            g.drawString(equipment == null ? itemBenefit(item) : equipmentBenefit(equipment), panelX + 112, y + 52);
            Color buyFill = affordable ? new Color(68, 90, 53) : new Color(58, 59, 66);
            actionButton(g, panelX + panelW - 190, y + 18, 116, 36, "Buy " + cost + "g", () -> state.buyShopItem(buttonIndex), buyFill, new Color(110, 139, 92), affordable);
            y += 84;
        }
        if (state.activeNpc != null && state.activeNpc.recruitId() != null && state.activeNpc.recruitCost() > 0) {
            if (state.isRecruited(state.activeNpc.recruitId())) {
                g.setColor(new Color(144, 215, 150));
                g.drawString(state.activeNpc.name() + " travels with you.", panelX + 40, y + 10);
            } else {
                actionButton(g, panelX + 40, y - 8, 238, 34, "Hire " + state.activeNpc.name() + " - " + state.activeNpc.recruitCost() + "g", state::hireActiveRecruit, new Color(69, 62, 88), new Color(125, 107, 166), true);
            }
        }
        g.setColor(new Color(196, 198, 205));
        g.drawString("Number keys buy matching items. H hires when available.", panelX + 40, panelY + panelH - 32);
        actionButton(g, panelX + panelW - 176, panelY + panelH - 52, 132, 34, "Leave Shop", state::closeOverlay, new Color(83, 61, 61), new Color(149, 96, 88), true);
    }

    private String itemBenefit(Item item) {
        if (item == null) {
            return "Field provision";
        }
        if (item.heal() > 0 && item.mp() > 0) {
            return "Restores " + item.heal() + " HP and " + item.mp() + " MP";
        }
        if (item.heal() > 0) {
            return "Restores " + item.heal() + " HP";
        }
        if (item.mp() > 0) {
            return "Restores " + item.mp() + " MP";
        }
        return "Field provision";
    }

    private String equipmentBenefit(Equipment equipment) {
        String stats = String.join("  ", equipment.statLines());
        return stats.isBlank() ? equipment.description() : equipment.slot() + "   " + stats;
    }

    private void drawWorldMap(Graphics2D g) {
        int x = 170;
        int y = 64;
        int w = 920;
        int h = 760;
        drawOverlayBase(g, x, y, w, h);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(244, 239, 220));
        g.drawString("World Map", x + 36, y + 48);
        int mapX = x + 36;
        int mapY = y + 80;
        int mapW = 720;
        int mapH = 640;
        g.setColor(new Color(8, 10, 16));
        g.fillRect(mapX, mapY, mapW, mapH);
        for (int py = 0; py < mapH; py++) {
            int wy = py * WorldMap.ROWS / mapH;
            for (int px = 0; px < mapW; px++) {
                int wx = px * WorldMap.COLS / mapW;
                char tile = state.world.tileAt(WorldMap.OVERWORLD_ID, wx, wy);
                g.setColor(Terrain.color(tile));
                g.fillRect(mapX + px, mapY + py, 1, 1);
            }
        }
        drawMapMarker(g, mapX, mapY, mapW, mapH, 82, 105, "Riverside");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 152, 145, "Archive");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 205, 78, "Highwall");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 228, 185, "Belltower");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 150, 230, "Sanctum");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 112, 158, "Oakhaven");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 83, 62, "Snowrest");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 102, 245, "Dunewick");
        drawMapMarker(g, mapX, mapY, mapW, mapH, 240, 153, "Mireford");
        if (WorldMap.OVERWORLD_ID.equals(state.currentMapId)) {
            int px = mapX + state.playerX * mapW / WorldMap.COLS;
            int py = mapY + state.playerY * mapH / WorldMap.ROWS;
            g.setColor(new Color(255, 245, 174));
            g.fillOval(px - 5, py - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.drawOval(px - 5, py - 5, 10, 10);
        }
        g.setColor(new Color(81, 88, 108));
        g.drawRect(mapX, mapY, mapW, mapH);
        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(new Color(210, 213, 222));
        wrap(g, "Use this as a navigation reference. Yellow marks cities, villages, and your position when you are on the overworld.", x + 780, y + 96, 100, 20);
        sidebarButton(g, x + 780, y + h - 70, 96, 30, "Close", state::toggleWorldMap);
    }

    private void drawMapMarker(Graphics2D g, int mapX, int mapY, int mapW, int mapH, int wx, int wy, String label) {
        int px = mapX + wx * mapW / WorldMap.COLS;
        int py = mapY + wy * mapH / WorldMap.ROWS;
        g.setColor(new Color(245, 214, 117));
        g.fillRect(px - 3, py - 3, 7, 7);
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString(label, px + 6, py + 4);
    }

    private void drawOverlayBase(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(9, 10, 16, 226));
        g.fillRoundRect(x, y, w, h, 8, 8);
        g.setColor(new Color(124, 112, 83));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(x, y, w, h, 8, 8);
    }

    private void bar(Graphics2D g, int x, int y, int value, int max, Color fill, String label) {
        int width = 220;
        int height = 18;
        g.setColor(new Color(40, 42, 52));
        g.fillRoundRect(x, y, width, height, 6, 6);
        int filled = max <= 0 ? 0 : (int) Math.round(width * Math.max(0.0, Math.min(1.0, value / (double) max)));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, height, 6, 6);
        g.setColor(new Color(235, 235, 235));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(label + " " + value + "/" + max, x + 8, y + 14);
    }

    private void smallBar(Graphics2D g, int x, int y, int value, int max, Color fill, String label) {
        int width = 80;
        int height = 14;
        g.setColor(new Color(40, 42, 52));
        g.fillRoundRect(x, y, width, height, 5, 5);
        int filled = max <= 0 ? 0 : (int) Math.round(width * Math.max(0.0, Math.min(1.0, value / (double) max)));
        g.setColor(fill);
        g.fillRoundRect(x, y, filled, height, 5, 5);
        g.setColor(new Color(235, 235, 235));
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString(label + " " + value + "/" + max, x + 5, y + 11);
    }

    private void wrap(Graphics2D g, String text, int x, int y, int width, int lineHeight) {
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) > width && !line.isEmpty()) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        for (String output : lines) {
            g.drawString(output, x, y);
            y += lineHeight;
        }
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }

    private void drawCenteredIn(Graphics2D g, String text, int x, int y, int width) {
        FontMetrics metrics = g.getFontMetrics();
        g.drawString(text, x + (width - metrics.stringWidth(text)) / 2, y);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int tileSize() {
        return Math.max(24, GameConfig.TILE * state.zoom / 100);
    }

    private int scaled(int value) {
        return Math.max(1, value * state.zoom / 100);
    }

    private final class Keys extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent event) {
            int code = event.getKeyCode();
            if (state.mode == GameMode.CLASS_SELECT) {
                if (code == KeyEvent.VK_1) {
                    state.chooseClass("Knight");
                } else if (code == KeyEvent.VK_2) {
                    state.chooseClass("Mage");
                } else if (code == KeyEvent.VK_3) {
                    state.chooseClass("Ranger");
                } else if (code == KeyEvent.VK_F9) {
                    load();
                }
                repaint();
                return;
            }

            if (code == KeyEvent.VK_F5) {
                save();
            } else if (code == KeyEvent.VK_F9) {
                load();
            } else if (code == KeyEvent.VK_ESCAPE) {
                if (state.mode != GameMode.EXPLORE && state.mode != GameMode.BATTLE) {
                    state.closeOverlay();
                }
            } else if (state.mode == GameMode.BATTLE) {
                handleBattleKey(code);
            } else if (state.mode == GameMode.DIALOG) {
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) {
                    state.advanceDialog();
                } else if (code == KeyEvent.VK_H) {
                    state.hireActiveRecruit();
                }
            } else if (state.mode == GameMode.QUEST_LOG) {
                if (code == KeyEvent.VK_Q) {
                    state.toggleQuestLog();
                }
            } else if (state.mode == GameMode.SKILLS) {
                if (code == KeyEvent.VK_K || code == KeyEvent.VK_Q) {
                    state.toggleSkills();
                }
            } else if (state.mode == GameMode.WORLD_MAP) {
                if (code == KeyEvent.VK_M || code == KeyEvent.VK_Q) {
                    state.toggleWorldMap();
                }
            } else if (state.mode == GameMode.INVENTORY) {
                if (code == KeyEvent.VK_I) {
                    state.toggleInventory();
                } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.useInventoryItem(code - KeyEvent.VK_1);
                }
            } else if (state.mode == GameMode.SHOP) {
                if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) {
                    state.buyShopItem(code - KeyEvent.VK_1);
                } else if (code == KeyEvent.VK_H) {
                    state.hireActiveRecruit();
                }
            } else if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) {
                state.move(0, -1);
            } else if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) {
                state.move(0, 1);
            } else if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) {
                state.move(-1, 0);
            } else if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) {
                state.move(1, 0);
            } else if (code == KeyEvent.VK_E) {
                state.interact();
            } else if (code == KeyEvent.VK_Q) {
                state.toggleQuestLog();
            } else if (code == KeyEvent.VK_M) {
                state.toggleWorldMap();
            } else if (code == KeyEvent.VK_K) {
                state.toggleSkills();
            } else if (code == KeyEvent.VK_I) {
                state.toggleInventory();
            } else if (code == KeyEvent.VK_H) {
                state.useItem("potion_small");
            } else if (code == KeyEvent.VK_J) {
                state.useItem("ether");
            }
            repaint();
        }

        private void handleBattleKey(int code) {
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_SPACE) {
                state.battleAttack();
            } else if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_3) {
                state.battleAbility(code - KeyEvent.VK_1);
            } else if (code == KeyEvent.VK_ENTER) {
                state.leaveFinishedBattle();
            } else if (code == KeyEvent.VK_R && state.battle != null && state.battle.finished && !state.battle.victory) {
                state.revive();
            } else if (code == KeyEvent.VK_H) {
                state.useItem("potion_small");
            } else if (code == KeyEvent.VK_J) {
                state.useItem("ether");
            }
        }

        private void save() {
            try {
                saves.save(state);
                state.status = "Saved Java game.";
            } catch (IOException ex) {
                state.status = "Save failed: " + ex.getMessage();
            }
        }

        private void load() {
            try {
                if (!saves.load(state)) {
                    state.status = "No Java save found.";
                }
            } catch (IOException ex) {
                state.status = "Load failed: " + ex.getMessage();
            }
        }
    }

    private final class Mouse extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            requestFocusInWindow();
            for (int i = buttons.size() - 1; i >= 0; i--) {
                UiButton button = buttons.get(i);
                if (button.contains(event.getX(), event.getY())) {
                    button.action().run();
                    return;
                }
            }
            if (state.mode == GameMode.EXPLORE && event.getX() < GameConfig.MAP_COLS * GameConfig.TILE && event.getY() < GameConfig.MAP_ROWS * GameConfig.TILE) {
                clickWorld(event.getX(), event.getY());
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent event) {
            state.adjustZoom(event.getWheelRotation() < 0 ? 10 : -10);
            repaint();
        }

        private void clickWorld(int x, int y) {
            int tileSize = tileSize();
            int targetX = lastCamX + x / tileSize;
            int targetY = lastCamY + y / tileSize;
            int dx = targetX - state.playerX;
            int dy = targetY - state.playerY;
            if (dx == 0 && dy == 0) {
                state.interact();
            } else if (Math.abs(dx) + Math.abs(dy) == 1) {
                state.move(Integer.signum(dx), Integer.signum(dy));
            } else {
                state.status = "Click an adjacent tile to move.";
            }
            repaint();
        }
    }
}
