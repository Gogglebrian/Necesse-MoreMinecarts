package poweredminecart;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.localization.message.GameMessage;
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
import necesse.entity.mobs.summon.summonFollowingMob.mountFollowingMob.MinecartMountMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawOptions.DrawOptions;
import necesse.gfx.drawOptions.texture.TextureDrawOptionsEnd;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.gfx.gameTexture.GameTexture;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.MinecartTrackObject;
import necesse.level.maps.Level;
import necesse.level.maps.light.GameLight;

import java.awt.*;
import java.util.List;

public class PoweredMinecartMountMob extends MinecartMountMob implements MountAbility {
    public final BooleanNetworkField headlightsOn;
    private boolean loadedFromSave = false;

    public PoweredMinecartMountMob() {
        super();
        this.setSpeed(PoweredMinecartMob.speed);
        this.accelerationMod = PoweredMinecartMob.accelMod;

        this.headlightsOn = this.registerNetworkField(new BooleanNetworkField(false));
    }

    @Override
    protected GameMessage getSummonLocalization() {
        return MobRegistry.getLocalization("poweredminecart");
    }

    public void init() {
        super.init();

        if (this.isServer() && !this.loadedFromSave) {
            PoweredMinecartMob.setInitialHeadlightState(
                    this.getLevel(),
                    this.getX(),
                    this.getY(),
                    this.minecartDir,
                    this.headlightsOn
            );
        }
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
        if (headlightsOn.get()) PoweredMinecartMob.refreshHeadlights(this.getLevel(), (Mob)this, this.minecartDir);
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
        final DrawOptions behind = ((TextureDrawOptionsEnd) PoweredMinecartMob.texture.initDraw().sprite(sprite.x, sprite.y, 64).addMaskShader(swimMask).startGlowOptions(level, (long)this.getID()).light(light).applyEnemyTracker(this, perspective)).pos(drawX, drawY);
        GameTexture overlayTexture = (this.headlightsOn.get()) ? PoweredMinecartMob.rubiesOverlayBrightHeadlightTexture : PoweredMinecartMob.rubiesOverlayTexture;
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
                    PoweredMinecartMob.texture.initDraw().sprite(sprite.x, sprite.y, 64).alpha(0.5F).draw(drawX, drawY);
                    return;
                }
            }
        }

        int drawX = camera.getDrawX(levelX) - 32;
        int drawY = camera.getDrawY(levelY) - 47;
        drawY += level.getLevelTile(getTileCoordinate(levelX), getTileCoordinate(levelY)).getLiquidBobbing();
        PoweredMinecartMob.texture.initDraw().sprite(0, dir, 64).alpha(0.5F).draw(drawX, drawY);
    }
}
