package com.volmit.iris.engine.feature.features;

import art.arcane.amulet.range.IntegerRange;
import art.arcane.source.api.NoisePlane;
import art.arcane.source.api.fractal.FractalFBMProvider;
import art.arcane.source.api.interpolator.StarcastInterpolator;
import art.arcane.source.api.noise.Generator;
import art.arcane.source.api.noise.provider.CellularProvider;
import art.arcane.source.api.noise.provider.SimplexProvider;
import com.volmit.iris.engine.Engine;
import com.volmit.iris.engine.feature.IrisFeature;
import com.volmit.iris.engine.feature.IrisFeatureSizedTarget;
import com.volmit.iris.engine.feature.IrisFeatureState;
import com.volmit.iris.engine.feature.IrisFeatureTarget;
import com.volmit.iris.platform.PlatformBlock;
import com.volmit.iris.util.ShortNoiseCache;
import lombok.AllArgsConstructor;
import lombok.Data;

public class FeatureTerrain extends IrisFeature<PlatformBlock, FeatureTerrain.TerrainFeatureState>
{
    private final PlatformBlock stone;
    private final NoisePlane generator;

    public FeatureTerrain(Engine engine)
    {
        super("terrain", engine);
        stone = engine.block("stone");
        Generator g = new Generator(new FractalFBMProvider((s) -> new CellularProvider(s), 1234));
        g.scale(0.01);
        this.generator = new StarcastInterpolator(new Generator(new SimplexProvider(engine.getWorld().getSeed()))
            .maxOutput(64)
            .minOutput(0)
            .scale(0.01).warp(g), 8, 96);
    }

    @Override
    public TerrainFeatureState prepare(Engine engine, IrisFeatureSizedTarget target) {
        final ShortNoiseCache noise = new ShortNoiseCache(target.getWidth(), target.getDepth());
        int cx,cz;

        for(int x : target.x())
        {
            cx = x - target.getOffsetX();

            for(int z : target.z())
            {
                cz = z - target.getOffsetZ();
                noise.set(cx, cz, (short) generator.noise(x, z));
            }
        }

        return new TerrainFeatureState(noise);
    }

    @Override
    public void generate(Engine engine, TerrainFeatureState state, IrisFeatureTarget<PlatformBlock> target) {
        for(int x : target.localX()) {
            for(int z : target.localZ()) {
                int h = state.getNoise().get(x, z);
                for(int y : new IntegerRange(target.y().getLeftEndpoint(), Math.min(target.y().getRightEndpoint(), h)))
                {
                    target.getHunk().set(x, y, z, stone);
                }
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class TerrainFeatureState implements IrisFeatureState {
        private final ShortNoiseCache noise;
    }
}