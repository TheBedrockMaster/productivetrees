package cy.jdkdigital.productivetrees.event;

import cy.jdkdigital.productivelib.event.BeeReleaseEvent;
import cy.jdkdigital.productivetrees.ProductiveTrees;
import cy.jdkdigital.productivetrees.common.block.ProductiveLogBlock;
import cy.jdkdigital.productivetrees.common.block.ProductiveSaplingBlock;
import cy.jdkdigital.productivetrees.common.block.entity.PollinatedLeavesBlockEntity;
import cy.jdkdigital.productivetrees.integrations.productivebees.CompatHandler;
import cy.jdkdigital.productivetrees.registry.TreeFinder;
import cy.jdkdigital.productivetrees.registry.TreeRegistrator;
import cy.jdkdigital.productivetrees.util.TreeUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = ProductiveTrees.MODID)
public class EventHandler
{
    @SubscribeEvent
    public static void onServerStarting(AddReloadListenerEvent event) {
        TreeFinder.context = event.getConditionContext();
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            TreeFinder.trees.forEach((id, treeObject) -> {
                ComposterBlock.COMPOSTABLES.put(TreeUtil.getBlock(id, "_sapling"), 0.3f);
                ComposterBlock.COMPOSTABLES.put(TreeUtil.getBlock(id, "_leaves"), 0.3f);
                if (treeObject.hasFruit()) {
                    ComposterBlock.COMPOSTABLES.put(treeObject.getFruit().getItem().getItem(), 0.65F);
                }
            });
        });
    }

    @SubscribeEvent
    public static void blockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level && event.getState().is(TreeRegistrator.POLLINATED_LEAVES.get()) && level.getBlockEntity(event.getPos()) instanceof PollinatedLeavesBlockEntity pollinatedLeavesBlockEntity) {
            Block.popResource(level, event.getPos(), pollinatedLeavesBlockEntity.getResult().copy());
        }
    }

    @SubscribeEvent
    public static void blockToolModified(BlockEvent.BlockToolModificationEvent event) {
        if (!event.isSimulated() && event.getToolAction().equals(ToolActions.AXE_STRIP) && event.getLevel() instanceof ServerLevel level) {
            if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof ProductiveLogBlock logBlock) {
                var tree = TreeUtil.getTree(logBlock);
                if (tree != null && !tree.getStripDrop().get().isEmpty()) {
                    Block.popResource(level, event.getPos(), tree.getStripDrop().get().copy());
                }
            }
        }
    }

    @SubscribeEvent
    public static void beeRelease(BeeReleaseEvent event) {
        if (ModList.get().isLoaded("productivebees")) {
            CompatHandler.beeRelease(event);
        }
    }

    @SubscribeEvent
    public static void onBlockGrow(SaplingGrowTreeEvent event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            Block grownBlock = serverLevel.getBlockState(event.getPos()).getBlock();
            if (grownBlock instanceof ProductiveSaplingBlock saplingBlock && !saplingBlock.getTree().getMutationInfo().target().equals(ProductiveTrees.EMPTY_RL)) {
                if (saplingBlock.getTree().getMutationInfo().chance() >= event.getRandomSource().nextFloat()) {
                    event.setFeature(ResourceKey.create(Registries.CONFIGURED_FEATURE, saplingBlock.getTree().getMutationInfo().target()));
                }
            }
        }
    }

//    @SubscribeEvent
//    private void onEntityDeath(LivingDeathEvent event) {
//        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FALL)) {
//            if (event.getSource().getEntity() instanceof FallingBlockEntity fallingBlock && fallingBlock.getBlockState().is(TreeRegistrator.COCONUT_SPROUT.get())) {
//
//            }
//        }
//    }
//
//    @SubscribeEvent
//    private void onEntityHurt(LivingHurtEvent event) {
//        if (event.getEntity() instanceof Player player && event.getSource().is(DamageTypeTags.IS_FALL)) {
//            if (event.getSource().getEntity() instanceof FallingBlockEntity fallingBlock && fallingBlock.getBlockState().is(TreeRegistrator.COCONUT_SPROUT.get())) {
//
//            }
//        }
//    }
}
