package mortar.api.atests;

import org.bukkit.SoundCategory;

import mortar.api.fulcrum.object.FCUSound;

public class SoundSteelDig extends FCUSound
{
	public SoundSteelDig()
	{
		super("steel.dig");
		setCategory(SoundCategory.BLOCKS);
		setDefaultPitch(1.15f);
		setDefaultPitchRandomness(0.175f);
		setDefaultVolume(0.4f);
		setStream(false);
		setSubtitle("Digging steel");
		addSounds("steel$", getClass(), "/assets/sounds/material/metal/metalbar_break$.ogg", 1, 11);
	}
}
