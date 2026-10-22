package poweredminecart;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.network.Packet;
import necesse.engine.registries.MobRegistry;
import necesse.engine.save.LoadData;
import necesse.engine.save.SaveData;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.entity.mobs.*;
import necesse.entity.mobs.networkField.BooleanNetworkField;
import necesse.entity.mobs.summon.MinecartLinePos;
import necesse.entity.mobs.summon.MinecartLines;
import necesse.entity.mobs.summon.MinecartMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.lootTable.LootItemInterface;
import necesse.inventory.lootTable.LootTable;
import necesse.inventory.lootTable.lootItem.LootItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.MinecartTrackObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

import java.awt.*;
import java.util.List;

public class PoweredMinecartMob extends MinecartMob implements MountAbility {
    public static GameTexture texture;
    public static GameTexture rubiesOverlayTexture;
    public static GameTexture rubiesOverlayBrightHeadlightTexture;
    public static LootTable lootTable = new LootTable(new LootItemInterface[]{new LootItem("poweredminecart")});
    public static float speed = 230.0F;
    public static float accelMod = 0.66F;
    public static float headlightDistanceAhead = 75.0F;
    public static float maxAllowedLightLevelForAutoHeadlight = 50.0F;

    public final BooleanNetworkField headlightsOn;
    private boolean loadedFromSave = false;

    public PoweredMinecartMob() {
        super();
        this.setSpeed(speed);
        this.accelerationMod = accelMod;

        this.headlightsOn = this.registerNetworkField(new BooleanNetworkField(false));
    }

    @Override
    public LootTable getLootTable() {
        return lootTable;
    }

    public static float getHeadlightOffsetX(int minecartDir) {
        if (minecartDir == 1) return headlightDistanceAhead; // right
        else if (minecartDir == 3) return -headlightDistanceAhead; // left
        else return 0; // up or down
    }

    public static float getHeadlightOffsetY(int minecartDir) {
        if (minecartDir == 0) return -headlightDistanceAhead; // up
        else if (minecartDir == 2) return headlightDistanceAhead; // down
        else return 0; // left or right
    }

    public static void setInitialHeadlightState(Level level, int x, int y, int minecartDir, BooleanNetworkField headlightsOn) {
        if (level.lightManager.getAmbientAndStaticLightLevel(getTileCoordinate(x), getTileCoordinate(y)).getLevel() <= maxAllowedLightLevelForAutoHeadlight) {
            headlightsOn.set(true);
            return;
        }

        //Check positions ahead
        float lightOffsetX = getHeadlightOffsetX(minecartDir);
        float lightOffsetY = getHeadlightOffsetY(minecartDir);

        if ((level.lightManager.getAmbientAndStaticLightLevel(getTileCoordinate(x + (int)lightOffsetX), getTileCoordinate(y + (int)lightOffsetY)).getLevel() <= maxAllowedLightLevelForAutoHeadlight)
                || (level.lightManager.getAmbientAndStaticLightLevel(getTileCoordinate(x + (int)lightOffsetX * 2), getTileCoordinate(y + (int)lightOffsetY * 2)).getLevel() <= maxAllowedLightLevelForAutoHeadlight))
        {
            headlightsOn.set(true);
        }
    }

    public void init() {
        super.init();

        if (this.isServer() && !this.loadedFromSave) {
            setInitialHeadlightState(
                    this.getLevel(),
                    this.getX(),
                    this.getY(),
                    this.minecartDir,
                    this.headlightsOn
            );
        }
    }

    public static void refreshHeadlights(Level level, Mob cart, int minecartDir) {
        float lightOffsetX = getHeadlightOffsetX(minecartDir);
        float lightOffsetY = getHeadlightOffsetY(minecartDir);

        level.lightManager.refreshParticleLightFloat(cart.x + lightOffsetX, cart.y + lightOffsetY, 5.0F, 0.5F, 125);
        level.lightManager.refreshParticleLightFloat(cart.x + (lightOffsetX*2.0F), cart.y + (lightOffsetY*2.0F), 5.0F, 0.5F, 175);
    }

    @Override
    public void addSaveData(SaveData save) {
        super.addSaveData(save);
        save.addBoolean("minecartHeadlightsOn", this.headlightsOn.get());
    }

    @Override
    public void applyLoadData(LoadData save) {
        super.applyLoadData(save);
        this.headlightsOn.set(save.getBoolean("minecartHeadlightsOn", false));
        this.loadedFromSave = true;
    }

