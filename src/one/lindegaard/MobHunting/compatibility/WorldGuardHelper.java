package one.lindegaard.MobHunting.compatibility;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardHelper implements Listener {

	private static final StateFlag MOBHUNTINGFLAG = new StateFlag("MobHunting", true);

	// *******************************************************************
	// getters
	// *******************************************************************

	public static StateFlag getMobHuntingFlag() {
		return MOBHUNTINGFLAG;
	}

	// *******************************************************************
	// SET / REMOVE FLAG
	// *******************************************************************

	private static State parseInput(String flagValue) {
		if (flagValue.equalsIgnoreCase("allow"))
			return State.ALLOW;
		else if (flagValue.equalsIgnoreCase("deny"))
			return State.DENY;
		else
			return null;
	}

	public static boolean setCurrentRegionFlag(CommandSender sender, World world, ProtectedRegion region,
			StateFlag stateFlag, String flagstate) {
		if (region.getFlag(getMobHuntingFlag()) != null) {
			Map<Flag<?>, Object> flags = region.getFlags();
			flags.remove(MOBHUNTINGFLAG);
			region.setFlags(flags);
		}
		region.setFlag(getMobHuntingFlag(), parseInput(flagstate));
		if (sender != null)
			sender.sendMessage(
					ChatColor.YELLOW + "Region flag MobHunting set on '" + region.getId() + "' to '" + flagstate + "'");
		String flagstring = "";
		Iterator<Entry<Flag<?>, Object>> i = region.getFlags().entrySet().iterator();
		while (i.hasNext()) {
			Entry<Flag<?>, Object> s = i.next();
			flagstring = flagstring + s.getKey().getName() + ": " + s.getValue();
			if (i.hasNext())
				flagstring = flagstring + ",";
		}
		if (sender != null)
			sender.sendMessage(ChatColor.GRAY + "(Current flags: " + flagstring + ")");

		return true;
	}

	public static boolean removeCurrentRegionFlag(CommandSender sender, World world, ProtectedRegion region,
			StateFlag stateFlag) {
		region.setFlag(stateFlag, null);
		if (sender != null)
			sender.sendMessage(ChatColor.YELLOW + "Region flag '" + stateFlag.getName() + "' removed from region '"
					+ region.getId() + "'");
		return true;
	}

	public static boolean isAllowedByWorldGuard(Entity damager, Entity damaged, StateFlag stateFlag,
			boolean defaultValue) {
		Player checkedPlayer = null;
		if (MyPetCompat.isMyPet(damager))
			checkedPlayer = MyPetCompat.getMyPetOwner(damager);
		else if (damager instanceof Player)
			checkedPlayer = (Player) damager;
		if (checkedPlayer != null) {
			RegionManager regionManager = WorldGuardCompat.getWorldGuardPlugin()
					.getRegionManager(checkedPlayer.getWorld());
			if (regionManager != null) {
				ApplicableRegionSet set = regionManager.getApplicableRegions(checkedPlayer.getLocation());
				if (set.size() > 0) {
					LocalPlayer localPlayer = WorldGuardCompat.getWorldGuardPlugin().wrapPlayer(checkedPlayer);
					State flag = set.queryState(localPlayer, stateFlag);
					if (flag == null)
						return defaultValue;
					else if (flag.equals(State.ALLOW))
						return true;
					else
						return false;
				} else
					return defaultValue;
			}
		}
		return defaultValue;
	}

	public static void registerFlag() {
		Plugin wg = Bukkit.getPluginManager().getPlugin("WorldGuard");
		try {
			// register MobHuting flag with the WorlsGuard Flag registry
			((WorldGuardPlugin) wg).getFlagRegistry().register(WorldGuardHelper.getMobHuntingFlag());
		 } catch (FlagConflictException e) {

			// some other plugin registered a flag by the same name already.
			// you may want to re-register with a different name, but this
			// could cause issues with saved flags in region files. it's
			// better
			// to print a message to let the server admin know of the
			// conflict
		}
	}

}
