package com.vestalihy.client.render;

import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import io.github.ocelot.glslprocessor.api.GlslInjectionPoint;
import io.github.ocelot.glslprocessor.api.GlslParser;
import io.github.ocelot.glslprocessor.api.GlslSyntaxException;
import io.github.ocelot.glslprocessor.api.node.GlslNodeList;
import io.github.ocelot.glslprocessor.api.node.GlslTree;
import io.github.ocelot.glslprocessor.lib.anarres.cpp.LexerException;
import net.minecraft.client.renderer.RenderType;

import java.io.IOException;
import java.util.List;

public class VestalihyThermalPreProcessor implements ShaderPreProcessor {

    @Override
    public void modify(final ShaderPreProcessor.Context ctx, final GlslTree tree) throws GlslSyntaxException, IOException, LexerException {
        // Target fragment shaders for chunk rendering and entity rendering
        if (ctx.isFragment()) {
            boolean match = false;
            if (ctx instanceof final ShaderPreProcessor.MinecraftContext minecraftContext) {
                final List<RenderType> renderTypes = RenderType.chunkBufferLayers();
                for (final RenderType renderType : renderTypes) {
                    if (minecraftContext.shaderInstance().equals("rendertype_%s".formatted(renderType.name))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    String name = minecraftContext.shaderInstance();
                    if (name.startsWith("rendertype_entity_") || name.startsWith("entity_")) {
                        match = true;
                    }
                }
            }

            if (match) {
                // Add the VestalihyThermalVision uniform
                tree.getBody().add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("uniform float VestalihyThermalVision;"));

                // At the end of main, mix fragColor.rgb with white based on VestalihyThermalVision
                final GlslNodeList mainFunctionBody = tree.mainFunction().orElseThrow().getBody();
                if (mainFunctionBody != null) {
                    mainFunctionBody.add(GlslParser.parseExpression("fragColor.rgb = mix(fragColor.rgb, vec3(1.0), VestalihyThermalVision);"));
                }
            }
        }
    }
}
