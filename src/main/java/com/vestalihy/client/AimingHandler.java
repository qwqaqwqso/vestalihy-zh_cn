package com.vestalihy.client;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.vestalihy.block.OpticsModuleBlock;
import com.vestalihy.block.ScopeType;
import com.vestalihy.compat.CBCHelper;
import com.vestalihy.compat.SableHelper;
import com.vestalihy.network.AimingPacket;
import com.vestalihy.network.ModNetwork;
import com.vestalihy.network.OpticsRotationPacket;
import com.vestalihy.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import java.util.Arrays;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public class AimingHandler {

    private static boolean isAiming = false;
    private static boolean thermalVisionActive = false;
    private static BlockPos cannonMountPos = null;
    private static BlockPos aimingBlockPos = null;
    private static Direction aimingFacing = Direction.NORTH;
    private static float zoomLevel = 1.0f;
    private static ResourceLocation currentScopeTexture = null;
    private static ScopeType currentScopeType = null;
    private static boolean wasThermalActive = false;
    private static BlockPos opticsModulePos = null;
    private static Direction opticsModuleFacing = Direction.NORTH;
    private static long lastScrollTime = 0;
    private static int savedHotbarSlot = -1;
    private static boolean isCommanderScope = false;
    private static float lastSentYaw = 0.0f;
    private static float lastSentPitch = 0.0f;

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final float MIN_ZOOM = 1.0f;
    private static final float MAX_ZOOM = 8.0f;
    private static final float ZOOM_STEP = 0.5f;

    public static boolean isAiming() {
        return isAiming;
    }

    public static boolean isThermalVisionActive() {
        return thermalVisionActive && isAiming;
    }

    public static boolean isCommanderScope() {
        return isCommanderScope;
    }



    private static Direction getCameraForward(Direction blockFacing) {
        return blockFacing.getOpposite();
    }

    private static float getCannonMountYaw(Level level, BlockEntity be, BlockPos mountPos, float partialTicks) {
        if (be == null || level == null || mountPos == null) {
            return 0f;
        }

        if (CBCHelper.isCompactCannonMount(be)) {
            float yaw = CBCHelper.getYawOffset(be, partialTicks);
            Object targetSubLevel = SableHelper.getSubLevelManagingPos(level, mountPos);
            if (targetSubLevel != null) {
                float[] shipRots = SableHelper.getWorldRotations(targetSubLevel, 0, 0, partialTicks);
                yaw -= shipRots[0];
            }
            return yaw;
        }

        try {
            BlockState state = SableHelper.getBlockStateSafely(level, mountPos);
            if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).toYRot();
            }
        } catch (Throwable ignored) {
        }

        return CBCHelper.getYawOffset(be, partialTicks);
    }

    public static void startAiming(BlockPos blockPos, BlockPos mountPos, Direction facing, Vec3 hitPos,
            ScopeType scopeType, BlockPos modulePos) {
        Minecraft mc = Minecraft.getInstance();

        isAiming = true;
        if (mc.player != null) {
            savedHotbarSlot = mc.player.getInventory().selected;
        }
        cannonMountPos = mountPos;
        aimingBlockPos = blockPos;
        aimingFacing = facing;
        zoomLevel = 1.0f;
        currentScopeType = scopeType;
        currentScopeTexture = scopeType.getTextureLocation(false);

        opticsModulePos = modulePos;
        if (modulePos != null && mc.level != null) {
            BlockState moduleState = SableHelper.getBlockStateSafely(mc.level, modulePos);
            if (moduleState.getBlock() instanceof OpticsModuleBlock) {
                opticsModuleFacing = moduleState.getValue(OpticsModuleBlock.FACING);
            } else {
                opticsModulePos = null;
            }
        }

        if (currentScopeType == ScopeType.BTR) {
            thermalVisionActive = false;
        }

        isCommanderScope = (scopeType == ScopeType.COMMANDER);

        if (opticsModulePos == null && currentScopeType != ScopeType.COMMANDER) {
            thermalVisionActive = false;
        }

        if (mc.level != null && mc.player != null) {
            setMinimapVisible(false);
            updateShaderEffect();

            // Sync player look to the camera immediately to avoid double-rotation.
            float targetYaw = opticsModulePos != null ? opticsModuleFacing.toYRot() : aimingFacing.toYRot();
            float targetPitch = 0f;
            try {
                BlockEntity be = cannonMountPos != null ? SableHelper.getBlockEntitySafely(mc.level, cannonMountPos) : null;
                if (be != null && CBCHelper.isCannonMount(be) && CBCHelper.isRunning(be)) {
                    targetPitch = CBCHelper.getPitchOffset(be, 1.0f);
                    targetYaw = getCannonMountYaw(mc.level, be, cannonMountPos, 1.0f);
                }
            } catch (Throwable ignored) {
            }

            if (isCommanderScope) {
                // Transform block-local angles to world since player angles are always in world space (even when passenger)
                Object subLevel = SableHelper.getSubLevelManagingPos(mc.level, opticsModulePos != null ? opticsModulePos : aimingBlockPos);
                if (subLevel != null) {
                    float[] worldRots = SableHelper.getWorldRotations(subLevel, targetYaw, targetPitch);
                    LOGGER.info("[cmd-scope] startAiming: transform local({},{}) -> world({},{})", targetYaw, targetPitch, worldRots[0], worldRots[1]);
                    targetYaw = worldRots[0];
                    targetPitch = worldRots[1];
                }
            }
            LOGGER.info("[cmd-scope] startAiming: isPassenger={} isCommander={} finalYaw={} finalPitch={}", mc.player.isPassenger(), isCommanderScope, targetYaw, targetPitch);

            mc.player.setYRot(targetYaw);
            mc.player.yRotO = targetYaw;
            mc.player.setXRot(targetPitch);
            mc.player.xRotO = targetPitch;
            mc.player.yHeadRot = targetYaw;
            mc.player.yHeadRotO = targetYaw;
            
            lastSentYaw = targetYaw;
            lastSentPitch = targetPitch;
        }

        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AimingPacket(mountPos, blockPos, true));
    }

    public static void stopAiming() {
        if (!isAiming)
            return;

        isAiming = false;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            setMinimapVisible(true);
        }

        if (com.vestalihy.client.render.VeilCompat.shouldBypassVanillaShader()) {
            com.vestalihy.client.render.VeilCompat.disableThermalPipeline();
        } else {
            if (mc.gameRenderer.currentEffect() != null) {
                mc.gameRenderer.shutdownEffect();
            }
        }

        if (opticsModulePos != null) {
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpticsRotationPacket(opticsModulePos, 0f, 0f));
        }

        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AimingPacket(cannonMountPos, aimingBlockPos, false));

        cannonMountPos = null;
        aimingBlockPos = null;
        savedHotbarSlot = -1;
        currentScopeType = null;
        currentScopeTexture = null;
        opticsModulePos = null;
        isCommanderScope = false;
    }

    private static SimpleSoundInstance thermalBackground = null;
    private static ScheduledExecutorService thermalScheduler = null;

    private static void updateShaderEffect() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null)
            return;

        if (thermalVisionActive) {
            if (com.vestalihy.client.render.VeilCompat.shouldBypassVanillaShader()) {
                com.vestalihy.client.render.VeilCompat.enableThermalPipeline();
            } else {
                mc.gameRenderer.loadEffect(ResourceLocation.fromNamespaceAndPath("vestalihy", "shaders/post/thermal.json"));
            }

            if (!wasThermalActive) {
                mc.player.playSound(ModSounds.THERMAL_ON.get(), 1.0f, 1.0f);
                wasThermalActive = true;
                thermalBackground = new SimpleSoundInstance(ModSounds.FONTS.get().getLocation(),
                        SoundSource.MASTER, 1.0f, 1.0f, SoundInstance.createUnseededRandom(),
                        false, 0, SoundInstance.Attenuation.NONE,
                        0.0D, 0.0D, 0.0D, true);

                if (thermalScheduler == null || thermalScheduler.isShutdown()) {
                    thermalScheduler = Executors.newSingleThreadScheduledExecutor();
                    thermalScheduler.scheduleAtFixedRate(() -> {
                        if (thermalVisionActive && mc.getSoundManager() != null && thermalBackground != null) {
                            mc.getSoundManager().play(thermalBackground);
                        } else {
                            thermalScheduler.shutdown();
                        }
                    }, 0, 3, TimeUnit.SECONDS);
                }
            }
        } else {
            if (com.vestalihy.client.render.VeilCompat.shouldBypassVanillaShader()) {
                com.vestalihy.client.render.VeilCompat.disableThermalPipeline();
            } else {
                mc.gameRenderer.shutdownEffect();
            }

            if (wasThermalActive) {
                mc.player.playSound(ModSounds.THERMAL_OFF.get(), 1.0f, 1.0f);
                wasThermalActive = false;
                if (thermalBackground != null) {
                    mc.getSoundManager().stop(thermalBackground);
                    thermalBackground = null;
                }

                if (thermalScheduler != null && !thermalScheduler.isShutdown()) {
                    thermalScheduler.shutdown();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!isAiming)
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stopAiming();
            return;
        }

        if (isCommanderScope) {
            float pitch = player.getXRot();
            if (pitch < -30.0f) {
                player.setXRot(-30.0f);
                player.xRotO = -30.0f;
            } else if (pitch > 30.0f) {
                player.setXRot(30.0f);
                player.xRotO = 30.0f;
            }

            float currentYaw = player.getYRot();
            float currentPitch = player.getXRot();
            if (opticsModulePos != null && (Math.abs(currentYaw - lastSentYaw) > 1.0f || Math.abs(currentPitch - lastSentPitch) > 1.0f)) {
                float localYaw = currentYaw;
                // Always translate player world yaw to ship-local yaw before sending to server
                Object subLevel = SableHelper.getSubLevelManagingPos(mc.level, opticsModulePos);
                if (subLevel != null) {
                    float[] shipRots = SableHelper.getWorldRotations(subLevel, 0.0f, 0.0f);
                    float shipYaw = shipRots[0];
                    localYaw = (currentYaw - shipYaw) % 360.0f;
                }
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new OpticsRotationPacket(opticsModulePos, localYaw, 0.0f));
                lastSentYaw = currentYaw;
                lastSentPitch = currentPitch;
            }
        } else {
            try {
                net.minecraft.world.level.block.entity.BlockEntity be = cannonMountPos != null ? SableHelper.getBlockEntitySafely(mc.level, cannonMountPos) : null;
                boolean hasCannon = be != null && CBCHelper.isCannonMount(be) && CBCHelper.isRunning(be);
                if (!hasCannon) {
                    float targetYaw = opticsModulePos != null ? opticsModuleFacing.toYRot() : aimingFacing.toYRot();
                    float targetPitch = 0f;
                    if (be != null) {
                        targetPitch = CBCHelper.getPitchOffset(be, 1.0f);
                    }
                    
                    // Переводим локальные углы прицеливания в мировые перед записью игроку
                    Object subLevel = SableHelper.getSubLevelManagingPos(mc.level, opticsModulePos != null ? opticsModulePos : aimingBlockPos);
                    if (subLevel != null) {
                        float[] worldRots = SableHelper.getWorldRotations(subLevel, targetYaw, targetPitch);
                        targetYaw = worldRots[0];
                        targetPitch = worldRots[1];
                    }
                    
                    player.setYRot(targetYaw);
                    player.yRotO = targetYaw;
                    player.setXRot(targetPitch);
                    player.xRotO = targetPitch;
                    player.yHeadRot = targetYaw;
                    player.yHeadRotO = targetYaw;
                }
            } catch (Throwable ignored) {
            }
        }

        if (savedHotbarSlot != -1) {
            player.getInventory().selected = savedHotbarSlot;
        }

        if (opticsModulePos != null) {
            if (!(SableHelper.getBlockStateSafely(mc.level, opticsModulePos).getBlock() instanceof OpticsModuleBlock)) {
                opticsModulePos = null;
                if (thermalVisionActive && currentScopeType != ScopeType.COMMANDER) {
                    thermalVisionActive = false;
                    updateShaderEffect();
                }
            }
        }

        if (player.input.shiftKeyDown) {
            stopAiming();
            wasThermalActive = false;
            if (com.vestalihy.client.render.VeilCompat.shouldBypassVanillaShader()) {
                com.vestalihy.client.render.VeilCompat.disableThermalPipeline();
            } else {
                if (mc.gameRenderer != null) {
                    mc.gameRenderer.shutdownEffect();
                }
            }
            if (thermalBackground != null && mc.getSoundManager() != null) {
                mc.getSoundManager().stop(thermalBackground);
            }
            thermalBackground = null;
            return;
        }

        if (cannonMountPos != null && SableHelper.getBlockEntitySafely(mc.level, cannonMountPos) == null) {
            stopAiming();
            return;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        if (isAiming) {
            updateShaderTime();
        }
    }

    private static void updateShaderTime() {
        if (!thermalVisionActive)
            return;

        Minecraft mc = Minecraft.getInstance();
        float time = (float) (System.currentTimeMillis() % 100000) / 1000.0f;

        if (com.vestalihy.client.render.VeilCompat.shouldBypassVanillaShader()) {
            float width = mc.getWindow().getWidth();
            float height = mc.getWindow().getHeight();
            com.vestalihy.client.render.VeilCompat.updateThermalUniforms(time, width, height);
            return;
        }

        if (mc.gameRenderer == null)
            return;

        net.minecraft.client.renderer.PostChain effectChain = mc.gameRenderer.currentEffect();
        if (effectChain != null) {
            try {
                java.lang.reflect.Field passesField = null;
                try {
                    passesField = net.minecraft.client.renderer.PostChain.class.getDeclaredField("passes");
                } catch (NoSuchFieldException e) {
                    passesField = net.minecraft.client.renderer.PostChain.class.getDeclaredField("f_110009_");
                }
                passesField.setAccessible(true);
                java.util.List<net.minecraft.client.renderer.PostPass> passes = (java.util.List<net.minecraft.client.renderer.PostPass>) passesField
                        .get(effectChain);

                for (net.minecraft.client.renderer.PostPass pass : passes) {
                    java.lang.reflect.Field shaderField = null;
                    try {
                        shaderField = net.minecraft.client.renderer.PostPass.class.getDeclaredField("effect");
                    } catch (NoSuchFieldException e) {
                        shaderField = net.minecraft.client.renderer.PostPass.class.getDeclaredField("f_110196_");
                    }
                    shaderField.setAccessible(true);
                    net.minecraft.client.renderer.ShaderInstance shader = (net.minecraft.client.renderer.ShaderInstance) shaderField
                            .get(pass);

                    if (shader != null) {
                        com.mojang.blaze3d.shaders.AbstractUniform timeUniform = shader.getUniform("Time");
                        if (timeUniform != null) {
                            timeUniform.set(time);
                        }
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        if (!isAiming)
            return;
        event.setFOV(event.getFOV() / zoomLevel);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        if (isThermalVisionActive()) {
            event.setRed(0);
            event.setGreen(0);
            event.setBlue(0);
        }
    }

    public static Vec3 getAbsoluteWorldCameraPosition(float partialTicks) {
        if (!isAiming) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        BlockPos pos = opticsModulePos != null ? opticsModulePos : aimingBlockPos;
        Direction facing = opticsModulePos != null ? opticsModuleFacing : aimingFacing;
        if (pos == null) return null;

        Vec3 worldPos;
        if (isCommanderScope) {
            Vec3 center = Vec3.atCenterOf(pos);
            if (opticsModulePos != null) {
                BlockEntity moduleBe = SableHelper.getBlockEntitySafely(mc.level, opticsModulePos);
                if (moduleBe instanceof com.vestalihy.block.OpticsModuleBlockEntity optics) {
                    center = center.add(optics.getOffsetX(), optics.getOffsetY(), optics.getOffsetZ());
                }
            }

            Object subLevel = SableHelper.getSubLevelManagingPos(mc.level, pos);
            if (subLevel != null) {
                dev.ryanhcode.sable.companion.math.Pose3dc pose = SableHelper.getPose((dev.ryanhcode.sable.sublevel.SubLevel) subLevel, partialTicks);
                if (pose != null) {
                    center = pose.transformPosition(center);
                }
            }
            Quaternionf globalRot = getAbsoluteWorldCameraRotation(partialTicks);
            Vector3f forwardVec = new Vector3f(0.0F, 0.0F, -1.0F).rotate(globalRot);
            Vec3 lookVec = new Vec3(forwardVec.x(), forwardVec.y(), forwardVec.z());
            worldPos = center.add(lookVec.scale(0.65));
            LOGGER.info("[cmd-scope] getPosition: isPassenger={} lookVec={} worldPos={}", mc.player.isPassenger(), lookVec, worldPos);
        } else {
            Vec3 localPos = getCameraLocalPosition(mc.level, pos, facing);
            Object subLevel = SableHelper.getSubLevelManagingPos(mc.level, pos);
            if (subLevel != null) {
                dev.ryanhcode.sable.companion.math.Pose3dc pose = SableHelper.getPose((dev.ryanhcode.sable.sublevel.SubLevel) subLevel, partialTicks);
                if (pose != null) {
                    return pose.transformPosition(localPos);
                }
            }
            worldPos = localPos;
        }
        
        return worldPos;
    }

    private static Vec3 getCameraLocalPosition(Level level, BlockPos blockPos, Direction facing) {
        Vec3 start = Vec3.atCenterOf(blockPos);
        
        // If using optics module, add its offset
        if (opticsModulePos != null) {
            BlockEntity moduleBe = SableHelper.getBlockEntitySafely(level, opticsModulePos);
            if (moduleBe instanceof com.vestalihy.block.OpticsModuleBlockEntity optics) {
                start = start.add(optics.getOffsetX(), optics.getOffsetY(), optics.getOffsetZ());
            }
        }
        
        Vec3 dir = Vec3.atLowerCornerOf(facing.getNormal());

        // Check blocks in front directly using block states (rifo-master approach)
        BlockPos pos1 = blockPos.relative(facing);
        BlockPos pos2 = blockPos.relative(facing, 2);

        boolean hasBlock1 = !level.getBlockState(pos1).isAir();
        boolean hasBlock2 = !level.getBlockState(pos2).isAir();

        double offset = 0.5; // Default front face of block

        if (hasBlock1) {
            if (hasBlock2) {
                // Both blocks are solid. Skip past both! Place at 2.5
                offset = 2.5;
            } else {
                // Only block 1 is solid. Skip past it! Place at 1.5
                offset = 1.5;
            }
        }

        return start.add(dir.scale(offset));
    }

    public static Quaternionf getAbsoluteWorldCameraRotation(float partialTicks) {
        if (!isAiming) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;

        BlockPos pos = opticsModulePos != null ? opticsModulePos : aimingBlockPos;
        Direction facing = opticsModulePos != null ? opticsModuleFacing : aimingFacing;
        if (pos == null) return null;

        Quaternionf localRot = new Quaternionf();
        if (isCommanderScope) {
            float yaw = mc.player.getViewYRot(partialTicks);
            float pitch = mc.player.getViewXRot(partialTicks);
            
            // Player yaw/pitch are ALWAYS in world space, even when sitting.
            localRot.rotationYXZ(
                    -yaw * (float) (Math.PI / 180.0f) + (float) Math.PI,
                    -pitch * (float) (Math.PI / 180.0f),
                    0.0f
            );
            
            LOGGER.info("[cmd-scope] getRotation: yaw={} pitch={} rot={}", yaw, pitch, localRot);
            
            return localRot;
        } else {
            // Get cannon pitch and yaw if available
            float pitch = 0f;
            float yaw = facing.toYRot();
            
            BlockEntity be = cannonMountPos != null ? SableHelper.getBlockEntitySafely(mc.level, cannonMountPos) : null;
            boolean hasCannon = be != null && CBCHelper.isCannonMount(be) && CBCHelper.isRunning(be);

            if (hasCannon) {
                // Get pitch from cannon and invert it so camera follows cannon direction
                pitch = -CBCHelper.getPitchOffset(be, partialTicks);
                
                // Get yaw from cannon
                yaw = CBCHelper.getYawOffset(be, partialTicks);
                
                // Check if this is a compact cannon mount
                boolean isCompact = CBCHelper.isCompactCannonMount(be);
                
                // Get the base facing of the cannon mount to apply correct rotation
                try {
                    BlockState mountState = SableHelper.getBlockStateSafely(mc.level, cannonMountPos);
                    if (mountState.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
                        Direction mountFacing = mountState.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
                        
                        if (isCompact) {
                            // Compact cannon mounts: only South and North need 180 degree correction
                            if (mountFacing == Direction.SOUTH || mountFacing == Direction.NORTH) {
                                yaw += 180f;
                            }
                            // East and West work perfectly without correction
                        } else {
                            // Regular cannon mounts need 180 degree correction for all directions
                            yaw += 180f;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            // Create local rotation
            if (hasCannon) {
                // For active cannon: use yaw directly without the 180 degree flip
                localRot.rotationYXZ(
                        -yaw * (float) (Math.PI / 180.0f),
                        -pitch * (float) (Math.PI / 180.0f),
                        0.0f
                );
            } else {
                // For static aiming block: keep the original formula with 180 degree flip
                localRot.rotationYXZ(
                        -yaw * (float) (Math.PI / 180.0f) + (float) Math.PI,
                        -pitch * (float) (Math.PI / 180.0f),
                        0.0f
                );
            }
        }

        // Get sublevel and apply its rotation
        Object subLevel = SableHelper.getSubLevelManagingPos(mc.level, pos);
        if (subLevel != null) {
            dev.ryanhcode.sable.companion.math.Pose3dc pose = SableHelper.getPose((dev.ryanhcode.sable.sublevel.SubLevel) subLevel, partialTicks);
            if (pose != null) {
                Quaternionf poseQuat = new Quaternionf(pose.orientation());
                return new Quaternionf(poseQuat).mul(localRot);
            }
        }

        return localRot;
    }

    public static void handleMouseScrollDirect(double delta) {
        if (!isAiming)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && savedHotbarSlot != -1) {
            mc.player.getInventory().selected = savedHotbarSlot;
        }

        long now = System.currentTimeMillis();
        if (now - lastScrollTime < 100) { // 100 ms debounce for physical notches
            return;
        }
        lastScrollTime = now;

        if (delta > 0) {
            zoomLevel = Math.min(MAX_ZOOM, zoomLevel + ZOOM_STEP);
        } else if (delta < 0) {
            zoomLevel = Math.max(MIN_ZOOM, zoomLevel - ZOOM_STEP);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        if (!isAiming)
            return;
        
        if (Keybinds.THERMAL_VISION_KEY.consumeClick()) {
            if (currentScopeType != ScopeType.BTR && (opticsModulePos != null || currentScopeType == ScopeType.COMMANDER)) {
                thermalVisionActive = !thermalVisionActive;
                updateShaderEffect();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        if (isAiming && event.getButton() == 0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isAiming) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() && event.getEntity().isShiftKeyDown() && event.getItemStack().isEmpty()) {
            BlockState state = event.getLevel().getBlockState(event.getPos());
            if (state.getBlock() instanceof OpticsModuleBlock) {
                BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
                if (be instanceof com.vestalihy.block.OpticsModuleBlockEntity optics) {
                    Minecraft.getInstance().setScreen(new com.vestalihy.client.gui.OpticsModuleScreen(event.getPos(), optics.getOffsetX(), optics.getOffsetY(), optics.getOffsetZ()));
                    event.setCanceled(true);
                    event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onMovementInput(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) {
        if (!isAiming)
            return;

        var input = event.getInput();
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.forwardImpulse = 0;
        input.leftImpulse = 0;
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (isAiming) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderGuiLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!isAiming) return;
        
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            event.setCanceled(true);
        } else if (event.getName().equals(VanillaGuiLayers.HOTBAR)) {
            event.setCanceled(true);

            if (currentScopeTexture == null) return;

            int width = event.getGuiGraphics().guiWidth();
            int height = event.getGuiGraphics().guiHeight();

            // Enable blending and reset shader color to fix transparency issues under Sodium
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

            var mc = Minecraft.getInstance();
            var player = mc.player;
            net.minecraft.client.gui.GuiGraphics graphics = event.getGuiGraphics();
            ResourceLocation texture = currentScopeType.getTextureLocation(isThermalVisionActive());
            graphics.blit(texture, 0, 0, 0, 0, width, height, width, height);

            ResourceLocation reticle = currentScopeType.getReticleTexture(isThermalVisionActive());
            if (reticle != null && (isThermalVisionActive() || currentScopeType != ScopeType.CHINA)) {
                graphics.blit(reticle, 0, 0, 0, 0, width, height, width, height);
            }

            // Flush GuiGraphics to draw the textures immediately with active blending state
            graphics.flush();
        }
    }

    private static void setMinimapVisible(boolean visible) {
        try {
            Class.forName("xaero.minimap.XaeroMinimap");
            var settings = Class.forName("xaero.minimap.XaeroMinimap").getMethod("getSettings").invoke(null);
            settings.getClass().getField("minimap").set(settings, visible);
        } catch (Exception ignored) {
        }

        try {
            Class.forName("journeymap.client.core.JourneyMapClient");
            var jmInstance = Class.forName("journeymap.client.core.JourneyMapClient").getMethod("getInstance").invoke(null);
            jmInstance.getClass().getMethod("setUiVisible", boolean.class).invoke(jmInstance, visible);
        } catch (Exception ignored) {
        }
    }
}
