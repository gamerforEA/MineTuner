package com.gamerforea.minetuner.client.model;

import com.google.common.collect.Maps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelFormatException;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.WavefrontObject;
import org.lwjgl.opengl.GL11;

import java.io.InputStream;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class DisplayListedWavefrontObject extends WavefrontObject
{
	private static final String ALL_KEY = null;
	private final Map<String, Integer> lists;

	public DisplayListedWavefrontObject(ResourceLocation resource) throws ModelFormatException
	{
		super(resource);
		this.lists = this.initDisplayLists();
	}

	public DisplayListedWavefrontObject(String filename, InputStream inputStream) throws ModelFormatException
	{
		super(filename, inputStream);
		this.lists = this.initDisplayLists();
	}

	private Map<String, Integer> initDisplayLists()
	{
		Map<String, Integer> lists = Maps.newHashMapWithExpectedSize(this.groupObjects.size() + 1);

		// TODO If model.groupObjects.size() == 1 && (model.currentGroupObject == null || model.currentGroupObject.glDrawingMode == GL11.GL_TRIANGLES) then singletonMap
		// TODO Lazy initialization

		int listId = GL11.glGenLists(this.groupObjects.size() + 1);

		for (GroupObject obj : this.groupObjects)
		{
			GL11.glNewList(listId, GL11.GL_COMPILE);
			obj.render();
			GL11.glEndList();
			lists.put(obj.name, listId++);
		}

		// We can't reuse generated lists with GL11#glCallLists because WavefrontObject#renderAll used a single Tessellator call and specific drawMode
		GL11.glNewList(listId, GL11.GL_COMPILE);
		super.renderAll();
		GL11.glEndList();
		lists.put(ALL_KEY, listId);

		return lists;
	}

	@Override
	public void renderAll()
	{
		GL11.glCallList(this.lists.get(ALL_KEY));
	}

	@Override
	public void renderOnly(String... groupNames)
	{
		if (groupNames == null || groupNames.length == 0)
			return;

		for (String group : groupNames)
		{
			this.renderPart(group);
		}
	}

	@Override
	public void renderPart(String partName)
	{
		if (partName == null)
			return;

		Integer listId = this.lists.get(partName);

		if (listId == null)
		{
			for (Map.Entry<String, Integer> entry : this.lists.entrySet())
			{
				if (entry.getKey().equalsIgnoreCase(partName))
				{
					listId = entry.getValue();
					this.lists.put(partName, listId);
					break;
				}
			}
		}

		if (listId != null)
			GL11.glCallList(listId);
	}

	@Override
	public void renderAllExcept(String... excludedGroupNames)
	{
		for (Map.Entry<String, Integer> entry : this.lists.entrySet())
		{
			String groupName = entry.getKey();
			boolean skipPart = false;

			for (String excludedGroupName : excludedGroupNames)
			{
				if (excludedGroupName.equalsIgnoreCase(groupName))
				{
					skipPart = true;
					break;
				}
			}

			if (!skipPart)
				GL11.glCallList(entry.getValue());
		}
	}
}
