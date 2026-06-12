package com.vestalihy.block;

import net.minecraft.resources.ResourceLocation;

public enum ScopeType {
    USSR("sssr_scope_overlay", "sssr_scope_tv", "sssr_reticle_overlay", "sssr_reticle_tv"),
    CHINA("china_scope_overlay", "china_scope_tv", null, "china_reticle_tv"),
    NATO("nato_scope_overlay", "nato_scope_tv", "nato_reticle_overlay", "nato_reticle_tv"),
    BTR("btr_scope_overlay", "btr_scope_overlay", "btr_reticle_overlay", null),
    COMMANDER("sssr_scope_tv", "sssr_scope_tv", "sssr_reticle_tv", "sssr_reticle_tv");

    private final String standardTexture;
    private final String thermalTexture;
    private final String reticleTexture;
    private final String thermalReticleTexture;

    ScopeType(String standardTexture, String thermalTexture, String reticleTexture, String thermalReticleTexture) {
        this.standardTexture = standardTexture;
        this.thermalTexture = thermalTexture;
        this.reticleTexture = reticleTexture;
        this.thermalReticleTexture = thermalReticleTexture;
    }

    public ResourceLocation getTextureLocation(boolean thermal) {
        String name = thermal ? thermalTexture : standardTexture;
        return ResourceLocation.fromNamespaceAndPath("vestalihy", "textures/gui/" + name + ".png");
    }

    public ResourceLocation getReticleTexture(boolean thermal) {
        String name = thermal ? thermalReticleTexture : reticleTexture;
        if (name == null)
            return null;
        return ResourceLocation.fromNamespaceAndPath("vestalihy", "textures/gui/" + name + ".png");
    }

    public String getSerializedName() {
        return name().toLowerCase();
    }
}
