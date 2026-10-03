package com.yuan;

import com.yuan.space_slash.YuanSpaceSlashEffect;
import java.nio.file.Files;
import java.nio.file.Path;
import org.joml.Vector3f;

public final class YuanSpaceSlashCheck {
    public static void main(String[] args) throws Exception {
        String yuan = Files.readString(Path.of("src/main/java/com/yuan/Yuan.java"));
        assert yuan.contains("registerMessage(10, YuanSpaceSlashPacket.class")
                : "space slash packet id 10 must be registered";
        assert yuan.contains("ENTITY_TYPES") && yuan.contains("SPACE_SLASH_ENTITY")
                && yuan.contains("YuanSpaceSlashEntity::new")
                : "space slash must register a client visual entity type";

        String item = Files.readString(Path.of("src/main/java/com/yuan/item/YuanGodSwordItem.java"));
        assert item.contains("public boolean hurtEnemy")
                : "god sword must expose server-side hurtEnemy";
        assert item.contains("super.hurtEnemy")
                : "hurtEnemy must preserve the original weapon damage";
        assert item.contains("YuanSpaceSlashPacket") && item.contains("TRACKING_ENTITY_AND_SELF")
                : "hurtEnemy must broadcast the space slash packet to tracking players";
        assert item.contains("!attacker.level().isClientSide")
                : "space slash broadcast must run server-side only";
        assert item.contains("getYRot") && item.contains("getXRot")
                : "hurtEnemy must send the attacker's slash orientation";
        assert item.contains("getEyePosition") && item.contains("getLookAngle")
                && item.contains("getBoundingBox().clip")
                : "hurtEnemy must raycast the attacker's look ray against the target hitbox";

        String packet = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashPacket.java"));
        assert packet.contains("entityId") && packet.contains("seed")
                && packet.contains("yaw") && packet.contains("pitch") && packet.contains("roll")
                : "space slash packet must carry target id, seed and orientation";
        assert packet.contains("hitX") && packet.contains("hitY") && packet.contains("hitZ")
                : "space slash packet must carry the hit position";
        assert packet.contains("YuanSpaceSlashRender.spawn")
                : "client packet handler must spawn the custom world effect";

        String render = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashRender.java"));
        assert render.contains("RenderLevelStageEvent") && render.contains("AFTER_PARTICLES")
                : "space slash must render through the level stage event";
        assert render.contains("AFTER_LEVEL") && render.contains("YuanIris.isShaderPackActive()")
                && render.contains("ModList.get().isLoaded(\"oculus\")")
                : "space slash must move to AFTER_LEVEL while shaders are active";
        assert render.contains("YuanRenderStateSnapshot.capture()")
                && render.contains("capturedState.restore()")
                : "space slash must restore the captured render state before drawing at AFTER_LEVEL";
        assert render.contains("YuanSpaceSlashEntity") && render.contains("mc.level.addEntity")
                : "space slash must spawn a visual entity through the client level while shaders are active";
        assert render.contains("value = Dist.CLIENT")
                : "space slash render subscriber must be restricted to Dist.CLIENT";
        assert !render.contains("Particle")
                : "space slash must not use the vanilla particle pipeline";

        String effect = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashEffect.java"));
        assert effect.contains("SLASH_BODY") && effect.contains("SLASH_GLOW")
                : "space slash effect must draw a 3D body layer and an additive glow layer";
        assert effect.contains("YuanIris.isShaderPackActive()") && effect.contains("SLASH_FALLBACK")
                : "space slash must fall back to vanilla render types while shaders are active";
        assert effect.contains("ModList.get().isLoaded(\"oculus\")")
                : "space slash must use the fallback path whenever Oculus is installed";
        assert effect.contains("YuanSpaceSlashShaders.configureBody(params, progress, seed)")
                && effect.contains("YuanSpaceSlashShaders.configureGlow(params, progress, seed)")
                : "space slash effect must configure the custom shader before drawing";
        assert effect.contains("YuanSpaceSlashMesh") && effect.contains("drawSlashBlade")
                && effect.contains("drawSlashGlow")
                : "space slash effect must render a 3D blade plus a subtle glow ribbon";
        assert !effect.contains("ribbonRoll") && !effect.contains("150.0f * progress")
                : "space slash must stay fixed to the attack direction instead of spinning";
        assert effect.contains("getEntity") && effect.contains("lastPos")
                : "space slash effect must follow the target and retain a last position fallback";
        assert effect.contains("offset") && effect.contains("entity.position().add(offset)")
                : "space slash effect must stay glued to the server-side hit position";
        assert effect.contains("getLookVector") && effect.contains("getUpVector")
                && effect.contains("PARALLEL_EPSILON")
                : "space slash must project onto the camera plane and never collapse to an edge-on line";
        assert effect.contains("initOrientation") && effect.contains("longAxis == null")
                : "space slash orientation must be captured once and not follow the camera every frame";
        String entityRenderer = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashEntityRenderer.java"));
        assert effect.contains("normalizeOrElse(") && entityRenderer.contains("normalizeOrElse(")
                : "both orientation copies must guard degeneracy before normalizing";
        assert !effect.contains("this.side.lengthSquared()") && !effect.contains("this.thick.lengthSquared()")
                && !entityRenderer.contains("side.lengthSquared() <") && !entityRenderer.contains("thick.lengthSquared() <")
                : "a length check placed after normalize() can never fire, because NaN compares false";
        Vector3f fallback = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f degenerate = YuanSpaceSlashEffect.normalizeOrElse(new Vector3f(), fallback);
        assert Float.isFinite(degenerate.x()) && Float.isFinite(degenerate.y())
                && Float.isFinite(degenerate.z())
                : "a zero-length cross product must fall back instead of normalizing to NaN";
        assert Math.abs(degenerate.length() - 1.0f) < 1.0e-4f
                : "the fallback axis must stay unit length";
        Vector3f diagonal = YuanSpaceSlashEffect.normalizeOrElse(new Vector3f(3.0f, 4.0f, 0.0f), fallback);
        assert Math.abs(diagonal.length() - 1.0f) < 1.0e-5f && diagonal.x() > 0.5f && diagonal.y() > 0.79f
                : "a non-degenerate vector must normalize to itself";
        assert effect.contains("params.durationSeconds") && !effect.contains("FLASH_FRACTION")
                : "space slash effect must use the configurable duration";
        String mesh = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashMesh.java"));
        assert mesh.contains("drawSlashBlade") && mesh.contains("drawSlashGlow")
                && !mesh.contains("drawHalfDisc")
                : "space slash mesh must build a tapered 3D blade plus a glow ribbon";
        assert mesh.contains("drawSlashBladeFallback") && mesh.contains("drawSlashGlowFallback")
                : "space slash fallback mesh must exist for Iris/Oculus";
        assert mesh.contains("Math.abs(2.0f * t0 - 1.0f)")
                : "space slash blade must taper linearly to sharp tips like the reference screenshot";
        assert !mesh.contains("arcBend") && !mesh.contains("Math.sin(Math.PI")
                : "space slash blade must stay straight until the user confirms an arc version";
        assert mesh.contains("thickness") && mesh.contains("sideQuad")
                : "space slash blade must have real thickness with side faces";
        assert mesh.contains("LightTexture.FULL_BRIGHT")
                : "space slash blade must render fullbright";

        String renderType = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashRenderType.java"));
        assert renderType.contains("NEW_ENTITY")
                && renderType.contains("YuanSpaceSlashShaders")
                : "space slash must define a custom shader path for non-shader rendering";
        assert renderType.contains("SLASH_FALLBACK") && renderType.contains("SLASH_GLOW_FALLBACK")
                && renderType.contains("getRendertypeEntityTranslucentShader")
                && renderType.contains("textures/misc/white.png")
                : "space slash fallback render types must use the vanilla translucent shader and white texture";
        assert renderType.contains("LEQUAL_DEPTH_TEST")
                : "space slash fallback must enable depth testing so the slash can be occluded";
        assert renderType.contains("TRANSLUCENT_TRANSPARENCY") && renderType.contains("ADDITIVE_TRANSPARENCY")
                : "space slash must use a black body pass plus a controlled additive glow pass";
        String shaders = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashShaders.java"));
        assert shaders.contains("RegisterShadersEvent")
                && shaders.contains("space_slash_body") && shaders.contains("space_slash_glow")
                : "space slash body and glow shaders must both be registered";
        assert shaders.contains("bodySeed.set((float) seed)")
                && shaders.contains("glowSeed.set((float) seed)")
                : "space slash shader uniforms must be set as floats to avoid vanilla int buffer NPE";
        assert Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_body.json"))
                && Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_body.vsh"))
                && Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_body.fsh"))
                && Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_glow.json"))
                && Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_glow.vsh"))
                && Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_glow.fsh"))
                : "space slash body and glow shader resources must exist";
        assert Files.readString(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash_body.fsh"))
                .contains("smoothstep(1.0, CoreWidth, d)")
                : "space slash black core must taper smoothly toward the top and bottom edges";
        assert !Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash.json"))
                && !Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash.vsh"))
                && !Files.exists(Path.of("src/main/resources/assets/yuan/shaders/core/space_slash.fsh"))
                : "old shared-Mode space slash shader resources must be removed";
        assert !Files.exists(Path.of("src/main/resources/assets/yuan/textures/effect/space_slash.png"))
                : "space slash sticker texture must be removed after switching to the shader";
        assert Files.exists(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashMesh.java"))
                : "space slash ribbon mesh generator missing";
        assert Files.exists(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashEntity.java"))
                && Files.exists(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashEntityRenderer.java"))
                : "space slash entity and renderer classes must exist";
        String renderer = Files.readString(Path.of("src/main/java/com/yuan/space_slash/YuanSpaceSlashEntityRenderer.java"));
        assert renderer.contains("SLASH_FALLBACK") && renderer.contains("SLASH_GLOW_FALLBACK")
                && renderer.contains("drawSlashGlowFallback")
                : "space slash entity renderer must draw both the body and the additive glow fallback layers";
        String client = Files.readString(Path.of("src/main/java/com/yuan/client/YuanClient.java"));
        assert client.contains("EntityRenderersEvent.RegisterRenderers")
                && client.contains("YuanSpaceSlashEntityRenderer::new")
                : "space slash entity renderer must be registered on the client";
    }
}
