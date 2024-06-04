package org.xet.experiments.builder.algorithm.height_map;

import java.util.ArrayList;
import java.util.List;

public class RegistryGeneratorsHeightMap {
    private static final List<GeneratorHeightMap> generators = new ArrayList<>();

    static GeneratorHeightMap V1 = registerGenerator(new GeneratorV1());
    static GeneratorHeightMap V2 = registerGenerator(new GeneratorV2());
    static GeneratorHeightMap V3 = registerGenerator(new GeneratorV3());
    static GeneratorHeightMap V4 = registerGenerator(new GeneratorV4());

    public static GeneratorHeightMap getGeneratorByName(String name) {
        for (GeneratorHeightMap gen : generators) {
            if (gen.getName().equals(name)) {
                return gen;
            }
        }

        throw new IllegalArgumentException("Unknown generator name: " + name);
    }

    public static String[] getAvailableNames() {
        List<String> names = new ArrayList<>();
        for (GeneratorHeightMap gen : generators) {
            names.add(gen.getName());
        }
        return names.toArray(new String[0]);
    }

    private static <T extends GeneratorHeightMap> T registerGenerator(T entry) {
        RegistryGeneratorsHeightMap.generators.add(entry);
        return entry;
    }
}
