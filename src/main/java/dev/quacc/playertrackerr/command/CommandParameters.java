package dev.quacc.playertrackerr.command;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParameters {
    String name();
    String[] aliases() default {};
    String usage() default "";
    int requiredArgs() default 0;
    String[] permissions() default {};
    boolean playerOnly() default false;
    String description() default "";
}