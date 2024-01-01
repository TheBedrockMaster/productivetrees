package cy.jdkdigital.productivetrees.common.block;

import cy.jdkdigital.productivetrees.registry.WoodObject;
import cy.jdkdigital.productivetrees.util.TreeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class ProductivePlankBlock extends Block
{
    protected final WoodObject treeObject;

    public ProductivePlankBlock(Properties properties, WoodObject treeObject) {
        super(properties);
        this.treeObject = treeObject;
    }

    @Override
    public boolean hidesNeighborFace(BlockGetter level, BlockPos pos, BlockState state, BlockState neighborState, Direction dir) {
        String name = treeObject.getId().getPath();
        return TreeUtil.isTranslucentTree(name) && neighborState.getBlock() instanceof ProductivePlankBlock;
    }

    @Override
    public float getSpeedFactor() {
        String name = treeObject.getId().getPath();
        return name.equals("black_ember") ? 1.1f : super.getSpeedFactor();
    }

    public WoodObject getTree() {
        return treeObject;
    }
}
