package mortar.bukkit.inventory;

import org.bukkit.Material;

import mortar.bukkit.world.MaterialBlock;

public class UIRainbowDecorator implements WindowDecorator
{
	@Override
	public Element onDecorateBackground(Window window, int position, int row)
	{
		int apos = window.getRealPosition(position, row);

		return new UIElement("bh")
				.setBackground(true)
				.setName(" ")
				.setMaterial(
						new MaterialBlock(
								Material.STAINED_GLASS_PANE,
								(byte) (apos % 15)));
	}
}
