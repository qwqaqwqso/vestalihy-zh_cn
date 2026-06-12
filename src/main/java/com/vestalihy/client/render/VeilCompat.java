package com.vestalihy.client.render;

import net.minecraft.resources.ResourceLocation;

public class VeilCompat {

    private static Boolean veilAvailable = null;
    private static Boolean sodiumAvailable = null;
    private static Boolean irisAvailable = null;

    public static boolean isVeilAvailable() {
        if (veilAvailable == null) {
            try {
                veilAvailable = net.neoforged.fml.ModList.get().isLoaded("veil");
            } catch (Throwable e) {
                veilAvailable = false;
            }
        }
        return veilAvailable;
    }

    public static boolean isSodiumAvailable() {
        if (sodiumAvailable == null) {
            try {
                var list = net.neoforged.fml.ModList.get();
                sodiumAvailable = list.isLoaded("sodium") || list.isLoaded("embeddium");
            } catch (Throwable e) {
                sodiumAvailable = false;
            }
        }
        return sodiumAvailable;
    }

    public static boolean isIrisAvailable() {
        if (irisAvailable == null) {
            try {
                var list = net.neoforged.fml.ModList.get();
                irisAvailable = list.isLoaded("iris") || list.isLoaded("oculus");
            } catch (Throwable e) {
                irisAvailable = false;
            }
        }
        return irisAvailable;
    }

    public static boolean isIrisShaderActive() {
        if (isIrisAvailable()) {
            try {
                Class<?> apiClass = Class.forName("net.irisshaders.iris.api.IrisApi");
                Object apiInstance = apiClass.getMethod("getInstance").invoke(null);
                return (boolean) apiClass.getMethod("isShaderPackInUse").invoke(apiInstance);
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static boolean shouldBypassVanillaShader() {
        // Bypass vanilla loadEffect() if Sodium is active, Iris is active, or Veil is present
        // because calling it under these circumstances can cause severe texture corruption or crashes.
        return isSodiumAvailable() || isIrisAvailable() || isVeilAvailable();
    }

    public static void enableThermalPipeline() {
        if (isVeilAvailable()) {
            try {
                foundry.veil.api.client.render.VeilRenderer renderer = foundry.veil.api.client.render.VeilRenderSystem.renderer();
                if (renderer != null) {
                    foundry.veil.api.client.render.post.PostProcessingManager manager = renderer.getPostProcessingManager();
                    if (manager != null) {
                        manager.add(ResourceLocation.fromNamespaceAndPath("vestalihy", "thermal"));
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static void disableThermalPipeline() {
        if (isVeilAvailable()) {
            try {
                foundry.veil.api.client.render.VeilRenderer renderer = foundry.veil.api.client.render.VeilRenderSystem.renderer();
                if (renderer != null) {
                    foundry.veil.api.client.render.post.PostProcessingManager manager = renderer.getPostProcessingManager();
                    if (manager != null) {
                        manager.remove(ResourceLocation.fromNamespaceAndPath("vestalihy", "thermal"));
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static void updateThermalUniforms(float time, float width, float height) {
        if (isVeilAvailable()) {
            try {
                foundry.veil.api.client.render.VeilRenderer renderer = foundry.veil.api.client.render.VeilRenderSystem.renderer();
                if (renderer != null) {
                    foundry.veil.api.client.render.post.PostProcessingManager manager = renderer.getPostProcessingManager();
                    if (manager != null) {
                        ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("vestalihy", "thermal");
                        if (manager.isActive(loc)) {
                            foundry.veil.api.client.render.post.PostPipeline pipeline = manager.getPipeline(loc);
                            if (pipeline != null) {
                                var timeUniform = pipeline.getUniformSafe("Time");
                                if (timeUniform != null && timeUniform.isValid()) {
                                    timeUniform.setFloat(time);
                                }
                                var inSizeUniform = pipeline.getUniformSafe("InSize");
                                if (inSizeUniform != null && inSizeUniform.isValid()) {
                                    inSizeUniform.setVector(width, height);
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
    public static void registerPreprocessors() {
        if (isVeilAvailable()) {
            try {
                foundry.veil.platform.VeilEventPlatform.INSTANCE.onVeilAddShaderProcessors((provider, registry) -> {
                    registry.addPreprocessor(new com.vestalihy.client.render.VestalihyThermalPreProcessor(), false);
                });
            } catch (Throwable ignored) {
            }
        }
    }
}

