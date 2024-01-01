package cy.jdkdigital.productivetrees.datagen;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import cy.jdkdigital.productivetrees.ProductiveTrees;
import cy.jdkdigital.productivetrees.common.block.ProductiveFruitBlock;
import cy.jdkdigital.productivetrees.common.feature.FruitLeafPlacerDecorator;
import cy.jdkdigital.productivetrees.common.feature.FruitLeafReplacerDecorator;
import cy.jdkdigital.productivetrees.feature.trunkplacers.CenteredUpwardsBranchingTrunkPlacer;
import cy.jdkdigital.productivetrees.registry.TreeFinder;
import cy.jdkdigital.productivetrees.registry.TreeObject;
import cy.jdkdigital.productivetrees.util.TreeUtil;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.featuresize.FeatureSize;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.SimpleStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.TrunkPlacer;
import net.minecraft.world.level.levelgen.feature.trunkplacers.UpwardsBranchingTrunkPlacer;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class FeatureProvider implements DataProvider
{
    private final PackOutput output;

    public FeatureProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        PackOutput.PathProvider placedFeaturePath = this.output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/placed_feature");
        PackOutput.PathProvider configuredFeaturePath = this.output.createPathProvider(PackOutput.Target.DATA_PACK, "worldgen/configured_feature");

        List<CompletableFuture<?>> output = new ArrayList<>();

        Map<ResourceLocation, Supplier<JsonElement>> placedFeatures = Maps.newHashMap();
        Map<ResourceLocation, Supplier<JsonElement>> configuredFeatures = Maps.newHashMap();
        TreeFinder.trees.forEach((id, treeObject) -> {
            placedFeatures.put(treeObject.getId(), getPlacedFeature(treeObject));
            if (!TreeUtil.isSpecialTree(treeObject.getId())) {
                configuredFeatures.put(treeObject.getId(), getConfiguredFeature(treeObject));
            }
        });

        placedFeatures.forEach((rLoc, supplier) -> {
            output.add(DataProvider.saveStable(cache, supplier.get(), placedFeaturePath.json(rLoc)));
        });
        configuredFeatures.forEach((rLoc, supplier) -> {
            output.add(DataProvider.saveStable(cache, supplier.get(), configuredFeaturePath.json(rLoc)));
        });

        return CompletableFuture.allOf(output.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Productive Trees Feature generator";
    }

    private Supplier<JsonElement> getPlacedFeature(TreeObject treeObject) {
        return () -> {
            JsonElement placement = PlacementModifier.CODEC.encodeStart(JsonOps.INSTANCE, BlockPredicateFilter.forPredicate(BlockPredicate.wouldSurvive(treeObject.getSaplingBlock().get().defaultBlockState(), Vec3i.ZERO))).getOrThrow(false, ProductiveTrees.LOGGER::error);
            JsonArray placementArray = new JsonArray();
            placementArray.add(placement);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("feature", treeObject.getId().toString());
            jsonObject.add("placement", placementArray);
            return jsonObject;
        };
    }

    private Supplier<JsonElement> getConfiguredFeature(TreeObject treeObject) {
        String name = treeObject.getId().getPath();
        return () -> {
            JsonObject config = new JsonObject();

            // decorators
            JsonArray decoratorArray = new JsonArray();
            if (treeObject.hasFruit()) {
                var state = treeObject.getFruitBlock().get().defaultBlockState();
                if (treeObject.getId().getPath().equals("banana")) {
                    state = state.setValue(ProductiveFruitBlock.getAgeProperty(), 1);
                }
                decoratorArray.add(fruitDecorators.containsKey(name) ? fruitDecorators.get(name).apply(SimpleStateProvider.simple(state)) : fruitDecorators.get("default").apply(SimpleStateProvider.simple(state)));
            }
            config.add("decorators", decoratorArray);
            // dirt_provider
            config.add("dirt_provider", DIRT_PROVIDER);
            // foliage_placer
            config.add("foliage_placer", foliagePlacers.containsKey(name) ? foliagePlacers.get(name) : foliagePlacers.get("default"));
            // foliage_provider
            config.add("foliage_provider", BlockStateProvider.CODEC.encodeStart(JsonOps.INSTANCE, SimpleStateProvider.simple(treeObject.getLeafBlock().get())).getOrThrow(false, ProductiveTrees.LOGGER::error));
//            config.add("foliage_provider", BlockStateProvider.CODEC.encodeStart(JsonOps.INSTANCE, SimpleStateProvider.simple(Blocks.AIR)).getOrThrow(false, ProductiveTrees.LOGGER::error));
            // minimum_size
            config.add("minimum_size", FeatureSize.CODEC.encodeStart(JsonOps.INSTANCE, new TwoLayersFeatureSize(1, 0, 1)).getOrThrow(false, ProductiveTrees.LOGGER::error));
            // trunk_placer
            config.add("trunk_placer", trunkPlacers.containsKey(name) ? trunkPlacers.get(name) : trunkPlacers.get("default"));
            // trunk_provider
            config.add("trunk_provider", BlockStateProvider.CODEC.encodeStart(JsonOps.INSTANCE, SimpleStateProvider.simple(treeObject.getLogBlock().get())).getOrThrow(false, ProductiveTrees.LOGGER::error));

            config.addProperty("force_dirt", false);
            config.addProperty("ignore_vines", true);

            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("type", "minecraft:tree");
            jsonObject.add("config", config);
            return jsonObject;
        };
    }

    private JsonElement createFoliage(int radius, int height) {
        return FoliagePlacer.CODEC.encodeStart(JsonOps.INSTANCE, new BlobFoliagePlacer(ConstantInt.of(radius), ConstantInt.of(0), height)).getOrThrow(false, ProductiveTrees.LOGGER::error);
    }

    private JsonElement createStraightTrunk(int height, int randA, int randB) {
        return TrunkPlacer.CODEC.encodeStart(JsonOps.INSTANCE, new StraightTrunkPlacer(height, randA, randB)).getOrThrow(false, ProductiveTrees.LOGGER::error);
    }

    private JsonElement createBranchingTrunk(int height, int randA, int randB, IntProvider extraBranchSteps, float placeBranchPerLogProbability, IntProvider extraBranchLength) {
        return TrunkPlacer.CODEC.encodeStart(JsonOps.INSTANCE, new CenteredUpwardsBranchingTrunkPlacer(height, randA, randB, extraBranchSteps, placeBranchPerLogProbability, extraBranchLength)).getOrThrow(false, ProductiveTrees.LOGGER::error);
    }

    private final JsonElement DIRT_PROVIDER = BlockStateProvider.CODEC.encodeStart(JsonOps.INSTANCE, SimpleStateProvider.simple(Blocks.DIRT)).getOrThrow(false, ProductiveTrees.LOGGER::error);

    private Function<SimpleStateProvider, JsonElement> createDanglerFruitProvider(float density, int maxFruits) {
        return (fruitProvider) -> TreeDecorator.CODEC.encodeStart(JsonOps.INSTANCE, new FruitLeafPlacerDecorator(density, maxFruits, fruitProvider)).getOrThrow(false, ProductiveTrees.LOGGER::error);
    }

    private final Function<SimpleStateProvider, JsonElement> MEDIUM_FRUIT_DISTRIBUTION = (fruitProvider) -> TreeDecorator.CODEC.encodeStart(JsonOps.INSTANCE, new FruitLeafReplacerDecorator(0.4f, fruitProvider)).getOrThrow(false, ProductiveTrees.LOGGER::error);
    private final Map<String, Function<SimpleStateProvider, JsonElement>> fruitDecorators = new HashMap<>() {{
        put("default", (fruitProvider) -> TreeDecorator.CODEC.encodeStart(JsonOps.INSTANCE, new FruitLeafReplacerDecorator(0.6f, fruitProvider)).getOrThrow(false, ProductiveTrees.LOGGER::error));
        put("almond", MEDIUM_FRUIT_DISTRIBUTION);
        put("avocado", (fruitProvider) -> TreeDecorator.CODEC.encodeStart(JsonOps.INSTANCE, new FruitLeafReplacerDecorator(0.3f, fruitProvider)).getOrThrow(false, ProductiveTrees.LOGGER::error));
        put("banana", createDanglerFruitProvider(0.2f, 3));
        put("red_banana", createDanglerFruitProvider(0.2f, 3));
        put("plantain", createDanglerFruitProvider(0.2f, 3));
        put("breadfruit", createDanglerFruitProvider(0.2f, 6));
        put("copoazu", createDanglerFruitProvider(0.2f, 5));
        put("coconut", createDanglerFruitProvider(0.2f, 4));
        put("cempedak", createDanglerFruitProvider(0.2f, 3));
        put("jackfruit", createDanglerFruitProvider(0.2f, 4));
        put("hala_fruit", createDanglerFruitProvider(0.2f, 4));
        put("beech", MEDIUM_FRUIT_DISTRIBUTION);
        put("butternut", MEDIUM_FRUIT_DISTRIBUTION);
        put("hazel", MEDIUM_FRUIT_DISTRIBUTION);
        put("pecan", MEDIUM_FRUIT_DISTRIBUTION);
        put("pistachio", MEDIUM_FRUIT_DISTRIBUTION);
        put("wallnut", MEDIUM_FRUIT_DISTRIBUTION);
    }};
    private final Map<String, JsonElement> foliagePlacers = new HashMap<>() {{
        put("default", createFoliage(2, 3));
        put("alder", createFoliage(4, 5));
        put("avocado", createFoliage(4, 3));
        put("banana", createFoliage(3, 1));
        put("red_banana", createFoliage(3, 1));
        put("plantain", createFoliage(3, 1));
        put("asai_palm", createFoliage(3, 2));
        put("date_palm", createFoliage(4, 2));
        put("elderberry", createFoliage(4, 4));
        put("juniper", createFoliage(2, 6));
    }};
    private final Map<String, JsonElement> trunkPlacers = new HashMap<>() {{
        put("default", createStraightTrunk(6, 2, 0));
        put("alder", createBranchingTrunk(24, 2, 2, UniformInt.of(1, 6), 0.5F, UniformInt.of(0, 1)));
        put("avocado", createStraightTrunk(9, 10, 0));
        put("banana", createStraightTrunk(5, 6, 0));
        put("red_banana", createStraightTrunk(5, 6, 0));
        put("plantain", createStraightTrunk(5, 6, 0));
        put("asai_palm", createStraightTrunk(9, 5, 2));
        put("date_palm", createStraightTrunk(8, 4, 2));
        put("beech", createStraightTrunk(20, 10, 0));
        put("copoazu", createStraightTrunk(8, 2, 1));
        put("elderberry", createStraightTrunk(5, 0, 0));
        put("juniper", createStraightTrunk(4, 0, 0));
    }};
}
