package com.gamerforea.minetuner.tweaks.memory;

import com.gamerforea.minetuner.MineTunerMod;
import com.gamerforea.minetuner.util.CacheUtils;
import com.google.common.base.Stopwatch;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.Locale;
import net.minecraft.util.StatCollector;
import net.minecraft.util.StringTranslate;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class LanguageDeduplicator
{
	public static void deduplicateLanguages()
	{
		Stopwatch stopwatch = Stopwatch.createStarted();
		Map<String, String> cache = new HashMap<>(4096);

		StringTranslate localizedName = ReflectionHelper.getPrivateValue(StatCollector.class, null, "field_74839_a", "localizedName");
		StringTranslate fallbackTranslator = ReflectionHelper.getPrivateValue(StatCollector.class, null, "field_150828_b", "fallbackTranslator");
		deduplicateStrings(cache, localizedName);
		deduplicateStrings(cache, fallbackTranslator);

		Map<String, Properties> modLanguageData = ReflectionHelper.getPrivateValue(LanguageRegistry.class, LanguageRegistry
				.instance(), "modLanguageData");
		for (Properties languageData : modLanguageData.values())
		{
			deduplicateStrings(cache, languageData);
		}

		if (FMLCommonHandler.instance().getSide().isClient())
			deduplicateClientLanguages(cache);

		MineTunerMod.LOGGER.info("Language deduplication finished in {} ms", stopwatch.stop()
																					  .elapsed(TimeUnit.MILLISECONDS));
	}

	@SideOnly(Side.CLIENT)
	private static void deduplicateClientLanguages(Map<String, String> cache)
	{
		Locale currentLocale = ReflectionHelper.getPrivateValue(LanguageManager.class, null, "field_135049_a", "currentLocale");
		Map<String, String> field_135032_a = ReflectionHelper.getPrivateValue(Locale.class, currentLocale, "field_135032_a");
		deduplicateStrings(cache, field_135032_a);
	}

	private static void deduplicateStrings(Map<String, String> cache, StringTranslate stringTranslate)
	{
		Map<String, String> languageList = ReflectionHelper.getPrivateValue(StringTranslate.class, stringTranslate, "field_74816_c", "languageList");
		deduplicateStrings(cache, languageList);
	}

	private static <K, V> void deduplicateStrings(Map<String, String> cache, Map<K, V> map)
	{
		List<ImmutablePair<K, V>> newEntries = new ArrayList<>();

		for (Map.Entry<K, V> entry : map.entrySet())
		{
			K key = entry.getKey();
			V value = entry.getValue();
			K cachedKey = CacheUtils.cache(String.class, cache, key);
			V cachedValue = CacheUtils.cache(String.class, cache, value);

			if (key != cachedKey)
				newEntries.add(new ImmutablePair<>(cachedKey, cachedValue));
			else if (value != cachedValue)
				entry.setValue(cachedValue);
		}

		for (ImmutablePair<K, V> newEntry : newEntries)
		{
			map.remove(newEntry.left);
			map.put(newEntry.left, newEntry.right);
		}
	}
}
