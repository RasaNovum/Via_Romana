package net.rasanovum.viaromana.fabric.init;

import net.rasanovum.viaromana.fabric.triggers.*;

@SuppressWarnings("InstantiationOfUtilityClass")
public class ViaRomanaModTriggers {
	public static void load() {
		new OnBlockBreak();
		new OnBlockClick();
		new OnBlockPlace();
		new OnDimensionChange();
		new OnJoinWorld();
		new OnPlayerTick();
		new OnServerTick();
		new OnWorldLoad();
	}
}
