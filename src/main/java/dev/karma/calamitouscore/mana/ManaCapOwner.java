package dev.karma.calamitouscore.mana;

import net.minecraft.world.entity.player.Player;

/**
 * Implemented by {@code PlayerManaCapMixin} so callers with a {@link Player} in
 * hand can bind the owning player onto Brutality's shared mana capability
 * before it is read.
 */
public interface ManaCapOwner {

    void calamitouscore$bind(Player player);
}
