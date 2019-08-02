package com.gamerforea.minetuner.coremod;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.commons.lang3.tuple.MutablePair;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ListIterator;

public final class ASMTransformer implements IClassTransformer
{
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass)
	{
		if (transformedName.equals("net.minecraftforge.client.model.AdvancedModelLoader"))
		{
			byte[] bytes = transformAdvancedModelLoader(basicClass);
			CoreMod.LOGGER.error("{} transformed", transformedName);
			return bytes;
		}
		else if (transformedName.equals("net.minecraftforge.client.model.obj.ObjModelLoader"))
		{
			byte[] bytes = transformObjModelLoader(basicClass);
			CoreMod.LOGGER.error("{} transformed", transformedName);
			return bytes;
		}

		return basicClass;
	}

	private static byte[] transformAdvancedModelLoader(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		new ClassReader(basicClass).accept(classNode, 0);
		boolean modified = false;

		for (MethodNode methodNode : classNode.methods)
		{
			if (methodNode.name.equals("loadModel") && methodNode.desc.equals("(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/IModelCustom;"))
			{
				for (ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator(); iterator.hasNext(); )
				{
					AbstractInsnNode insnNode = iterator.next();
					if (insnNode.getOpcode() == Opcodes.INVOKEINTERFACE && insnNode.getType() == AbstractInsnNode.METHOD_INSN)
					{
						MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
						if (methodInsnNode.owner.equals("net/minecraftforge/client/model/IModelCustomLoader") && methodInsnNode.name
								.equals("loadInstance") && methodInsnNode.desc.equals("(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/IModelCustom;"))
							iterator.set(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/gamerforea/minetuner/coremod/ASMHooks", "loadModel", "(Lnet/minecraftforge/client/model/IModelCustomLoader;Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/IModelCustom;", false));
					}
				}

				modified = true;
				break;
			}
		}

		if (!modified)
		{
			CoreMod.LOGGER.error("{} can't be transformed", classNode.name);
			return basicClass;
		}

		ClassWriter classWriter = new ClassWriter(0);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}

	private static byte[] transformObjModelLoader(byte[] basicClass)
	{
		ClassNode classNode = new ClassNode();
		new ClassReader(basicClass).accept(classNode, 0);
		boolean modified = false;

		for (MethodNode methodNode : classNode.methods)
		{
			Deque<MutablePair<AbstractInsnNode, AbstractInsnNode>> stack = new ArrayDeque<>();

			for (ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator(); iterator.hasNext(); )
			{
				AbstractInsnNode insnNode = iterator.next();
				if (insnNode.getOpcode() == Opcodes.NEW && insnNode.getType() == AbstractInsnNode.TYPE_INSN)
				{
					TypeInsnNode newInsnNode = (TypeInsnNode) insnNode;
					if (newInsnNode.desc.equals("net/minecraftforge/client/model/obj/WavefrontObject"))
					{
						MutablePair<AbstractInsnNode, AbstractInsnNode> constructorFrame = new MutablePair<>(newInsnNode, null);

						if (iterator.hasNext())
						{
							AbstractInsnNode dupInsnNode = iterator.next();
							if (dupInsnNode.getOpcode() == Opcodes.DUP && dupInsnNode.getType() == AbstractInsnNode.INSN)
								constructorFrame.right = dupInsnNode;
						}

						stack.push(constructorFrame);
					}
				}
				else if (insnNode.getOpcode() == Opcodes.INVOKESPECIAL && insnNode.getType() == AbstractInsnNode.METHOD_INSN)
				{
					MethodInsnNode initInsnNode = (MethodInsnNode) insnNode;
					if (initInsnNode.owner.equals("net/minecraftforge/client/model/obj/WavefrontObject") && initInsnNode.name
							.equals("<init>"))
					{
						MutablePair<AbstractInsnNode, AbstractInsnNode> constructorFrame = stack.pop();
						if (constructorFrame.left != null && constructorFrame.right != null)
						{
							if (initInsnNode.desc.equals("(Lnet/minecraft/util/ResourceLocation;)V"))
							{
								methodNode.instructions.remove(constructorFrame.left);
								methodNode.instructions.remove(constructorFrame.right);
								iterator.set(new MethodInsnNode(Opcodes.INVOKESTATIC, "com/gamerforea/minetuner/coremod/ASMHooks", "createWavefrontObject", "(Lnet/minecraft/util/ResourceLocation;)Lnet/minecraftforge/client/model/obj/WavefrontObject;", false));
								modified = true;
							}
						}
					}
				}
			}
		}

		if (!modified)
		{
			CoreMod.LOGGER.error("{} can't be transformed", classNode.name);
			return basicClass;
		}

		ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(classWriter);
		return classWriter.toByteArray();
	}
}
