package com.gamerforea.minetuner.client.model;

import com.google.common.collect.Maps;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.model.IModelCustom;
import net.minecraftforge.client.model.obj.GroupObject;
import net.minecraftforge.client.model.obj.WavefrontObject;
import org.lwjgl.opengl.GL11;

import java.util.Map;

@SideOnly(Side.CLIENT)
public final class DisplayListedWavefrontObjectWrapper implements IModelCustom
{
	private static final String ALL_KEY = null;
	private final WavefrontObject model;
	private final Map<String, Integer> lists;

	public DisplayListedWavefrontObjectWrapper(WavefrontObject model)
	{
		this.model = model;
		this.lists = Maps.newHashMapWithExpectedSize(model.groupObjects.size() + 1);

		// TODO If model.groupObjects.size() == 1 && (model.currentGroupObject == null || model.currentGroupObject.glDrawingMode == GL11.GL_TRIANGLES) then singletonMap
		// TODO Lazy initialization

		int listId = GL11.glGenLists(model.groupObjects.size() + 1);

		for (GroupObject obj : model.groupObjects)
		{
			GL11.glNewList(listId, GL11.GL_COMPILE);
			obj.render();
			GL11.glEndList();
			this.lists.put(obj.name, listId++);
		}

		// We can't reuse generated lists with GL11#glCallLists because WavefrontObject#renderAll used a single Tessellator call and specific drawMode
		GL11.glNewList(listId, GL11.GL_COMPILE);
		model.renderAll();
		GL11.glEndList();
		this.lists.put(ALL_KEY, listId);
	}

	@Override
	public String getType()
	{
		return this.model.getType();
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
				if (partName.equalsIgnoreCase(entry.getKey()))
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
			if (groupName == null)
				continue;

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
