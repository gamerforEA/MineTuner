package com.gamerforea.minetuner.tweaks.memory;

import com.gamerforea.minetuner.MineTunerMod;
import net.minecraft.launchwrapper.LaunchClassLoader;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public final class ResourceCacheGC implements Runnable
{
	private static final Field RESOURCE_CACHE_FIELD;
	private final WeakReference<LaunchClassLoader> classLoader;
	private final TimeUnit periodTimeUnit;
	private final long period;
	private final List<String> garbageResources = new ArrayList<String>(256);

	public static void start(
			@Nullable ClassLoader classLoader, @Nonnull TimeUnit periodTimeUnit, @Nonnegative long period)
	{
		if (RESOURCE_CACHE_FIELD == null)
		{
			MineTunerMod.LOGGER.error("Resource cache not found. GC can't be started");
			return;
		}

		LaunchClassLoader launchClassLoader = null;

		while (classLoader != null)
		{
			if (classLoader instanceof LaunchClassLoader)
			{
				launchClassLoader = (LaunchClassLoader) classLoader;
				break;
			}

			try
			{
				classLoader = classLoader.getParent();
			}
			catch (SecurityException e)
			{
				e.printStackTrace();
			}
		}

		if (launchClassLoader == null)
		{
			MineTunerMod.LOGGER.error("LaunchClassLoader not found. Resource cache GC can't be started");
			return;
		}

		Thread thread = new Thread(new ResourceCacheGC(launchClassLoader, periodTimeUnit, period), "Resource cache GC");
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.setDaemon(true);
		thread.start();
		MineTunerMod.LOGGER.info("Resource cache GC started");
	}

	private ResourceCacheGC(
			@Nonnull LaunchClassLoader classLoader, @Nonnull TimeUnit periodTimeUnit, @Nonnegative long period)
	{
		this.classLoader = new WeakReference<LaunchClassLoader>(classLoader);
		this.periodTimeUnit = periodTimeUnit;
		this.period = period;
	}

	@Override
	public void run()
	{
		while (true)
		{
			try
			{
				this.periodTimeUnit.sleep(this.period);
			}
			catch (InterruptedException ignored)
			{
			}

			LaunchClassLoader classLoader = this.classLoader.get();
			if (classLoader == null)
			{
				MineTunerMod.LOGGER.warn("LaunchClassLoader is absent. Stopping GC...");
				break;
			}

			Map<String, byte[]> resourceCache = getResourceCache(classLoader);
			if (resourceCache == null)
			{
				MineTunerMod.LOGGER.warn("Resource cache is absent. Stopping GC...");
				break;
			}

			if (resourceCache.isEmpty())
			{
				// Prevent java.util.concurrent.ConcurrentHashMap.KeySetView allocation
				if (!this.garbageResources.isEmpty())
					this.garbageResources.clear();
				continue;
			}

			if (this.garbageResources.isEmpty())
				this.garbageResources.addAll(resourceCache.keySet());
			else
			{
				resourceCache.keySet().removeAll(this.garbageResources);
				this.garbageResources.clear();
			}
		}
	}

	private static Map<String, byte[]> getResourceCache(LaunchClassLoader classLoader)
	{
		if (RESOURCE_CACHE_FIELD == null)
			return null;

		try
		{
			return (Map<String, byte[]>) RESOURCE_CACHE_FIELD.get(classLoader);
		}
		catch (IllegalAccessException ignored)
		{
		}

		return null;
	}

	static
	{
		Field resourceCacheField;

		try
		{
			resourceCacheField = LaunchClassLoader.class.getDeclaredField("resourceCache");
			resourceCacheField.setAccessible(true);
			if (!Map.class.isAssignableFrom(resourceCacheField.getType()))
			{
				MineTunerMod.LOGGER.error("Resource cache has illegal type ({} is not assignable from {})", Map.class, resourceCacheField
						.getType());
				resourceCacheField = null;
			}
		}
		catch (NoSuchFieldException e)
		{
			MineTunerMod.LOGGER.error("Resource cache not found", e);
			resourceCacheField = null;
		}
		catch (SecurityException e)
		{
			MineTunerMod.LOGGER.error("Resource cache is not accessible", e);
			resourceCacheField = null;
		}

		RESOURCE_CACHE_FIELD = resourceCacheField;
	}
}
