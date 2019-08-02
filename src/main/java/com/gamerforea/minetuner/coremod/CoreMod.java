package com.gamerforea.minetuner.coremod;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

import static com.gamerforea.minetuner.ModConstants.COREMOD_NAME;
import static com.gamerforea.minetuner.ModConstants.MC_VERSION;

@IFMLLoadingPlugin.MCVersion(MC_VERSION)
@IFMLLoadingPlugin.Name(COREMOD_NAME)
@IFMLLoadingPlugin.SortingIndex(Short.MAX_VALUE)
public final class CoreMod implements IFMLLoadingPlugin // -Dfml.coreMods.load=com.gamerforea.minetuner.coremod.CoreMod
{
	public static final Logger LOGGER = LogManager.getLogger(COREMOD_NAME);
	private static boolean isObfuscated = false;

	@Override
	public String[] getASMTransformerClass()
	{
		return new String[] { "com.gamerforea.minetuner.coremod.ASMTransformer" };
	}

	@Override
	public String getModContainerClass()
	{
		return null;
	}

	@Override
	public String getSetupClass()
	{
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data)
	{
		isObfuscated = (Boolean) data.get("runtimeDeobfuscationEnabled");
	}

	@Override
	public String getAccessTransformerClass()
	{
		return null;
	}

	public static boolean isObfuscated()
	{
		return isObfuscated;
	}
}
