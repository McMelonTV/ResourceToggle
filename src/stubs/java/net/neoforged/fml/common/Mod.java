package net.neoforged.fml.common;

import net.neoforged.api.distmarker.Dist;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Compile-time stub of the NeoForge {@code @Mod} annotation. The real
 * annotation exists on the NeoForge classpath at runtime; this stub only
 * needs to match the members used by this mod and is never packaged.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {
    String value();

    Dist[] dist() default {};

    String[] depends() default {};
}
