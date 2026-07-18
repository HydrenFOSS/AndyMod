package com.ma4z.andymod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class AndyModConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue enabled;
    public static final ForgeConfigSpec.ConfigValue<String> provider;
    public static final ForgeConfigSpec.ConfigValue<String> apiKey;
    public static final ForgeConfigSpec.ConfigValue<String> model;
    public static final ForgeConfigSpec.DoubleValue temperature;
    public static final ForgeConfigSpec.IntValue maxTokens;
    public static final ForgeConfigSpec.IntValue timeout;

    static {
        BUILDER.push("ai");
        enabled = BUILDER.define("enabled", true);
        provider = BUILDER.define("provider", "groq");
        apiKey = BUILDER.define("apiKey", "");
        model = BUILDER.define("model", "llama-3.1-8b-instant");
        temperature = BUILDER.defineInRange("temperature", 0.7, 0.0, 2.0);
        maxTokens = BUILDER.defineInRange("maxTokens", 512, 1, 4096);
        timeout = BUILDER.defineInRange("timeout", 30, 1, 120);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }
}