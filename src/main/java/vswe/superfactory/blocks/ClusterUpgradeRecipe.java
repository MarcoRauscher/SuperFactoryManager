package vswe.superfactory.blocks;

import net.minecraft.block.Block;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.RecipeSorter;
import vswe.superfactory.SuperFactoryManager;
import vswe.superfactory.registry.ModBlocks;

public class ClusterUpgradeRecipe extends ShapelessRecipes {
	private static final NonNullList RECIPE;
	private static final ItemStack   RESULT;

	static {
		RESULT = new ItemStack(ModBlocks.CABLE_CLUSTER, 1, 8);
		RECIPE = NonNullList.create();
		RECIPE.add(new ItemStack(ModBlocks.CABLE_CLUSTER, 1, 0));
		for (int i = 0; i < 8; i++) {
			RECIPE.add(new ItemStack(ModBlocks.CABLE));
		}
	}

	public ClusterUpgradeRecipe() {
		super(SuperFactoryManager.MODID + "cluster", RESULT, RECIPE);
		RecipeSorter.register("sfm:clusterupgrade", ClusterUpgradeRecipe.class, RecipeSorter.Category.SHAPELESS, "after:minecraft:shapeless");
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting inv) {
		for (int i = 0; i < inv.getSizeInventory(); i++) {
			ItemStack itemStack = inv.getStackInSlot(i);

			if (!itemStack.isEmpty() && Block.getBlockFromItem(itemStack.getItem()) == ModBlocks.CABLE_CLUSTER) {
				ItemStack copy = itemStack.copy();
				copy.setItemDamage(8);
				return copy;
			}
		}
		return super.getCraftingResult(inv);
	}
}