    @Override
    public void clientTick() {
        super.clientTick();
        if (headlightsOn.get()) refreshHeadlights(this.getLevel(), (Mob)this, this.minecartDir);
    }

    public void runMountAbility(PlayerMob player, Packet content) {
        boolean newState = !this.headlightsOn.get();
        this.headlightsOn.set(newState);

        if (this.isClient()) {
            SoundManager.playSound(GameResources.tick, SoundEffect.effect(player).pitch(0.8F));
        }
    }

    public boolean canRunMountAbility(PlayerMob player, Packet content) {
        return true;
    }

    @Override
    public void addDrawables(List<MobDrawable> list, OrderableDrawables tileList, OrderableDrawables topList, Level level, int x, int y, TickManager tickManager, GameCamera camera, PlayerMob perspective) {
        super.addDrawables(list, tileList, topList, level, x, y, tickManager, camera, perspective);
        GameLight light = level.getLightLevel(getTileCoordinate(x), getTileCoordinate(y));
        int drawX = camera.getDrawX(x) - 32;
        int drawY = camera.getDrawY(y) - 47;
        Point sprite = this.getAnimSprite(x, y, this.minecartDir);
        drawY += this.getBobbing(x, y);
        drawY += level.getTile(getTileCoordinate(x), getTileCoordinate(y)).getMobSinkingAmount(this);
        final MaskShaderOptions swimMask = this.getSwimMaskShaderOptions(this.inLiquidFloat(x, y));
        final DrawOptions behind = ((TextureDrawOptionsEnd) texture.initDraw().sprite(sprite.x, sprite.y, 64).addMaskShader(swimMask).startGlowOptions(level, (long)this.getID()).light(light).applyEnemyTracker(this, perspective)).pos(drawX, drawY);
        GameTexture overlayTexture = (this.headlightsOn.get()) ? rubiesOverlayBrightHeadlightTexture : rubiesOverlayTexture;
        final DrawOptions overlay = ((TextureDrawOptionsEnd) overlayTexture.initDraw().sprite(sprite.x, sprite.y, 64).addMaskShader(swimMask).pos(drawX, drawY));
        list.add(new MobDrawable() {
            public void draw(TickManager tickManager) {
            }

            public void drawBehindRider(TickManager tickManager) {
                swimMask.use();
                behind.draw();
                overlay.draw();
                swimMask.stop();
            }
        });
        this.addShadowDrawables(tileList, level, x, y, light, camera);
    }

    public static void drawPlacePreview(Level level, int levelX, int levelY, int dir, GameCamera camera) {
        Mob mob = MobRegistry.getMob("poweredminecart", level);
        if (mob != null) {
            mob.setPos((float)levelX, (float)levelY, true);
            int tileX = mob.getTileX();
            int tileY = mob.getTileY();
            GameObject object = level.getObject(tileX, tileY);
            if (object instanceof MinecartTrackObject) {
                MinecartTrackObject trackObject = (MinecartTrackObject)object;
                float moveX = 0.0F;
                float moveY = 0.0F;
                if (dir == 0) {
                    moveY = -1.0F;
                } else if (dir == 1) {
                    moveX = 1.0F;
                } else if (dir == 2) {
                    moveY = 1.0F;
                } else {
                    moveX = -1.0F;
                }

                MinecartLines lines = trackObject.getMinecartLines(level, tileX, tileY, moveX, moveY, false);
                MinecartLinePos pos = lines.getMinecartPos((float)levelX, (float)levelY, dir);
                if (pos != null) {
                    int drawX = camera.getDrawX(pos.x) - 32;
                    int drawY = camera.getDrawY(pos.y) - 47;
                    Point sprite = mob.getAnimSprite((int)pos.x, (int)pos.y, pos.dir);
                    drawY += mob.getBobbing((int)pos.x, (int)pos.y);
                    drawY += level.getTile(getTileCoordinate(pos.x), getTileCoordinate(pos.y)).getMobSinkingAmount(mob);
                    texture.initDraw().sprite(sprite.x, sprite.y, 64).alpha(0.5F).draw(drawX, drawY);
                    return;
                }
            }
        }

        int drawX = camera.getDrawX(levelX) - 32;
        int drawY = camera.getDrawY(levelY) - 47;
        drawY += level.getLevelTile(getTileCoordinate(levelX), getTileCoordinate(levelY)).getLiquidBobbing();
        texture.initDraw().sprite(0, dir, 64).alpha(0.5F).draw(drawX, drawY);
    }
}
