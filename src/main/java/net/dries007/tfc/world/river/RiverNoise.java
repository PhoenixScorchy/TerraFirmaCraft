/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.river;

import net.minecraft.util.Mth;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.RiverEdge;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

public final class RiverNoise
{
    // land near the edges of rivers, but not guaranteed away from edge
    public static RiverNoiseSampler banked(Seed seed)
    {
        return new RiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(3).spread(0.05f).scaled(-0.2f, 0.2f);
            final Noise2D bankCutNoise = new OpenSimplex2D(seed.next()).octaves(3).abs().spread(0.025).scaled(0, 1, SEA_LEVEL_Y - 4, SEA_LEVEL_Y + 30);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 57 + (distFac < 1.0 ? distFac * 6 : 6);

                final double heightInWeight = Mth.clamp(distFac - 1, 0, 2);
                final double riverWeight = 2 - heightInWeight;

                return height = Math.min((heightIn * heightInWeight + riverHeight * riverWeight) / 2, bankCutNoise.noise(x, z));
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    // high banks near the edges of rivers that decrease away from edge
    public static RiverNoiseSampler tallBanked(Seed seed)
    {
        return new RiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(3).spread(0.05f).scaled(-0.2f, 0.2f);
            final Noise2D surfaceNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.07).scaled(-2, 2);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 57 + (distFac < 1.0 ? distFac * 9 : 9) + surfaceNoise.noise(x, z);

                final double heightInWeight = Mth.clamp(distFac - 1, 0, 2);
                final double riverWeight = 2 - heightInWeight;

                return height = (heightIn * heightInWeight + riverHeight * riverWeight) / 2;
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    // wide, flat shores with slopes far from shore
    public static RiverNoiseSampler floodplain(Seed seed)
    {
        return new RiverNoiseSampler()
        {
            final Noise2D distNoise = new OpenSimplex2D(seed.forkStable().next()).octaves(4).spread(0.05f).scaled(-0.8f, 0.8f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight;
                if (distFac < 1)
                {
                    final double depth = Mth.clampedMap(info.widthSq(), RiverEdge.MIN_WIDTH * RiverEdge.MIN_WIDTH, RiverEdge.MAX_WIDTH * RiverEdge.MAX_WIDTH, 3, 6);
                    riverHeight = SEA_LEVEL_Y - 2 - depth + distFac * depth;
                }
                else if (distFac < 2.0)
                {
                    riverHeight = SEA_LEVEL_Y - 2;
                }
                else
                {
                    final double heightInWeight = Mth.clamp(2 * distFac - 4, 0, 1);
                    final double riverWeight = 1 - heightInWeight;
                    riverHeight = (SEA_LEVEL_Y - 1.5) * riverWeight + heightIn * heightInWeight;
                }

                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    // ~45 degree slopes
    public static RiverNoiseSampler wide(Seed seed)
    {
        return new RiverNoiseSampler()
        {

            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                 double riverHeight;
                if (distFac < 1) {
                    double depth = Math.ceil(Math.sqrt(info.widthSq()) / 2.5);
                    riverHeight = SEA_LEVEL_Y - 1 - (1 - distFac) * depth + baseNoise.noise(x, z);
                } else {
                    riverHeight = SEA_LEVEL_Y - 1 + (distFac - 1) * 0.3 * Math.sqrt(info.widthSq()) + baseNoise.noise(x, z);
                }

                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    // A copy of wide, but a little deeper to help with shore blending
    public static RiverNoiseSampler wideDeep(Seed seed)
    {
        return new RiverNoiseSampler()
        {

            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 0.8f + distNoise.noise(x, z);
                final double riverHeight = 55 + distFac * 7 + baseNoise.noise(x, z);

                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static RiverNoiseSampler canyon(Seed seed)
    {
        return new RiverNoiseSampler()
        {

            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-7, 3);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.3f, 0.2f);
            final Noise2D lowFreqCliffNoise = new OpenSimplex2D(seed.next()).spread(0.0007f).clamped(0, 1);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 1.3 + distNoise.noise(x, z);
                final double adjDistFac = distFac > 0.6 ? distFac * 0.4 + 0.8 : distFac;

                final double riverHeight = 55 + Mth.lerp(lowFreqCliffNoise.noise(x, z), distFac, adjDistFac) * 16 + baseNoise.noise(x, z);

                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static RiverNoiseSampler tallCanyon(Seed seed)
    {
        return new RiverNoiseSampler()
        {

            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-7, 3);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.3f, 0.2f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = info.normDistSq() * 1.3 + distNoise.noise(x, z);
                final double adjDistFac = distFac > 0.32 ? distFac * 0.2 + 1.6 : distFac;

                final double riverHeight = 55 + adjDistFac * 16 + baseNoise.noise(x, z);

                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    // Vertical cliff with 45 degree slopes above and below
    public static RiverNoiseSampler talus(Seed seed)
    {
        return new RiverNoiseSampler()
        {

            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D cliffHeightNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.1f).scaled(3f, 8f);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);

            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = Math.sqrt(info.normDistSq()) + distNoise.noise(x, z);
                final double talusRiverHeight = 55 + distFac * 12 + baseNoise.noise(x, z) + (distFac > 1.5 ? cliffHeightNoise.noise(x, z) : 0);
                final double canyonRiverHeight = 55 + info.normDistSq() * 1.3 * 16;

                // Use noise similar to tall canyon at edges of talus biomes to avoid artifacts with other river noise functions
                final double riverHeight = Mth.clampedMap(thisWeight, 0.9, 1, canyonRiverHeight, talusRiverHeight);

                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    // Rows of vertical cliffs
    public static RiverNoiseSampler terraces(Seed seed)
    {
        return new RiverNoiseSampler()
        {
            final Noise2D baseNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-2.5f, 1.5f);
            final Noise2D cliffHeightNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.1f).scaled(4f, 8f);
            final Noise2D cliffBaseNoise = new OpenSimplex2D(seed.next()).octaves(2).spread(0.06f).scaled(SEA_LEVEL_Y - 2, SEA_LEVEL_Y + 4);
            final Noise2D distNoise = new OpenSimplex2D(seed.next()).octaves(4).spread(0.05f).scaled(-0.15f, 0.15f);
            double height;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                final double distFac = Math.sqrt(info.normDistSq()) * 0.85 + distNoise.noise(x, z);
                final double slopedRiverHeight = 54 + distFac * 12 + baseNoise.noise(x, z);
                final double cliffBaseHeight = cliffBaseNoise.noise(x, z);
                final double cliffHeight = cliffHeightNoise.noise(x, z);
                // A smaller set of cliffs nearer the water
                final double lowerTerrace = slopedRiverHeight > cliffBaseHeight ? Mth.clampedMap(slopedRiverHeight, cliffBaseHeight, cliffBaseHeight + 1.1, 0, cliffHeight) : 0;
                // A larger set of cliffs up higher
                final double upperTerrace = slopedRiverHeight > cliffBaseHeight + cliffHeight ? Mth.clampedMap(slopedRiverHeight, cliffBaseHeight + cliffHeight, cliffBaseHeight + cliffHeight + 1.4, 0, cliffHeight + 4) : 0;
                final double terraceRiverHeight = slopedRiverHeight + lowerTerrace + upperTerrace;
                final double canyonRiverHeight = 55 + info.normDistSq() * 1.3 * 16;

                // Use noise similar to tall canyon at edges of terrace biomes to avoid artifacts with other river noise functions
                final double riverHeight = Mth.clampedMap(thisWeight, 0.9, 1, canyonRiverHeight, terraceRiverHeight);
                return height = Math.min(riverHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                // Carve all the way to the sky over surface rivers below sea level so no overhangs or air pockets form above them
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }

    public static RiverNoiseSampler cave(Seed seed)
    {
        return new RiverNoiseSampler()
        {

            double height, canyonHeight;

            @Override
            public double setColumnAndSampleHeight(RiverInfo info, int x, int z, double heightIn, double thisWeight)
            {
                canyonHeight = 55 + info.normDistSq() * 1.3 * 16;
                return height = Math.min(canyonHeight, heightIn);
            }

            @Override
            public double noise(int y, double noiseIn)
            {
                if (height < SEA_LEVEL_Y && y > height)
                {
                    return 1;
                }
                return y > height ? 0 : noiseIn;
            }
        };
    }
}
