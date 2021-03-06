package vswe.superfactory.components;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.IItemHandler;
import vswe.superfactory.blocks.ConnectionBlockType;
import vswe.superfactory.tiles.TileEntityManager;

import java.util.*;

public class CraftingBufferElement implements IItemBufferElement, IItemBufferSubElement {
	private static final ItemStack DUMMY_ITEM = ItemStack.EMPTY;
	private static final double SPEED_MULTIPLIER = 0.05F;
	private static final Random rand             = new Random();
	private List<ItemStack>             containerItems;
	private ComponentMenuCrafting       craftingMenu;
	private CommandExecutor             executor;
	private List<IItemHandler> inventories = new ArrayList<IItemHandler>();
	private boolean                     isCrafting;
	private boolean                     justRemoved;
	private int                         overflowBuffer;
	private IRecipe                     recipe;
	private ItemStack                   result;
	private ComponentMenuContainerScrap scrapMenu;

	public CraftingBufferElement(CommandExecutor executor, ComponentMenuCrafting craftingMenu, ComponentMenuContainerScrap scrapMenu) {
		this.executor = executor;
		this.craftingMenu = craftingMenu;
		this.scrapMenu = scrapMenu;
		recipe = craftingMenu.getDummy().getRecipe();
		if (recipe != null) {
			result = recipe.getCraftingResult(craftingMenu.getDummy());
		} else {
			result = ItemStack.EMPTY;
		}
		containerItems = new ArrayList<ItemStack>();
	}

	@Override
	public void prepareSubElements() {
		isCrafting = true;
		justRemoved = false;
	}

	@Override
	public IItemBufferSubElement getSubElement() {
		if (isCrafting && !result.isEmpty()) {
			isCrafting = false;
			return this;
		} else {
			return null;
		}
	}

	@Override
	public void removeSubElement() {
		//nothing to do
	}

	@Override
	public int retrieveItemCount(int moveCount) {
		return moveCount; //no limit
	}

	@Override
	public void decreaseStackSize(int moveCount) {
		//no limit
	}

	@Override
	public void releaseSubElements() {
		if (!result.isEmpty()) {
			if (overflowBuffer > 0) {
				ItemStack overflow = result.copy();
				overflow.setCount(overflowBuffer);
				disposeOfExtraItem(overflow);
				overflowBuffer = 0;
			}
			for (ItemStack containerItem : containerItems) {
				disposeOfExtraItem(containerItem);
			}
			containerItems.clear();
		}
	}

	private void disposeOfExtraItem(ItemStack itemStack) {
		TileEntityManager         manager     = craftingMenu.getParent().getManager();
		List<SlotInventoryHolder> inventories = CommandExecutor.getContainers(manager, scrapMenu, ConnectionBlockType.INVENTORY);
		CommandExecutor.getValidSlots(scrapMenu, inventories);

		for (SlotInventoryHolder inventoryHolder : inventories) {

			for (SideSlotTarget sideSlotTarget : inventoryHolder.getValidSlots().values()) {
				IItemHandler inventory = inventoryHolder.getInventory(sideSlotTarget.getSide());
				for (int slot : sideSlotTarget.getSlots()) {
					if (inventory.insertItem(slot, itemStack, true) != itemStack) {
						ItemStack itemInSlot = inventory.getStackInSlot(slot);
						if (itemInSlot.isEmpty() || (itemInSlot.isItemEqual(itemStack) && ItemStack.areItemStackTagsEqual(itemStack, itemInSlot) && itemStack.isStackable())) {
							int itemCountInSlot = itemInSlot.isEmpty() ? 0 : itemInSlot.getCount();

							int moveCount = Math.min(itemStack.getCount(), Math.min(inventory.getSlotLimit(slot), itemStack.getMaxStackSize()) - itemCountInSlot);

							if (moveCount > 0) {
								if (itemInSlot.isEmpty()) {
									itemInSlot = itemStack.copy();
									itemInSlot.setCount(0);
									inventory.insertItem(slot, itemInSlot, false);
								}

								itemInSlot.grow(moveCount);
								itemStack.shrink(moveCount);
								if (itemStack.getCount() == 0) {
									return;
								}
							}
						}
					}
				}
			}
		}


		double spawnX = manager.getPos().getX() + rand.nextDouble() * 0.8 + 0.1;
		double spawnY = manager.getPos().getY() + rand.nextDouble() * 0.3 + 1.1;
		double spawnZ = manager.getPos().getZ() + rand.nextDouble() * 0.8 + 0.1;

		EntityItem entityitem = new EntityItem(manager.getWorld(), spawnX, spawnY, spawnZ, itemStack);

		entityitem.motionX = rand.nextGaussian() * SPEED_MULTIPLIER;
		entityitem.motionY = rand.nextGaussian() * SPEED_MULTIPLIER + 0.2F;
		entityitem.motionZ = rand.nextGaussian() * SPEED_MULTIPLIER;

		manager.getWorld().spawnEntity(entityitem);
	}

