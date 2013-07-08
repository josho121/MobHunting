package au.com.mineauz.MobHunting.achievements;

import org.bukkit.entity.Creeper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import au.com.mineauz.MobHunting.MobHuntKillEvent;
import au.com.mineauz.MobHunting.MobHunting;

public class CreeperBoxing implements Achievement, Listener
{

	@Override
	public String getName()
	{
		return "Creeper Boxing";
	}

	@Override
	public String getID()
	{
		return "creeperboxing";
	}

	@Override
	public String getDescription()
	{
		return "Box with a creeper and win!";
	}

	@Override
	public double getPrize()
	{
		return MobHunting.config().specialCreeperPunch;
	}

	@EventHandler
	private void onKill(MobHuntKillEvent event)
	{
		if(event.getEntity() instanceof Creeper && !event.getDamageInfo().usedWeapon)
			MobHunting.instance.getAchievements().awardAchievement(this, event.getPlayer());
	}
}
