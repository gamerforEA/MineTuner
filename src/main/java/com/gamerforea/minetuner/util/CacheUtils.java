package com.gamerforea.minetuner.util;

import java.util.Map;

public final class CacheUtils
{
	public static <CT, T> T cache(Class<? extends CT> cachedType, Map<CT, CT> cache, T obj)
	{
		if (cachedType.isInstance(obj))
		{
			CT object = cachedType.cast(obj);
			CT cachedObject = cache.putIfAbsent(object, object);
			if (cachedObject != null)
				return unsafeCast(cachedObject);
		}

		return obj;
	}

	@SuppressWarnings("unchecked")
	private static <T> T unsafeCast(Object obj)
	{
		return (T) obj;
	}
}
