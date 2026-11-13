/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.carver;

import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.soil.IDirtBlock;
import net.dries007.tfc.util.Helpers;

public final class CarverHelpers
{
    public static final BlockState AIR = Blocks.CAVE_AIR.defaultBlockState();
    public static final FluidState WATER = Fluids.WATER.defaultFluidState();
    public static final FluidState LAVA = Fluids.LAVA.defaultFluidState();

    private static boolean touchesOrUnderliesFluid(ChunkAccess chunk, BlockPos pos, BlockPos.MutableBlockPos checkPos)
    {
        final int x = pos.getX();
        final int y = pos.getY();
        final int z = pos.getZ();
        final int maxY = chunk.getMaxBuildHeight() - 1;

        for (int dy = 1; dy <= 4 && y + dy <= maxY; dy++)
        {
            checkPos.set(x, y + dy, z);
            if (!chunk.getFluidState(checkPos).isEmpty())
            {
                return true;
            }
        }

        final int minChunkX = chunk.getPos().getMinBlockX();
        final int maxChunkX = chunk.getPos().getMaxBlockX();
        final int minChunkZ = chunk.getPos().getMinBlockZ();
        final int maxChunkZ = chunk.getPos().getMaxBlockZ();

        if (x > minChunkX && !chunk.getFluidState(checkPos.set(x - 1, y, z)).isEmpty()) return true;
        if (x < maxChunkX && !chunk.getFluidState(checkPos.set(x + 1, y, z)).isEmpty()) return true;
        if (z > minChunkZ && !chunk.getFluidState(checkPos.set(x, y, z - 1)).isEmpty()) return true;
        if (z < maxChunkZ && !chunk.getFluidState(checkPos.set(x, y, z + 1)).isEmpty()) return true;

        return false;
    }

    public static <C extends CarverConfiguration> boolean carveBlock(CarvingContext context, C config, ChunkAccess chunk, BlockPos.MutableBlockPos pos, BlockPos.MutableBlockPos checkPos, Aquifer aquifer, MutableBoolean reachedSurface, BiPredicate<C, BlockState> canReplaceBlock)
    {
        final BlockState stateAt = chunk.getBlockState(pos);
        if (!stateAt.getFluidState().isEmpty() || touchesOrUnderliesFluid(chunk, pos, checkPos))
        {
            return false;
        }
        if (canReplaceBlock.test(config, stateAt) || isDebugEnabled(config))
        {
            final BlockState carvingState = getCarveState(context, config, pos, aquifer);
            if (carvingState != null)
            {
                chunk.setBlockState(pos, carvingState, false);
                if (reachedSurface.isTrue())
                {
                    checkPos.setWithOffset(pos, Direction.DOWN);
                    if (chunk.getBlockState(checkPos).getBlock() instanceof IDirtBlock dirt)
                    {
                        chunk.setBlockState(checkPos, dirt.getGrass(), false);
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static <C extends CarverConfiguration> BlockState getCarveState(CarvingContext context, C config, BlockPos pos, Aquifer aquifer)
    {
        if (pos.getY() <= config.lavaLevel.resolveY(context))
        {
            return LAVA.createLegacyBlock();
        }
        else
        {
            final BlockState carveState = aquifer.computeSubstance(new DensityFunction.SinglePointContext(pos.getX(), pos.getY(), pos.getZ()) , 0);
            if (carveState == null)
            {
                return isDebugEnabled(config) ? config.debugSettings.getBarrierState() : null;
            }
            return isDebugEnabled(config) ? getDebugState(config, carveState) : carveState;
        }
    }

    public static BlockState getDebugState(CarverConfiguration config, BlockState state)
    {
        if (Helpers.isBlock(state, Blocks.AIR))
        {
            return config.debugSettings.getAirState();
        }
        else if (Helpers.isBlock(state, Blocks.WATER))
        {
            final BlockState debugState = config.debugSettings.getWaterState();
            return debugState.hasProperty(BlockStateProperties.WATERLOGGED) ? debugState.setValue(BlockStateProperties.WATERLOGGED, true) : debugState;
        }
        else
        {
            return Helpers.isBlock(state, Blocks.LAVA) ? config.debugSettings.getLavaState() : state;
        }
    }

    public static boolean isDebugEnabled(CarverConfiguration config)
    {
        return config.debugSettings.isDebugMode();
    }
}
