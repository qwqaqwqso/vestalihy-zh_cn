package com.vestalihy.client.render;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Рендерер для стандартного Blockbench JSON (без format_version/GeckoLib).
 * UV координаты в JSON хранятся в пикселях (0..textureSize).
 * Для нормализации в [0,1]: u = uv_value / textureSize.
 */
public class BbModelRenderer {

    private record BakedFace(
            float[] positions, // 4 вершины * 3 float (в блоках)
            float[] uvs,       // 4 UV * 2 float (нормализованные [0,1])
            float[] normal     // нормаль 3 float
    ) {}

    private final List<BakedFace> faces = new ArrayList<>();

    public static BbModelRenderer load(ResourceManager rm, ResourceLocation loc) {
        BbModelRenderer r = new BbModelRenderer();
        try (InputStream stream = rm.open(loc)) {
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();

            // UV в Blockbench entity JSON хранятся в единицах где 16 = вся текстура.
            // texture_size в JSON — размер текстуры в пикселях, но UV делитель всегда 16.
            if (!root.has("elements")) return r;

            for (JsonElement el : root.getAsJsonArray("elements")) {
                r.parseElement(el.getAsJsonObject());
            }
        } catch (Exception e) {
            // пустая модель если файл не найден или ошибка парсинга
        }
        return r;
    }

    private void parseElement(JsonObject el) {
        float[] from = jsonArr3(el.getAsJsonArray("from"));
        float[] to   = jsonArr3(el.getAsJsonArray("to"));

        // Вращение элемента (опционально)
        float[] rotOrigin = {8f, 8f, 8f};
        float   rotAngle  = 0f;
        int     rotAxis   = 1; // Y по умолчанию
        if (el.has("rotation")) {
            JsonObject rot = el.getAsJsonObject("rotation");
            rotAngle  = rot.get("angle").getAsFloat();
            String ax = rot.get("axis").getAsString();
            rotAxis   = ax.equals("x") ? 0 : ax.equals("y") ? 1 : 2;
            rotOrigin = jsonArr3(rot.getAsJsonArray("origin"));
        }

        if (!el.has("faces")) return;
        JsonObject facesObj = el.getAsJsonObject("faces");

        // UV в Blockbench entity JSON: делитель всегда 16 (BB UV space = 0..16 = вся текстура)
        final float scaleU = 1f / 16f;
        final float scaleV = 1f / 16f;

        String[] faceNames = {"north", "south", "east", "west", "up", "down"};
        for (String name : faceNames) {
            if (!facesObj.has(name)) continue;
            JsonObject face = facesObj.getAsJsonObject(name);
            if (!face.has("uv")) continue;

            JsonArray uv = face.getAsJsonArray("uv");
            float u0 = uv.get(0).getAsFloat() * scaleU;
            float v0 = uv.get(1).getAsFloat() * scaleV;
            float u1 = uv.get(2).getAsFloat() * scaleU;
            float v1 = uv.get(3).getAsFloat() * scaleV;

            // 4 вершины грани
            float[][] verts = getFaceVertices(name, from, to);
            float[]   norm  = getFaceNormal(name);

            // Применяем вращение элемента
            if (rotAngle != 0f) {
                for (float[] v : verts) {
                    rotatePoint(v, rotOrigin, rotAxis, rotAngle);
                }
                rotateNormal(norm, rotAxis, rotAngle);
            }

            // Переводим из пикселей (Blockbench 16px = 1 блок) в блоки
            float[] positions = new float[12];
            for (int i = 0; i < 4; i++) {
                positions[i*3]   = verts[i][0] / 16f;
                positions[i*3+1] = verts[i][1] / 16f;
                positions[i*3+2] = verts[i][2] / 16f;
            }

            // UV раскладка: (u0,v0) → (u0,v1) → (u1,v1) → (u1,v0)
            float[] uvCoords = {u0, v0,  u0, v1,  u1, v1,  u1, v0};

            faces.add(new BakedFace(positions, uvCoords, norm));
        }
    }