	@Override
	public void remove() {
		//nothing to do
	}

	@Override
	public void onUpdate() {
		for (IItemHandler inventory : inventories) {
			//            inventory.markDirty();
		}
		inventories.clear();
	}

	@Override
	public int getSizeLeft() {
		if (!justRemoved) {
			return overflowBuffer > 0 ? overflowBuffer : findItems(false) ? result.getCount() : 0;
		} else {
			justRemoved = false;
			return 0;
		}
	}

	@Override
	public void reduceAmount(int amount) {
		justRemoved = true;
		if (overflowBuffer > 0) {
			overflowBuffer = overflowBuffer - amount;
		} else {
			findItems(true);
			overflowBuffer = result.getCount() - amount;
		}
		isCrafting = true;
	}

	@Override
	public ItemStack getItemStack() {
		if (useAdvancedDetection()) {
			findItems(false);
		}

		return result;
	}

	private boolean findItems(boolean remove) {
		Map<Integer, ItemStack> foundItems = new HashMap<Integer, ItemStack>();
		for (ItemBufferElement itemBufferElement : executor.itemBuffer) {
			int count = itemBufferElement.retrieveItemCount(9);
			for (Iterator<SlotStackInventoryHolder> iterator = itemBufferElement.getSubElements().iterator(); iterator.hasNext(); ) {
				IItemBufferSubElement itemBufferSubElement = iterator.next();
				ItemStack             itemstack            = itemBufferSubElement.getItemStack();
				int                   subCount             = Math.min(count, itemBufferSubElement.getSizeLeft());
				for (int i = 0; i < 9; i++) {
					CraftingSetting setting = (CraftingSetting) craftingMenu.getSettings().get(i);
					if (foundItems.get(i).isEmpty()) {
						if (!setting.isValid()) {
							foundItems.put(i, DUMMY_ITEM);
						} else if (subCount > 0 && setting.isEqualForCommandExecutor(itemstack)) {
							foundItems.put(i, itemstack.copy());

							if (craftingMenu.getDummy().isItemValidForRecipe(recipe, craftingMenu.getResultItem(), foundItems, useAdvancedDetection())) {
								subCount--;
								count--;
								if (remove) {
									if (itemstack.getItem().hasContainerItem(itemstack)) {
										containerItems.add(itemstack.getItem().getContainerItem(itemstack));
									}
									itemBufferElement.decreaseStackSize(1);
									itemBufferSubElement.reduceAmount(1);
									if (itemBufferSubElement.getSizeLeft() == 0) {
										itemBufferSubElement.remove();
										iterator.remove();
									}
									inventories.add(((SlotStackInventoryHolder) itemBufferSubElement).getInventory());
								}
							} else {
								foundItems.remove(i);
							}
						}
					}
				}
			}
		}

		if (foundItems.size() == 9) {
			result = craftingMenu.getDummy().getResult(foundItems);
			result = !result.isEmpty() ? result.copy() : ItemStack.EMPTY;
			return true;
		} else {
			return false;
		}
	}

	private boolean useAdvancedDetection() {
		return craftingMenu.getResultItem().getFuzzyMode() != FuzzyMode.PRECISE;
	}
}
