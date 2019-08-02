package com.gamerforea.minetuner;

import com.gamerforea.minetuner.tweaks.memory.LanguageDeduplicator;
import com.gamerforea.minetuner.tweaks.memory.ModelDeduplicator;
import com.gamerforea.minetuner.tweaks.memory.ResourceCacheGC;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.concurrent.TimeUnit;

import static com.gamerforea.minetuner.ModConstants.*;

@Mod(modid = MODID, name = NAME, version = VERSION, acceptableRemoteVersions = "*")
public final class MineTunerMod
{
	public static final Logger LOGGER = LogManager.getLogger(NAME);

	@Mod.Instance
	public static MineTunerMod instance;

	private boolean languageDeduplication;
	private boolean languageDeduplicated;
	private boolean modelDeduplication;

	private boolean modelDisplayListsGen;
	private boolean modelDisplayListsGenWrapper;

	@Mod.EventHandler
	public void constuct(FMLConstructionEvent event)
	{
		Configuration cfg = new Configuration(new File(Loader.instance().getConfigDir(), NAME + ".cfg"));
		int resourceCacheGcPeriod = cfg.getInt("Period", "Resource Cache GC", 30, 0, 3600, "GC period in seconds. Old cache elements will be removed every second GC iteration");
		this.languageDeduplication = cfg.getBoolean("Language Deduplication", "Deduplication", true, "Localization string deduplication");
		this.modelDeduplication = cfg.getBoolean("Model Deduplication", "Deduplication", false, "Model deduplication");
		this.modelDisplayListsGen = cfg.getBoolean("Model Display List Generation", "Graphics", false, "Generate Display Lists to improve model rendering performance");
		this.modelDisplayListsGenWrapper = cfg.getBoolean("Model Display List Generation Wrapper", "Graphics", false, "Wrap models instead using inheritance for Display Lists generation (may produce ClassCastException, use at your own risk)");
		if (cfg.hasChanged())
			cfg.save();

		if (resourceCacheGcPeriod > 0)
			ResourceCacheGC.start(this.getClass().getClassLoader(), TimeUnit.SECONDS, resourceCacheGcPeriod);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		if (this.modelDeduplication)
			ModelDeduplicator.deduplicateModels();

		ModelDeduplicator.clearModels();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Mod.EventHandler
	public void serverStart(FMLServerAboutToStartEvent event)
	{
		if (this.languageDeduplication && !this.languageDeduplicated)
		{
			this.languageDeduplicated = true;
			LanguageDeduplicator.deduplicateLanguages();
		}
	}

	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event)
	{
		// TODO Hook net.minecraft.client.resources.LanguageManager.onResourceManagerReload
		if (this.languageDeduplication && !this.languageDeduplicated)
		{
			this.languageDeduplicated = true;
			LanguageDeduplicator.deduplicateLanguages();
		}
	}

	public boolean isModelDeduplication()
	{
		return this.modelDeduplication;
	}

	public boolean isModelDisplayListsGen()
	{
		return this.modelDisplayListsGen;
	}

	public boolean isModelDisplayListsGenWrapper()
	{
		return this.modelDisplayListsGenWrapper;
	}
}
