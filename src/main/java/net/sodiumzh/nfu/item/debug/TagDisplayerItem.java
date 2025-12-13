package net.sodiumzh.nfu.item.debug;

import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.sodiumzh.nfu.info.ComponentBuilder;
import net.sodiumzh.nfu.item.NFUItem;
import net.sodiumzh.nfu.util.NFUInfoStatics;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TagDisplayerItem extends NFUItem {

    public TagDisplayerItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult interactLivingEntity(Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level.isClientSide) {
            ComponentBuilder builder = ComponentBuilder.create().appendText("Entity: ").append(target.getType().getDescription())
                .appendText(" Tags: ");
            List<TagKey<EntityType<?>>> allTags = Optional.ofNullable(ForgeRegistries.ENTITIES.tags())
                .flatMap(tags -> tags.getReverseTag(target.getType()))
                .map(tag -> tag.getTagKeys().toList()).orElse(List.of());
            for (int i = 0; i < allTags.size(); ++i) {
                builder.appendText(allTags.get(i).location().toString());
                if (i != allTags.size() - 1)
                    builder.appendText("; ");
            }
            NFUInfoStatics.printMessage(player, builder.build());
        }
        return InteractionResult.sidedSuccess(player.level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        if (player == null) return InteractionResult.PASS;
        BlockState bs = player.level.getBlockState(pContext.getClickedPos());
        if (!player.level.isClientSide) {
            ComponentBuilder builder = ComponentBuilder.create().appendText("Block: ").append(bs.getBlock().getName())
                .appendText(" Tags: ");
            List<TagKey<Block>> allTags = Optional.ofNullable(ForgeRegistries.BLOCKS.tags())
                .flatMap(tags -> tags.getReverseTag(bs.getBlock()))
                .map(tag -> tag.getTagKeys().toList()).orElse(List.of());
            for (int i = 0; i < allTags.size(); ++i) {
                builder.appendText(allTags.get(i).location().toString());
                if (i != allTags.size() - 1)
                    builder.appendText("; ");
            }
            NFUInfoStatics.printMessage(player, builder.build());
        }
        return InteractionResult.sidedSuccess(player.level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        if (usedHand.equals(InteractionHand.OFF_HAND) || player.getItemInHand(InteractionHand.OFF_HAND).isEmpty())
            return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        if (!level.isClientSide) {
            Item item = player.getItemInHand(InteractionHand.OFF_HAND).getItem();
            ComponentBuilder builder = ComponentBuilder.create().appendText("Item: ").append(item.getDescription())
                .appendText(" Tags: ");
            List<TagKey<Item>> allTags = Optional.ofNullable(ForgeRegistries.ITEMS.tags())
                .flatMap(tags -> tags.getReverseTag(item))
                .map(tag -> tag.getTagKeys()
                    .sorted(Comparator.comparing(t -> t.location().toString())).toList()).orElse(List.of());
            for (int i = 0; i < allTags.size(); ++i) {
                builder.appendText(allTags.get(i).location().toString());
                if (i != allTags.size() - 1)
                    builder.appendText("; ");
            }
            NFUInfoStatics.printMessage(player, builder.build());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(usedHand), level.isClientSide);
    }
}
