package com.vestalihy.compat;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * Helper to access Sable API for coordinate transformations.
 */
public class SableHelper {

    public static boolean isAvailable() {
        try {
            Class.forName("dev.ryanhcode.sable.Sable");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * @return The SubLevel object managing the position or null
     */
    public static Object getSubLevelManagingPos(Level level, BlockPos pos) {
        if (!isAvailable() || level == null || pos == null) return null;
        Object containing = Sable.HELPER.getContaining(level, pos);
        if (containing != null) {
            return containing;
        }
        try {
            dev.ryanhcode.sable.api.sublevel.SubLevelContainer container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
            if (container != null) {
                dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = container.getPlot(new net.minecraft.world.level.ChunkPos(pos));
                if (plot != null) {
                    return plot.getSubLevel();
                }
            }
        } catch (Throwable e) {
            // Fail-safe
        }
        return null;
    }

    /**
     * @return The SubLevel object managing the position or null
     */
    public static Object getSubLevelManagingPos(Level level, Vec3 pos) {
        if (!isAvailable() || pos == null) return null;
        Object containing = Sable.HELPER.getContaining(level, JOMLConversion.toJOML(pos));
        if (containing != null) {
            return containing;
        }
        return getSubLevelManagingPos(level, BlockPos.containing(pos));
    }

    public static Pose3dc getPose(SubLevel subLevel, float partialTicks) {
        if (subLevel instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel clientSubLevel) {
            return clientSubLevel.renderPose(partialTicks);
        }
        return subLevel.logicalPose();
    }

    /**
     * Transforms sublevel coordinates to world coordinates.
     */
    public static Vec3 sublevelToWorld(Object subLevelObj, Vec3 sublevelPos) {
        if (subLevelObj == null || !(subLevelObj instanceof SubLevel subLevel))
            return sublevelPos;
        
        Pose3dc pose = subLevel.logicalPose();
        return pose.transformPosition(sublevelPos);
    }

    /**
     * Transforms sublevel coordinates to world coordinates using partial ticks for interpolation.
     */
    public static Vec3 sublevelToWorld(Object subLevelObj, Vec3 sublevelPos, float partialTicks) {
        if (subLevelObj == null || !(subLevelObj instanceof SubLevel subLevel))
            return sublevelPos;
        
        Pose3dc pose = getPose(subLevel, partialTicks);
        return pose.transformPosition(sublevelPos);
    }

    /**
     * Helper to get the absolute world eye position of an entity,
     * accounting for Sable sublevels.
     */
    public static Vec3 getEyePosInWorld(Entity entity, float partialTicks) {
        if (isAvailable()) {
            return Sable.HELPER.getEyePositionInterpolated(entity, partialTicks);
        }
        return entity.getEyePosition(partialTicks);
    }

    /**
     * Transforms a direction vector from sublevel to world space.
     */
    public static Vec3 transformDirection(Object subLevelObj, Vec3 localDir) {
        if (subLevelObj == null || !(subLevelObj instanceof SubLevel subLevel))
            return localDir;
        
        Pose3dc pose = subLevel.logicalPose();
        return pose.transformNormal(localDir);
    }

    /**
     * Transforms a direction vector from sublevel to world space using partial ticks for interpolation.
     */
    public static Vec3 transformDirection(Object subLevelObj, Vec3 localDir, float partialTicks) {
        if (subLevelObj == null || !(subLevelObj instanceof SubLevel subLevel))
            return localDir;
        
        Pose3dc pose = getPose(subLevel, partialTicks);
        return pose.transformNormal(localDir);
    }

    /**
     * Extracts absolute world yaw, pitch, and roll from sublevel transform.
     * 
     * @return [yaw, pitch, roll]
     */
    public static float[] getWorldRotations(Object subLevelObj, float localYaw, float localPitch) {
        if (subLevelObj == null || !(subLevelObj instanceof SubLevel subLevel))
            return new float[] { localYaw, localPitch, 0 };

        // Local look vector based on localYaw/localPitch
        double radYaw = Math.toRadians(localYaw + 90);
        double radPitch = Math.toRadians(-localPitch);
        double lx = Math.cos(radPitch) * Math.cos(radYaw);
        double ly = Math.sin(radPitch);
        double lz = Math.cos(radPitch) * Math.sin(radYaw);

        Vec3 worldLook = transformDirection(subLevelObj, new Vec3(lx, ly, lz));

        float worldYaw = (float) Math.toDegrees(Math.atan2(worldLook.z, worldLook.x)) - 90;
        float worldPitch = (float) Math.toDegrees(Math.asin(
                Math.max(-1, Math.min(1, -worldLook.y))));

        // Compute roll by transforming local up vector
        double upLx = Math.sin(radPitch) * Math.cos(radYaw);
        double upLy = Math.cos(radPitch);
        double upLz = Math.sin(radPitch) * Math.sin(radYaw);
        Vec3 localUp = new Vec3(-upLx, upLy, -upLz);
        Vec3 worldUp = transformDirection(subLevelObj, localUp);

        // Project worldUp onto the plane perpendicular to worldLook
        Vec3 wl = worldLook.normalize();
        double dot = worldUp.dot(wl);
        Vec3 projUp = worldUp.subtract(wl.scale(dot)).normalize();

        // Reference "no-roll" up: world Y projected onto the same plane
        Vec3 refUp = new Vec3(0, 1, 0);
        double dotRef = refUp.dot(wl);
        Vec3 projRef = refUp.subtract(wl.scale(dotRef)).normalize();

        // Roll = angle between projRef and projUp around wl axis
        double cosRoll = projUp.dot(projRef);
        Vec3 cross = projRef.cross(projUp);
        double sinRoll = cross.dot(wl);
        float worldRoll = (float) Math.toDegrees(Math.atan2(sinRoll, cosRoll));

        return new float[] { worldYaw, worldPitch, worldRoll };
    }

    /**
     * Extracts absolute world yaw, pitch, and roll from sublevel transform using partial ticks for interpolation.
     * 
     * @return [yaw, pitch, roll]
     */
    public static float[] getWorldRotations(Object subLevelObj, float localYaw, float localPitch, float partialTicks) {
        if (subLevelObj == null || !(subLevelObj instanceof SubLevel subLevel))
            return new float[] { localYaw, localPitch, 0 };

        // Local look vector based on localYaw/localPitch
        double radYaw = Math.toRadians(localYaw + 90);
        double radPitch = Math.toRadians(-localPitch);
        double lx = Math.cos(radPitch) * Math.cos(radYaw);
        double ly = Math.sin(radPitch);
        double lz = Math.cos(radPitch) * Math.sin(radYaw);

        Vec3 worldLook = transformDirection(subLevelObj, new Vec3(lx, ly, lz), partialTicks);

        float worldYaw = (float) Math.toDegrees(Math.atan2(worldLook.z, worldLook.x)) - 90;
        float worldPitch = (float) Math.toDegrees(Math.asin(
                Math.max(-1, Math.min(1, -worldLook.y))));

        // Compute roll by transforming local up vector
        double upLx = Math.sin(radPitch) * Math.cos(radYaw);
        double upLy = Math.cos(radPitch);
        double upLz = Math.sin(radPitch) * Math.sin(radYaw);
        Vec3 localUp = new Vec3(-upLx, upLy, -upLz);
        Vec3 worldUp = transformDirection(subLevelObj, localUp, partialTicks);

        // Project worldUp onto the plane perpendicular to worldLook
        Vec3 wl = worldLook.normalize();
        double dot = worldUp.dot(wl);
        Vec3 projUp = worldUp.subtract(wl.scale(dot)).normalize();

        // Reference "no-roll" up: world Y projected onto the same plane
        Vec3 refUp = new Vec3(0, 1, 0);
        double dotRef = refUp.dot(wl);
        Vec3 projRef = refUp.subtract(wl.scale(dotRef)).normalize();

        // Roll = angle between projRef and projUp around wl axis
        double cosRoll = projUp.dot(projRef);
        Vec3 cross = projRef.cross(projUp);
        double sinRoll = cross.dot(wl);
        float worldRoll = (float) Math.toDegrees(Math.atan2(sinRoll, cosRoll));

        return new float[] { worldYaw, worldPitch, worldRoll };
    }

    /**
     * Safely retrieves a BlockEntity from the level, falling back to searching Sable's plot chunks directly if not found in the main world.
     */
    public static net.minecraft.world.level.block.entity.BlockEntity getBlockEntitySafely(Level level, BlockPos pos) {
        if (level == null || pos == null) return null;
        net.minecraft.world.level.block.entity.BlockEntity be = level.getBlockEntity(pos);
        if (be == null && isAvailable()) {
            try {
                dev.ryanhcode.sable.api.sublevel.SubLevelContainer container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
                if (container != null) {
                    for (dev.ryanhcode.sable.sublevel.SubLevel subLevel : container.getAllSubLevels()) {
                        if (subLevel == null) continue;
                        dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = subLevel.getPlot();
                        if (plot == null) continue;
                        
                        for (dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder holder : plot.getLoadedChunks()) {
                            if (holder == null) continue;
                            net.minecraft.world.level.chunk.LevelChunk chunk = holder.getChunk();
                            if (chunk == null) continue;
                            
                            if (chunk.getPos().x == (pos.getX() >> 4) && chunk.getPos().z == (pos.getZ() >> 4)) {
                                return chunk.getBlockEntity(pos);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // Fail-safe
            }
        }
        return be;
    }

    /**
     * Safely retrieves a BlockState from the level, falling back to searching Sable's plot chunks directly if the state returned is Air.
     */
    public static net.minecraft.world.level.block.state.BlockState getBlockStateSafely(Level level, BlockPos pos) {
        if (level == null || pos == null) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        if (state.isAir() && isAvailable()) {
            try {
                dev.ryanhcode.sable.api.sublevel.SubLevelContainer container = dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level);
                if (container != null) {
                    for (dev.ryanhcode.sable.sublevel.SubLevel subLevel : container.getAllSubLevels()) {
                        if (subLevel == null) continue;
                        dev.ryanhcode.sable.sublevel.plot.LevelPlot plot = subLevel.getPlot();
                        if (plot == null) continue;
                        
                        for (dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder holder : plot.getLoadedChunks()) {
                            if (holder == null) continue;
                            net.minecraft.world.level.chunk.LevelChunk chunk = holder.getChunk();
                            if (chunk == null) continue;
                            
                            if (chunk.getPos().x == (pos.getX() >> 4) && chunk.getPos().z == (pos.getZ() >> 4)) {
                                return chunk.getBlockState(pos);
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                // Fail-safe
            }
        }
        return state;
    }

    /**
     * Checks if the entity is seated on a Sable seat entity on the server.
     */
    public static boolean isSeatedOnSable(Entity entity) {
        if (!isAvailable() || entity == null) return false;
        try {
            return Sable.HELPER.getVehicleSubLevel(entity) != null;
        } catch (Throwable t) {
            return false;
        }
    }

}
