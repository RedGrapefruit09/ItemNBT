package com.redgrapefruit.itemnbt3.specification.linking;

import com.redgrapefruit.itemnbt3.serializer.SerializerRegistry;
import com.redgrapefruit.itemnbt3.specification.DataCompound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public class DataLink {
    private final @NotNull Map<String, Field> fields = new HashMap<>();
    private final @NotNull Map<String, Field> composites = new HashMap<>();
    private final @NotNull Supplier<Object> factory;

    private static final @NotNull Logger LOGGER = LogManager.getLogger();

    public DataLink(@NotNull Supplier<Object> factory) {
        Objects.requireNonNull(factory);

        this.factory = factory;
    }

    public void addField(@NotNull String address, @NotNull Field field) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(field);

        fields.put(address, field);
    }

    public void addComposite(@NotNull String address, @NotNull Field field) {
        Objects.requireNonNull(address);
        Objects.requireNonNull(field);

        composites.put(address, field);
    }

    public void forwardLink(@NotNull DataCompound data, @NotNull Object instance) {
        fields.forEach((key, field) -> {
            try {
                field.set(instance, data.get(key));
            } catch (IllegalAccessException e) {
                LOGGER.error("Could not forward-link field " + field.getName() + ". Illegal access, make it public");
            }
        });

        // TODO: Composites
    }

    public void backwardLink(@NotNull DataCompound data, @NotNull Object instance) {
        fields.forEach((key, field) -> {
            Object value = null;

            try {
                value = field.get(instance);
            } catch (IllegalAccessException e) {
                LOGGER.error("Could not backward-link field " + field.getName() + ". Illegal access, make it public");
            }

            Objects.requireNonNull(value);

            data.put(key, value);
        });

        // TODO: Composites
    }

    public @NotNull Supplier<Object> getFactory() {
        return factory;
    }

    public static @NotNull DataLink create(@NotNull Class<?> clazz) {
        Objects.requireNonNull(clazz);

        if (clazz.isAnnotationPresent(Auto.class)) {
            return createAutomatic(clazz);
        } else {
            return createManual(clazz);
        }
    }

    private static @NotNull DataLink createAutomatic(@NotNull Class<?> clazz) {
        final DataLink link = new DataLink(createFactoryFromFirstConstructor(clazz));

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())) continue;

            if (SerializerRegistry.contains(field.getType())) {
                link.addField(field.getName(), field);
            } else {
                link.addComposite(field.getName(), field);
            }
        }

        return link;
    }

    private static @NotNull DataLink createManual(@NotNull Class<?> clazz) {
        final DataLink link = new DataLink(createFactoryFromFirstConstructor(clazz));

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isPublic(field.getModifiers())) continue;

            // Um yeah, name conflicts are bad
            if (field.isAnnotationPresent(com.redgrapefruit.itemnbt3.specification.linking.Field.class)) {
                final com.redgrapefruit.itemnbt3.specification.linking.Field annotation = field.getAnnotation(com.redgrapefruit.itemnbt3.specification.linking.Field.class);
                link.addField(annotation.from(), field);
            }

            if (field.isAnnotationPresent(Composite.class)) {
                final Composite annotation = field.getAnnotation(Composite.class);
                link.addComposite(annotation.from(), field);
            }
        }

        return link;
    }

    private static @NotNull Supplier<Object> createFactoryFromFirstConstructor(@NotNull Class<?> clazz) {
        return () -> {
            try {
                return clazz.getDeclaredConstructors()[0].newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Reflective generation of a data link failed while creating factory from constructor");
            }
        };
    }
}
