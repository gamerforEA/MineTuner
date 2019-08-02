package com.gamerforea.minetuner.tweaks.memory;

import com.gamerforea.minetuner.MineTunerMod;
import com.gamerforea.minetuner.util.CacheUtils;
import com.google.common.base.Stopwatch;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.strategy.HashingStrategy;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.obj.Face;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.Vertex;
import net.minecraftforge.client.model.obj.WavefrontObject;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class ModelDeduplicator
{
	private static final Map<ResourceLocation, IModelCustom> MODEL_CACHE = new HashMap<>();
	private static final ArrayList<WavefrontObject> WAVEFRONT_OBJECTS = new ArrayList<>();

	public static IModelCustom getCachedModel(ResourceLocation resource)
	{
		return MODEL_CACHE.get(resource);
	}

	public static void cacheModel(ResourceLocation resource, IModelCustom model)
	{
		MODEL_CACHE.put(resource, model);
	}

	public static void addWavefrontObject(WavefrontObject model)
	{
		WAVEFRONT_OBJECTS.add(model);
	}

	public static void clearModels()
	{
		MODEL_CACHE.clear();
		WAVEFRONT_OBJECTS.clear();
		WAVEFRONT_OBJECTS.trimToSize();
	}

	public static void deduplicateModels()
	{
		if (FMLCommonHandler.instance().getSide().isClient())
		{
			Stopwatch stopwatch = Stopwatch.createStarted();
			deduplicateModelsClient();
			MineTunerMod.LOGGER.info("Model vertices deduplication finished in {} ms", stopwatch.stop()
																								.elapsed(TimeUnit.MILLISECONDS));
		}
	}

	@SideOnly(Side.CLIENT)
	private static void deduplicateModelsClient()
	{
		if (WAVEFRONT_OBJECTS.isEmpty())
			return;

		TCustomHashMap<Vertex, Vertex> vertexCache = new TCustomHashMap<>(new HashingStrategy<Vertex>()
		{
			@Override
			public int computeHashCode(Vertex object)
			{
				return new HashCodeBuilder().append(object.x).append(object.y).append(object.z).toHashCode();
			}

			@Override
			public boolean equals(Vertex o1, Vertex o2)
			{
				if (o1 == o2)
					return true;
				if (o1 == null || o2 == null)
					return false;
				return Float.floatToIntBits(o1.x) == Float.floatToIntBits(o2.x) && Float.floatToIntBits(o1.y) == Float.floatToIntBits(o2.y) && Float
						.floatToIntBits(o1.z) == Float.floatToIntBits(o2.z);
			}
		}, 2048);

		for (WavefrontObject model : WAVEFRONT_OBJECTS)
		{
			model.vertices.trimToSize();
			deduplicateVertices(vertexCache, model.vertices);

			model.vertexNormals.trimToSize();
			deduplicateVertices(vertexCache, model.vertexNormals);

			model.textureCoordinates.trimToSize();
			model.groupObjects.trimToSize();

			for (GroupObject groupObject : model.groupObjects)
			{
				groupObject.faces.trimToSize();
				for (Face face : groupObject.faces)
				{
					deduplicateVertices(vertexCache, Arrays.asList(face.vertices));
					if (face.vertexNormals != null)
						deduplicateVertices(vertexCache, Arrays.asList(face.vertexNormals));
				}
			}
		}
	}

	@SideOnly(Side.CLIENT)
	private static void deduplicateVertices(Map<Vertex, Vertex> cache, List<Vertex> vertices)
	{
		for (int i = 0; i < vertices.size(); i++)
		{
			Vertex vertex = vertices.get(i);
			Vertex cachedVertex = CacheUtils.cache(Vertex.class, cache, vertex);
			if (vertex != cachedVertex)
				vertices.set(i, cachedVertex);
		}
	}
}
