package cy.jdkdigital.productivetrees.common.block;

import cy.jdkdigital.productivetrees.common.block.entity.ProductiveSignBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;

public class ProductiveWallSignBlock extends WallSignBlock
{
    public ProductiveWallSignBlock(Properties properties, WoodType woodType) {
        super(properties, woodType);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProductiveSignBlockEntity(pos, state);
    }
}
