package moreminecarts;

import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.ItemRegistry;
import necesse.engine.registries.MobRegistry;
import necesse.engine.registries.RecipeTechRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.inventory.recipe.Recipes;
import necesse.inventory.recipe.Recipe;
import necesse.inventory.recipe.Ingredient;

@ModEntry
public class MoreMinecartsMod {

    public void init() {
        ItemRegistry.registerItem("poweredminecart", new PoweredMinecartMountItem(), 400, true);
        MobRegistry.registerMob("poweredminecartmount", PoweredMinecartMountMob.class, false, false);
        MobRegistry.registerMob("poweredminecart", PoweredMinecartMob.class, false);
    }

    public void initResources() {
        PoweredMinecartMob.texture = GameTexture.fromFile("mobs/poweredminecart");
        PoweredMinecartMob.rubiesOverlayTexture = GameTexture.fromFile("mobs/poweredminecartrubiesoverlay");
    }

    public void postInit() {
        Recipes.registerModRecipe(new Recipe(
                "poweredminecart",
                1,
                RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                new Ingredient[] {
                        new Ingredient("minecart", 1),
                        new Ingredient("tungstenbar",10),
                        new Ingredient("ruby", 4),
                }).showBefore("minecart"));
    }
}