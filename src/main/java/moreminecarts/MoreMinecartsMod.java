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
        ItemRegistry.registerItem("tungstenminecart", new TungstenMinecartMountItem(), 400, true);
        MobRegistry.registerMob("tungstenminecartmount", TungstenMinecartMountMob.class, false, false);
        MobRegistry.registerMob("tungstenminecart", TungstenMinecartMob.class, false);
    }

    public void initResources() {
        TungstenMinecartMob.texture = GameTexture.fromFile("mobs/tungstenminecart");
    }

    public void postInit() {
        Recipes.registerModRecipe(new Recipe(
                "tungstenminecart",
                1,
                RecipeTechRegistry.TUNGSTEN_WORKSTATION,
                new Ingredient[] {
                        new Ingredient("minecart", 1),
                        new Ingredient("tungstenbar",10),
                        new Ingredient("ruby", 4),
                }).showBefore("minecart"));
    }
}