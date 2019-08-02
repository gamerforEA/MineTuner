package com.gamerforea.minetuner.coremod;

import com.gamerforea.minetuner.MineTunerMod;
import com.gamerforea.minetuner.client.model.DisplayListedWavefrontObject;
import com.gamerforea.minetuner.client.model.DisplayListedWavefrontObjectWrapper;
import com.gamerforea.minetuner.tweaks.memory.ModelDeduplicator;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.IModelCustomLoader;
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.WavefrontObject;

import java.io.InputStream;

public final class ASMHooks
{
	@SuppressWarnings("unused")
	public static WavefrontObject createWavefrontObject(ResourceLocation resource) throws ModelFormatException
	{
		MineTunerMod mod = MineTunerMod.instance;
		return mod.isModelDisplayListsGen() && !mod.isModelDisplayListsGenWrapper() ? new DisplayListedWavefrontObject(resource) : new WavefrontObject(resource);
	}

	@SuppressWarnings("unused")
	public static WavefrontObject createWavefrontObject(String filename, InputStream inputStream)
			throws ModelFormatException
	{
		MineTunerMod mod = MineTunerMod.instance;
		return mod.isModelDisplayListsGen() && !mod.isModelDisplayListsGenWrapper() ? new DisplayListedWavefrontObject(filename, inputStream) : new WavefrontObject(filename, inputStream);
	}

	@SuppressWarnings("unused")
	public static IModelCustom loadModel(IModelCustomLoader loader, ResourceLocation resource)
			throws ModelFormatException
	{
		MineTunerMod mod = MineTunerMod.instance;
		boolean deduplication = mod.isModelDeduplication();
		if (deduplication)
		{
			IModelCustom cachedModel = ModelDeduplicator.getCachedModel(resource);
			if (cachedModel != null)
				return cachedModel;
		}

		IModelCustom model = loader.loadInstance(resource);
		if (model == null)
			return null;

		if (!deduplication)
		{
			if (mod.isModelDisplayListsGen() && mod.isModelDisplayListsGenWrapper() && model.getClass() == WavefrontObject.class)
			{
				WavefrontObject wavefrontObject = (WavefrontObject) model;
				model = new DisplayListedWavefrontObjectWrapper(wavefrontObject);
			}

			return model;
		}

		if (model instanceof WavefrontObject)
		{
			WavefrontObject wavefrontObject = (WavefrontObject) model;
			ModelDeduplicator.addWavefrontObject(wavefrontObject);

			if (mod.isModelDisplayListsGen() && mod.isModelDisplayListsGenWrapper() && model.getClass() == WavefrontObject.class)
				model = new DisplayListedWavefrontObjectWrapper(wavefrontObject);
		}

		ModelDeduplicator.cacheModel(resource, model);
		return model;
	}
}
