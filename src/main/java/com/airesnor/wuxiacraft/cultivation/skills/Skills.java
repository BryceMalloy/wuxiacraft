package com.airesnor.wuxiacraft.cultivation.skills;

import com.airesnor.wuxiacraft.capabilities.CultivationProvider;
import com.airesnor.wuxiacraft.capabilities.SkillsProvider;
import com.airesnor.wuxiacraft.cultivation.ICultivation;
import com.airesnor.wuxiacraft.handlers.EventHandler;
import com.airesnor.wuxiacraft.networking.CultivationMessage;
import com.airesnor.wuxiacraft.networking.EnergyMessage;
import com.airesnor.wuxiacraft.networking.NetworkWrapper;
import com.airesnor.wuxiacraft.networking.ProgressMessage;
import com.airesnor.wuxiacraft.utils.TreeUtils;
import net.minecraft.block.IGrowable;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import java.nio.channels.AcceptPendingException;
import java.util.*;

public class Skills {

    public static final List<Skill> SKILLS = new ArrayList<>();

    public static void init() {
        SKILLS.add(GATHER_WOOD);
    }

    public static final Skill GATHER_WOOD = new Skill("gather_wood", 40f, 1.1f)
            .setAction(new ISkillAction() {
                @Override
                public void activate(EntityPlayer actor) {
                    World worldIn = actor.world;
                    ICultivation cultivation = actor.getCapability(CultivationProvider.CULTIVATION_CAP, null);
                    ISkillCap skillCap = actor.getCapability(SkillsProvider.SKILL_CAP_CAPABILITY, null);
                    RayTraceResult rtr = actor.rayTrace(6f, 1f);
                    BlockPos pos = rtr.getBlockPos();
                    if(worldIn.getBlockState(pos).getBlock() == Blocks.LOG) {
                        Stack<BlockPos> tree = TreeUtils.findTree(worldIn, pos);
                        while(!tree.isEmpty() && cultivation.hasEnergy(GATHER_WOOD.getCost())) {
                                skillCap.addScheduledBlockBreaks(tree.pop());
                                cultivation.remEnergy(GATHER_WOOD.getCost());
                                EventHandler.playerAddProgress(actor, cultivation, GATHER_WOOD.getProgress());
                        }
                    }
                }
            });

    public static final Skill ACCELERATE_GROWTH = new Skill("Accelerate growth", 80f, 4.0f).setAction(new ISkillAction() {
        @Override
        public void activate(EntityPlayer actor) {
            World worldIn = actor.world;
            RayTraceResult rtr = actor.rayTrace(8f, 1f);
            BlockPos pos = rtr.getBlockPos();
            if(pos != null) {
                List<BlockPos> neighbors = new ArrayList<>();
                neighbors.add(pos);
                neighbors.add(pos.north());
                neighbors.add(pos.east());
                neighbors.add(pos.west());
                neighbors.add(pos.south());
                neighbors.add(pos.north().east());
                neighbors.add(pos.north().west());
                neighbors.add(pos.south().east());
                neighbors.add(pos.south().west());
                for(BlockPos p : neighbors) {
                    if (worldIn.getBlockState(p).getBlock() instanceof IGrowable) {
                        IGrowable iGrowable = (IGrowable) worldIn.getBlockState(p).getBlock();
                        ICultivation cultivation = actor.getCapability(CultivationProvider.CULTIVATION_CAP, null);
                        if (iGrowable.canGrow(worldIn, p, worldIn.getBlockState(p), worldIn.isRemote)) {
                            if(cultivation.hasEnergy(ACCELERATE_GROWTH.getCost())) {
                                iGrowable.grow(worldIn, worldIn.rand, p, worldIn.getBlockState(p));
                                cultivation.remEnergy(ACCELERATE_GROWTH.getCost());
                                NetworkWrapper.INSTANCE.sendToServer(new EnergyMessage(1, ACCELERATE_GROWTH.getCost()));
                                EventHandler.playerAddProgress(actor, cultivation, ACCELERATE_GROWTH.getProgress());
                                NetworkWrapper.INSTANCE.sendToServer(new ProgressMessage(0, ACCELERATE_GROWTH.getProgress()));
                            }
                        }
                    }
                }
            }
        }
    });

}
