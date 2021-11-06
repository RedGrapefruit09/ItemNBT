package com.redgrapefruit.itemnbt3;

import com.redgrapefruit.itemnbt3.util.NbtCompoundMixinAccess;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The {@link ItemDataHelper} provides several key operations for working with {@link ItemData} instances.
 * <br><br>
 * Check out usage examples <a href="https://redgrapefruit.gitbook.io/itemnbt/api-examples/itemdatahelper">here</a>
 */
public class ItemDataHelper {
    // ItemDataHelper is static
    private ItemDataHelper() {
        throw new RuntimeException("ItemDataManager is not meant to be instantiated.");
    }

    private static <T extends ItemData> void prepare(@NotNull T instance, @NotNull ItemStack stack) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(stack);

        final NbtCompound nbt = stack.getOrCreateSubNbt(instance.getNbtCategory());

        // If the data has not been initialized yet, write in the default data
        if (nbt.isEmpty()) {
            ((NbtCompoundMixinAccess) nbt).clearNbt();
            instance.writeNbt(nbt);
        }

        instance.readNbt(nbt);
    }

    /**
     * Retrieves an {@link ItemData} attached to the given {@link ItemStack}'s NBT.
     *
     * @param factory A factory that initializes the {@link ItemData} instance. Typically, this should be a constructor reference for the instance's type.
     * @param stack The {@link ItemStack}, in whose NBT the data is located in.
     * @param <T> The type of the {@link ItemData}.
     * @return The obtained instance in the given generic type.
     */
    public static <T extends ItemData> T get(@NotNull Supplier<T> factory, @NotNull ItemStack stack) {
        Objects.requireNonNull(factory);
        Objects.requireNonNull(stack);

        final T instance = factory.get();
        prepare(instance, stack);
        return instance;
    }

    /**
     * Synchronizes any new changes in an {@link ItemData} instance with the {@link ItemStack}.
     * <br><br>
     * This method <b>has</b> to be called after you modify your {@link ItemData} instance, or else
     * your changes will be lost and the state will be reverted next time you use that instance.
     *
     * @param stack The {@link ItemStack}, in whose NBT the data is located in.
     * @param data The {@link ItemData} instance, to which changes have been made.
     */
    public static void synchronize(@NotNull ItemStack stack, @NotNull ItemData data) {
        Objects.requireNonNull(stack);
        Objects.requireNonNull(data);

        final NbtCompound nbt = stack.getOrCreateSubNbt(data.getNbtCategory());
        ((NbtCompoundMixinAccess) nbt).clearNbt();
        data.writeNbt(nbt);
    }

    /**
     * A helper that allows you to make changes to an {@link ItemData} instance, and these changes will
     * <i>automatically</i> be synchronized afterwards.
     *
     * @param stack The {@link ItemStack}, in whose NBT the data is located in.
     * @param data The {@link ItemData} instance, which you are going to modify.
     * @param usage The lambda {@link Consumer}, in which you can <b>safely</b> make changes to your instance.
     * @param <T> The type of the {@link ItemData}.
     */
    public static <T extends ItemData> void use(@NotNull ItemStack stack, @NotNull T data, @NotNull Consumer<T> usage) {
        Objects.requireNonNull(stack);
        Objects.requireNonNull(data);
        Objects.requireNonNull(usage);

        usage.accept(data);
        synchronize(stack, data);
    }
}
