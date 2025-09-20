package net.rasanovum.viaromana.init;

import net.rasanovum.viaromana.triggers.*;

@SuppressWarnings("InstantiationOfUtilityClass")
public class TriggerInit {
	public static void load() {
		new OnBlockBreak();
	}
}
