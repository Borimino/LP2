package com.sots.util.registries;

import com.sots.tiles.TileBasicPipe;
import com.sots.util.References;

import net.minecraftforge.fml.common.registry.GameRegistry;

public class TileRegistry {
	public static void init() {
		GameRegistry.registerTileEntity(TileBasicPipe.class, References.MODID +"_tilebaasicpipe");
	}
}
