package poweredminecart;

import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.registries.MobRegistry;
import necesse.engine.sound.SoundEffect;
import necesse.engine.sound.SoundManager;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.entity.mobs.summon.MinecartMob;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.item.mountItem.MinecartMountItem;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.MinecartTrackObject;
import necesse.level.gameObject.TrapTrackObject;
import necesse.level.maps.Level;

public class PoweredMinecartMountItem extends MinecartMountItem {
    public PoweredMinecartMountItem() {
        super();
        this.mobStringID = "poweredminecartmount";
        this.rarity = Rarity.RARE;
    }

    public ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
        ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
        tooltips.add(Localization.translate("itemtooltip", "poweredminecartheadlightstip"));
        return tooltips;
    }

    @Override
    public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight, InventoryItem item, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {
        if (!attackerMob.isPlayer) {
            return item;
        } else {
            PlayerMob player = (PlayerMob) attackerMob;
            if (this.canPlace(level, x, y, player, item, mapContent) == null) {
                if (level.isServer()) {
                    Mob mob = MobRegistry.getMob("poweredminecart", level);
                    if (mob instanceof MinecartMob) {
                        int dir = ((MinecartMob) mob).minecartDir = attackerMob.isAttacking ? attackerMob.beforeAttackDir : attackerMob.getDir();
                        mob.resetUniqueID();
                        level.entityManager.addMob(mob, (float) x, (float) y);
                        /*
                        if (mob instanceof PoweredMinecartMob) {
                            PoweredMinecartMob.setInitialHeadlightState(level, x, y, dir, ((PoweredMinecartMob)mob).headlightsOn);
                        }
                        */

                    }
                }

                if (level.isClient()) {
                    SoundManager.playSound(GameResources.blunthit, SoundEffect.effect((float) x, (float) y).volume(0.3F));
                    SoundManager.playSound(GameResources.cling, SoundEffect.effect((float) x, (float) y).volume(0.3F));
                }

                item.setAmount(item.getAmount() - 1);
                return item;
            } else {
                return item;
            }
        }
    }

    @Override
    protected String canPlace(Level level, int x, int y, PlayerMob player, InventoryItem item, GNDItemMap mapContent) {
        if (player.getPositionPoint().distance((double)x, (double)y) > (double)100.0F) {
            return "outofrange";
        } else {
            Mob mob = MobRegistry.getMob("poweredminecart", level);
            if (mob != null) {
                mob.setPos((float)x, (float)y, true);
                if (mob.collidesWith(level)) {
                    return "collision";
                }

                GameObject object = level.getObject(mob.getTileX(), mob.getTileY());
                if (!(object instanceof MinecartTrackObject) || object instanceof TrapTrackObject) {
                    return "nottracks";
                }
            }

            return null;
        }
    }

    @Override
    public void drawPlacePreview(Level level, int x, int y, GameCamera camera, PlayerMob player, InventoryItem item, PlayerInventorySlot slot) {
        String error = this.canPlace(level, x, y, player, item, (GNDItemMap)null);
        if (error == null) {
            int placeDir = player.isAttacking ? player.beforeAttackDir : player.getDir();
            PoweredMinecartMob.drawPlacePreview(level, x, y, placeDir, camera);
        }
    }
}

