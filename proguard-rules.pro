########################################
# CORE KEEP RULES (Forge-safe)
########################################

# Main @Mod class
-keep @net.neoforged.fml.common.Mod class * {
    <init>(...);
    public static final java.lang.String MODID;
}

# EventBus
-keep @net.neoforged.fml.common.EventBusSubscriber class * {
    <init>(...);
}
-keepclassmembers class * {
    @net.neoforged.bus.api.SubscribeEvent <methods>;
}

# Prevent obfuscation of Minecraft/Forge overrides
-keepclassmembers class * extends net.minecraft.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class * implements net.minecraft.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class * extends net.neoforged.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class * implements net.neoforged.** {
    public <methods>;
    protected <methods>;
}

# Prevent obfuscation of Sable/CBC overrides
-keepclassmembers class * extends dev.ryanhcode.sable.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class * implements dev.ryanhcode.sable.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class * extends com.rbasamoyai.createbigcannons.** {
    public <methods>;
    protected <methods>;
}
-keepclassmembers class * implements com.rbasamoyai.createbigcannons.** {
    public <methods>;
    protected <methods>;
}

# Keep ScopeType enum and its members (often used by name or ordinal)
-keep class com.vestalihy.block.ScopeType { *; }
-keepclassmembers enum com.vestalihy.block.ScopeType { *; }

# Mixins (critical)
-keep class com.vestalihy.mixin.** { *; }

########################################
# AGGRESSIVE OBFUSCATION
########################################

# Use the generated confusing dictionary
-classobfuscationdictionary dictionary.txt
-obfuscationdictionary dictionary.txt
-packageobfuscationdictionary dictionary.txt

# Move everything into one confusing package
-repackageclasses 'llI_lI_'

# Remove package names completely
-flattenpackagehierarchy

# Allow modifying access
-allowaccessmodification

# Aggressive renaming
-overloadaggressively

# Strip all debug info except required attributes
-keepattributes Signature,InnerClasses,*Annotation*

# Windows-safe class naming (must keep, case-insensitivity on Windows could cause file collisions)
-dontusemixedcaseclassnames

# Optimizations
-optimizations !code/simplification/arithmetic

########################################
# STRING HARDENING (partial)
########################################

-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents META-INF/**

########################################
# REMOVE METADATA / LOGGING
########################################

-assumenosideeffects class java.lang.System {
    public static void println(...);
}

########################################
# WARNING CONTROL
########################################

-dontwarn net.minecraft.**
-dontwarn net.neoforged.**
-dontwarn org.spongepowered.asm.**
-dontwarn dev.ryanhcode.**
-dontwarn com.rbasamoyai.**
-ignorewarnings

########################################
# OUTPUT MAPPING
########################################

-printmapping build/libs/mapping.txt