    // Вершины четырёх углов грани куба (в пикселях Blockbench)
    private static float[][] getFaceVertices(String face, float[] f, float[] t) {
        return switch (face) {
            case "north" -> new float[][]{{t[0],t[1],f[2]}, {t[0],f[1],f[2]}, {f[0],f[1],f[2]}, {f[0],t[1],f[2]}};
            case "south" -> new float[][]{{f[0],t[1],t[2]}, {f[0],f[1],t[2]}, {t[0],f[1],t[2]}, {t[0],t[1],t[2]}};
            case "east"  -> new float[][]{{t[0],t[1],t[2]}, {t[0],f[1],t[2]}, {t[0],f[1],f[2]}, {t[0],t[1],f[2]}};
            case "west"  -> new float[][]{{f[0],t[1],f[2]}, {f[0],f[1],f[2]}, {f[0],f[1],t[2]}, {f[0],t[1],t[2]}};
            case "up"    -> new float[][]{{f[0],t[1],f[2]}, {f[0],t[1],t[2]}, {t[0],t[1],t[2]}, {t[0],t[1],f[2]}};
            case "down"  -> new float[][]{{f[0],f[1],t[2]}, {f[0],f[1],f[2]}, {t[0],f[1],f[2]}, {t[0],f[1],t[2]}};
            default      -> new float[4][3];
        };
    }

    private static float[] getFaceNormal(String face) {
        return switch (face) {
            case "north" -> new float[]{  0,  0, -1};
            case "south" -> new float[]{  0,  0,  1};
            case "east"  -> new float[]{  1,  0,  0};
            case "west"  -> new float[]{ -1,  0,  0};
            case "up"    -> new float[]{  0,  1,  0};
            case "down"  -> new float[]{  0, -1,  0};
            default      -> new float[]{  0,  1,  0};
        };
    }

    private static void rotatePoint(float[] p, float[] o, int axis, float deg) {
        float cos = (float) Math.cos(Math.toRadians(deg));
        float sin = (float) Math.sin(Math.toRadians(deg));
        float dx = p[0]-o[0], dy = p[1]-o[1], dz = p[2]-o[2];
        if (axis == 0) {       // X
            p[1] = o[1] + dy*cos - dz*sin;
            p[2] = o[2] + dy*sin + dz*cos;
        } else if (axis == 1) { // Y
            p[0] = o[0] + dx*cos + dz*sin;
            p[2] = o[2] - dx*sin + dz*cos;
        } else {                // Z
            p[0] = o[0] + dx*cos - dy*sin;
            p[1] = o[1] + dx*sin + dy*cos;
        }
    }

    private static void rotateNormal(float[] n, int axis, float deg) {
        float cos = (float) Math.cos(Math.toRadians(deg));
        float sin = (float) Math.sin(Math.toRadians(deg));
        float nx = n[0], ny = n[1], nz = n[2];
        if (axis == 0) {
            n[1] = ny*cos - nz*sin;  n[2] = ny*sin + nz*cos;
        } else if (axis == 1) {
            n[0] = nx*cos + nz*sin;  n[2] = -nx*sin + nz*cos;
        } else {
            n[0] = nx*cos - ny*sin;  n[1] = nx*sin + ny*cos;
        }
    }

    private static float[] jsonArr3(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }

    /** Рисует все заранее вычисленные грани в переданный VertexConsumer. */
    public void render(PoseStack poseStack, VertexConsumer consumer, int light) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f mat  = pose.pose();
        Matrix3f norm = pose.normal();

        for (BakedFace f : faces) {
            float[] p  = f.positions();
            float[] uv = f.uvs();
            float[] n  = f.normal();

            Vector3f nVec = norm.transform(new Vector3f(n[0], n[1], n[2]));

            for (int i = 0; i < 4; i++) {
                Vector4f pos = mat.transform(new Vector4f(p[i*3], p[i*3+1], p[i*3+2], 1f));
                consumer.addVertex(pos.x(), pos.y(), pos.z())
                        .setColor(1f, 1f, 1f, 1f)
                        .setUv(uv[i*2], uv[i*2+1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light)
                        .setNormal(nVec.x(), nVec.y(), nVec.z());
            }
        }
    }
}
