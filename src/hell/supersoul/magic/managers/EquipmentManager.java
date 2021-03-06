package hell.supersoul.magic.managers;

import hell.supersoul.magic.core.ComboM;
import hell.supersoul.magic.core.Magic;
import hell.supersoul.magic.core.MagicItem;
import hell.supersoul.magic.core.MagicItem.MagicItemType;
import hell.supersoul.magic.core.MagicItem.ShortcutType;
import hell.supersoul.magic.rpg.PlayerM;
import net.minecraft.server.v1_12_R1.EnumItemSlot;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import hell.supersoul.magic.core.RegularM;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EquipmentManager {

	public enum EquipmentSlot {
		HELMET, CHESTPLATE, LEGGINGS, BOOTS, HAND;
	}

	static HashMap<Player, HashMap<EquipmentSlot, MagicItem>> equipmentData = new HashMap<>();
	static HashMap<Player, HashMap<EquipmentSlot, ItemStack>> currentItem = new HashMap<>();

	// Converts an inventory slot number to an EquipmentSlot enum.
	public static EquipmentSlot toEquipmentSlot(int slot) {
		if (slot == 39)
			return EquipmentSlot.HELMET;
		else if (slot == 38)
			return EquipmentSlot.CHESTPLATE;
		else if (slot == 37)
			return EquipmentSlot.LEGGINGS;
		else if (slot == 36)
			return EquipmentSlot.BOOTS;
		else if (slot >= 0 && slot <= 8)
			return EquipmentSlot.HAND;
		return null;
	}

	public static void checkAndUpdate(Player player, int slot) {
		checkAndUpdate(player, slot, null);
	}

	// Checks and updates the equimentData of a player for an inventory slot.
	public static void checkAndUpdate(Player player, int slot, ItemStack item) {

		// Safety checks
		if (player == null)
			return;
		if (!player.isOnline())
			return;
		if (!equipmentData.containsKey(player)) {
			equipmentData.put(player, new HashMap<>());
		}
		if (!currentItem.containsKey(player)) {
			currentItem.put(player, new HashMap<>());
		}
		PlayerM playerM = PlayerM.getPlayerM(player);
		if (playerM == null)
			return;

		// Gets a EquipmentSlot instance for the slot number.
		EquipmentSlot eSlot = EquipmentManager.toEquipmentSlot(slot);
		if (eSlot == null)
			return;

		// Checks if the item is the same as the last check, if same then no need to
		// update.
		if (item == null)
			item = player.getInventory().getItem(slot);
		ItemStack current = currentItem.get(player).get(eSlot);
		if (item == current) {
			player.sendMessage("same");
			return;
		} else {
			currentItem.get(player).put(eSlot, item);
			player.sendMessage("replace");
		}

		// Checks if the new item is air, then remove the MagicItem from the player's
		// data.
		if (item == null || item.getType().equals(Material.AIR)) {
			equipmentData.get(player).remove(eSlot);
			playerM.updateEquipmentStats();
			return;
		}

		// Checks if the new item is a MagicItem, if no then remove the previous
		// MagicItem from the player's data.
		MagicItem magicItem = LoreManager.getMagicItem(item);
		if (magicItem == null) {
			equipmentData.get(player).remove(eSlot);
			playerM.updateEquipmentStats();
			return;
		}

		// Updates the equipment data of the player.
		equipmentData.get(player).put(eSlot, magicItem);

		// Notify playerM to update the stats.
		playerM.updateEquipmentStats();

	}

	public static boolean hasMagic(Player player, Magic magic, EquipmentSlot slot) {
		if (player == null || magic == null)
			return false;
		if (slot == null) {
			for (MagicItem magicItem : equipmentData.get(player).values()) {
				for (Magic m : magicItem.getMagics()) {
					if (magic.getClass().equals(m.getClass()))
						return true;
				}
			}
			return false;
		} else {
			for (Magic m : equipmentData.get(player).get(slot).getMagics()) {
				if (magic.getClass().equals(m.getClass()))
					return true;
			}
			return false;
		}
	}

	// Gets the Magic Item instance for a player for a equipment slot.
	public static MagicItem getMagicItem(Player player, EquipmentSlot slot) {
		if (player == null)
			return null;
		if (slot == null)
			return null;
		if (!equipmentData.containsKey(player))
			return null;
		return equipmentData.get(player).get(slot);
	}

	// Under normal circumstances, there is only one combo magic for one magic item,
	// return null if more than one.
	public static ComboM getTheOnlyComboMagic(MagicItem item) {
		if (item == null)
			return null;
		int n = 0;
		Magic result = null;
		for (Magic magic : item.getMagics()) {
			if (magic instanceof ComboM) {
				n++;
				result = magic;
			}
		}
		if (n > 1)
			return null;
		return (ComboM) result;
	}

	// Triggers the shortcut of the magic item.
	public static void triggerShortcut(Player player, ShortcutType type) {

		// Safety checks
		if (player == null)
			return;
		if (type == null)
			return;
		if (!equipmentData.containsKey(player))
			return;
		MagicItem item = equipmentData.get(player).get(EquipmentSlot.HAND);
		if (item == null)
			return;
		if (item.getShortcuts().size() < 0)
			return;
		if (!item.getShortcuts().containsKey(type))
			return;

		if (!canUnleashMagic(player))
			return;

		// The magic type must be regular
		Magic magic = item.getMagics().get(item.getShortcuts().get(type));
		if (!(magic instanceof RegularM))
			return;

		// You cannot left click a weapon as shortcut
		if (item.getItemType().equals(MagicItemType.TOOL) && type.equals(ShortcutType.LEFT_CLICK))
			return;

		// You cannot right click book as shortcut
		if (item.getItemType().equals(MagicItemType.BOOK) && type.equals(ShortcutType.RIGHT_CLICK))
			return;

		((RegularM) magic).cast(player);

	}

	// Checks if the player can unleash a magic
	public static boolean canUnleashMagic(Player player) {
		if (ComboManager.getCurrentHitTask().containsKey(player))
			return false;
		if (!player.isOnGround())
			return false;
		return true;
	}

	// Get the equipment strength
	public static int getEquipmentStrength(Player player) {

		if (player == null)
			return 0;
		if (!equipmentData.containsKey(player))
			equipmentData.put(player, new HashMap<>());

		int strength = 0;
		for (MagicItem mi : equipmentData.get(player).values())
			strength += mi.getStrength();
		if (!equipmentData.get(player).containsKey(EquipmentSlot.HAND)
				&& currentItem.get(player).containsKey(EquipmentSlot.HAND)) {
			ItemStack is = currentItem.get(player).get(EquipmentSlot.HAND);
			net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(is);
			if (nmsStack.a(EnumItemSlot.MAINHAND).containsKey("generic.attackDamage")) {
				int attack = (int) nmsStack.a(EnumItemSlot.MAINHAND).get("generic.attackDamage").iterator().next().d()
						+ 1;
				strength += attack;
			}
		}
		return strength;

	}

	// Get the equipment strength
	public static int getEquipmentDefense(Player player) {

		if (player == null)
			return 0;
		if (!equipmentData.containsKey(player))
			equipmentData.put(player, new HashMap<>());

		int defense = 0;
		for (MagicItem mi : equipmentData.get(player).values())
			defense += mi.getDefense();
		defense += player.getAttribute(Attribute.GENERIC_ARMOR).getValue();
		defense += player.getAttribute(Attribute.GENERIC_ARMOR_TOUGHNESS).getValue();

		return defense;

	}
}
