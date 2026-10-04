package com.yuan;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

public final class YuanRenderCheck {
    public static void main(String[] args) throws Exception {
        String client = read("src/main/java/com/yuan/client/YuanClient.java");
        String shaders = read("src/main/java/com/yuan/client/shader/ModShaders.java");
        String renderTypes = read("src/main/java/com/yuan/client/shader/ModRenderType.java");
        String shaderConfig = read("src/main/resources/assets/yuan/shaders/core/rainbow_slime_block.json");
        String vertex = read("src/main/resources/assets/yuan/shaders/core/rainbow_slime_block.vsh");
        String fragment = read("src/main/resources/assets/yuan/shaders/core/rainbow_slime_block.fsh");
        String liquidConfig = read("src/main/resources/assets/yuan/shaders/core/liquid_glass.json");
        String liquidFragment = read("src/main/resources/assets/yuan/shaders/core/liquid_glass.fsh");
        String liquidRenderer = read("src/main/java/com/yuan/client/render/LiquidGlassRenderer.java");
        String model = read("src/main/resources/assets/yuan/models/item/yuan_sword.json");
        String bewlr = read("src/main/java/com/yuan/client/render/YuanSwordBEWLR.java");
        String tooltip = read("src/main/java/com/yuan/client/render/YuanSwordTooltipRenderer.java");
        String rounded = read("src/main/java/com/yuan/client/render/YuanRoundedRectRenderer.java");
        String cosmicShaders = read("src/main/java/com/yuan/client/cosmic/YuanCosmicShaders.java");
        String cosmicBaked = read("src/main/java/com/yuan/client/cosmic/YuanCosmicBakedModel.java");
        String godSwordConfig = read("src/main/java/com/yuan/item/YuanGodSwordConfig.java");
        String compose = read("src/main/kotlin/com/yuan/client/gui/YuanComposeTestScreen.kt");
        String tunnelFragment = read("src/main/resources/assets/yuan/shaders/core/tunnel_item.fsh");
        String voronoiFragment = read("src/main/resources/assets/yuan/shaders/core/voronoi_item.fsh");
        String ringParticle = read("src/main/resources/assets/yuan/particles/gilded_ring.json");
        String sparkParticle = read("src/main/resources/assets/yuan/particles/gilded_spark.json");

        assert !client.contains("onRegisterShaders") : "duplicate shader registration";
        assert shaders.contains("registerShaders(RegisterShadersEvent event)");
        assert !shaders.contains("cosmic_ring") && !shaders.contains("configureCosmicRing")
                : "removed cosmic ring shader is still registered";
        assert !renderTypes.contains("cosmicRing") && !tooltip.contains("220.0f, 200.0f")
                : "removed cosmic inner ring is still rendered";
        assert shaderConfig.contains("\"UV0\"") && shaderConfig.contains("\"UV2\"") : "BLOCK attributes incomplete";
        assert vertex.contains("in vec2 UV0;") && vertex.contains("in ivec2 UV2;") : "BLOCK vertex inputs incomplete";
        assert !fragment.contains("worldPos") : "fragment input has no vertex output";
        assert vertex.contains("out vec3 vertexPos;") && fragment.contains("in vec3 vertexPos;")
                : "slime shader vertex position interface is incomplete";
        assert !fragment.contains("FogStart") && !fragment.contains("FogEnd")
                : "GUI ring must not use world fog";
        assert fragment.contains("MAX_STEPS") && fragment.contains("mapSlime")
                && fragment.contains("iridescentColor") : "Fox Blade slime raymarch is incomplete";
        assert liquidConfig.contains("\"samplers\"") : "liquid glass samplers are not declared";
        assert !liquidRenderer.contains("NativeImage") && !liquidRenderer.contains("glReadPixels")
                && !liquidRenderer.contains("gaussianBlur")
                : "liquid glass must not use CPU framebuffer readback or blur";
        assert liquidRenderer.contains("TextureTarget") && liquidRenderer.contains("glBlitFramebuffer")
                : "liquid glass GPU scene copy is missing";
        assert liquidRenderer.contains("finally") && liquidRenderer.contains("GL_READ_FRAMEBUFFER_BINDING")
                && liquidRenderer.contains("GL_DRAW_FRAMEBUFFER_BINDING")
                : "liquid glass framebuffer state restoration is missing";
        assert cosmicShaders.contains("tunnel_item") && cosmicShaders.contains("setupTunnelUniforms")
                : "tunnel shader registration is missing";
        assert renderTypes.contains("tunnelRenderType")
                && cosmicBaked.contains("renderStyle == 4")
                && cosmicBaked.contains("renderTunnel")
                : "tunnel render style is not wired into the sword";
        assert godSwordConfig.contains("tunnelSpeed") && godSwordConfig.contains("tunnelBrightness")
                && godSwordConfig.contains("tunnelDensity")
                && godSwordConfig.contains("tunnelFov")
                && godSwordConfig.contains("Math.min(5, tag.getInt(\"renderStyle\"))")
                : "tunnel config fields or renderStyle 5 clamp are missing";
        assert compose.contains("\"隧道\"") && compose.contains("tunnelSpeed")
                && compose.contains("resetTunnelDefaults")
                && compose.contains("renderStyleUiValues")
                : "tunnel UI is not wired into the Compose screen";
        assert tunnelFragment.contains("ifsBox")
                && tunnelFragment.contains("for (int i = 0; i < 99; i++)")
                && tunnelFragment.contains("TunnelFov")
                && tunnelFragment.contains("ViewYaw")
                && tunnelFragment.contains("ViewPitch")
                : "tunnel raymarch shader is incomplete";
        assert cosmicShaders.contains("voronoi_item") && cosmicShaders.contains("setupVoronoiUniforms")
                : "voronoi shader registration is missing";
        assert renderTypes.contains("voronoiRenderType")
                && cosmicBaked.contains("renderStyle == 5")
                && cosmicBaked.contains("renderVoronoi")
                : "voronoi render style is not wired into the sword";
        assert godSwordConfig.contains("voronoiColor0") && godSwordConfig.contains("voronoiColorGap")
                && godSwordConfig.contains("voronoiDistortion") && godSwordConfig.contains("voronoiScale")
                && godSwordConfig.contains("voronoiFov")
                && godSwordConfig.contains("voronoiSpeed")
                : "voronoi config fields are missing";
        assert compose.contains("\"晶格\"") && compose.contains("resetVoronoiDefaults")
                && compose.contains("voronoiColorGlow") && compose.contains("voronoiPresets")
                && compose.contains("voronoiFov")
                : "voronoi UI or presets are not wired into the Compose screen";
        assert voronoiFragment.contains("voronoi(")
                && voronoiFragment.contains("u_noiseTexture")
                && voronoiFragment.contains("VoronoiDistortion")
                && voronoiFragment.contains("VoronoiGap")
                && voronoiFragment.contains("VoronoiGlow")
                && voronoiFragment.contains("VoronoiFov")
                && voronoiFragment.contains("VoronoiColorCount")
                : "voronoi shader is incomplete";
        assert Files.exists(Path.of("src/main/resources/assets/yuan/textures/effect/voronoi_noise.png"))
                : "voronoi noise texture is missing";
        assert tooltip.contains("ClientPlayerNetworkEvent.LoggingOut")
                && tooltip.contains("liquidGlassRenderer.close()")
                : "liquid glass render targets are not released on logout";
        assert liquidConfig.contains("\"SceneTex\"") && liquidConfig.contains("\"BlurTex\"")
                : "liquid glass optical samplers are incomplete";
        assert liquidFragment.contains("sdgBox") && liquidFragment.contains("refThickness")
                && liquidFragment.contains("refDisp") && liquidFragment.contains("glareRange")
                : "ReGlass rounded-rectangle optical model is incomplete";
        assert liquidFragment.contains("smoothstep(-0.75, 0.75, merged)")
                : "ReGlass exterior shadow mask is inverted";
        assert !liquidFragment.contains("capsuleRadius") && !liquidFragment.contains("caustic")
                && !liquidConfig.contains("\"velocity\"") && !liquidConfig.contains("\"spring\"")
                : "removed capsule motion model is still present";
        assert !liquidFragment.contains("fbm") && !liquidFragment.contains("droplet")
                && !liquidFragment.contains("bubble")
                : "Apple liquid glass must not use procedural frost or droplets";
        assert model.contains("\"parent\": \"item/handheld\"") : "sword must use vanilla handheld model dispatch";
        assert bewlr.contains("getModel(BASE_MODEL)") : "BEWLR must use the baked base model";
        assert !bewlr.contains("getItemRenderer().getModel") : "BEWLR recursive model lookup";
        assert bewlr.contains("isModelTransformContext(context)") : "BEWLR must isolate model transforms";
        assert bewlr.contains("translate(0.5, 0.5, 0.5)") : "BEWLR must undo outer centering";
        assert bewlr.contains("getTransforms().getTransform(context).apply") : "handheld transform missing";
        assert bewlr.contains("translate(-0.5, -0.5, -0.5)") : "BEWLR must restore centering";
        assert bewlr.contains("context == ItemDisplayContext.GROUND") : "ground model transform must be included";
        assert bewlr.contains("0xF000F0") : "GUI render must be full bright";
        assert !bewlr.contains("poseStack.scale(0.35F") : "ground size must not be hard-coded";
        assert !tooltip.contains("bufferSource().endBatch();") : "tooltip flushes unrelated buffers";
        assert count(tooltip, "230.0f, 190.0f, 0.9f, -35.0f, 64") == 2
                : "rainbow ring must have back and front halves";
        int background = tooltip.indexOf("renderFoxBackground(graphics");
        int glass = tooltip.indexOf("renderLiquidGlassBackground(graphics");
        int rainbowFront = tooltip.lastIndexOf("230.0f, 190.0f, 0.9f, -35.0f, 64");
        assert background < glass && glass < rainbowFront
                : "background, panel, and rainbow front order is incorrect";
        assert tooltip.contains("BG_WIDTH = 580.0f") && tooltip.contains("BG_HEIGHT = 320.0f")
                : "Yuan background size must remain unchanged";
        assert tooltip.contains("centerX - BG_WIDTH / 2 + 5")
                : "Yuan background must remain centered on the rings";
        assert !tooltip.contains("glBindTexture") && !tooltip.contains("activeTexture")
                : "raw OpenGL texture binding bypasses RenderSystem";
        assert !rounded.contains("bufferSource().endBatch();") : "rounded rect flushes unrelated buffers";
        assert ringParticle.contains("\"yuan:gilded_ring\"") : "ring sprite path includes particle twice";
        assert sparkParticle.contains("\"yuan:gilded_spark\"") : "spark sprite path includes particle twice";

        String ccUniform = read("src/main/java/com/yuan/client/cosmic/CCUniform.java");
        assert ccUniform.contains("this.cache = values.clone()")
                : "uniform cache must copy the caller array, otherwise in-place UV edits never re-upload";
        assert !ccUniform.contains("this.cache = values;")
                : "uniform cache still aliases the caller array";
        assert cosmicBaked.contains("renderQuadList(pose, consumer, maskQuads(maskSprites)")
                && cosmicBaked.contains("MASK_QUAD_CACHE")
                : "mask geometry must be baked once and reused per atlas sprite identity";
        String irisProbe = read("src/main/java/com/yuan/client/cosmic/YuanIris.java");
        assert irisProbe.contains("MethodHandle") && !irisProbe.contains("printStackTrace")
                : "Iris shader-pack probe must cache its reflection and stop dumping stacks per frame";

        String deferred = read("src/main/java/com/yuan/client/cosmic/YuanDeferredRender.java");
        assert !deferred.contains("AFTER_LEVEL && YuanIris")
                : "the pending item queue must drain even when the pack was toggled off mid-frame";
        assert cosmicBaked.contains("List<PendingRender> pending = new ArrayList<>(PENDING_RENDERS);")
                : "the pending queue must be swapped out and cleared before it is drained";
        String snapshot = read("src/main/java/com/yuan/client/cosmic/YuanRenderStateSnapshot.java");
        assert snapshot.contains("if (modelViewPushed)")
                : "cleanup must not pop a model view pose that restore never pushed";
        String itemMixin = read("src/main/java/com/yuan/mixin/ItemRendererMixin.java");
        assert itemMixin.contains("} finally {") && itemMixin.contains("pose.popPose();")
                : "the taken-over item render must always balance its PoseStack push";

        Path mask = Path.of("src/main/resources/assets/yuan/textures/item/yuan_god_sword_mask.png");
        assert Files.exists(mask) : "black hole sword mask texture missing";
        var maskImage = ImageIO.read(mask.toFile());
        assert maskImage.getWidth() == 32 && maskImage.getHeight() == 32 : "cosmic sword mask must be 32x32";
        int maskAlphaCount = 0;
        for (int x = 0; x < maskImage.getWidth(); x++) {
            for (int y = 0; y < maskImage.getHeight(); y++) {
                int alpha = (maskImage.getRGB(x, y) >>> 24) & 0xFF;
                if (alpha > 128) {
                    maskAlphaCount++;
                }
            }
        }
        assert maskAlphaCount >= 50
                : "god sword mask alpha silhouette must contain at least 50 pixels, got " + maskAlphaCount;

        String godSwordItem = read("src/main/java/com/yuan/item/YuanGodSwordItem.java");
        String itemsJava = read("src/main/java/com/yuan/registry/YuanItems.java");
        assert godSwordItem.contains("initializeClient") && godSwordItem.contains("IClientItemExtensions")
            : "god sword item must register client extensions";
        assert itemsJava.contains("new YuanGodSwordItem(")
            : "yuan_god_sword must use YuanGodSwordItem";

        try (var textures = Files.list(Path.of("src/main/resources/assets/yuan/textures/item"))) {
            for (Path texture : textures.filter(path -> path.toString().endsWith(".png")).toList()) {
                var image = ImageIO.read(texture.toFile());
                assert image.getWidth() >= 16 && image.getHeight() >= 16
                        : texture.getFileName() + " disables atlas mipmaps";
            }
        }

    }

    private static String read(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    private static int count(String value, String needle) {
        return (value.length() - value.replace(needle, "").length()) / needle.length();
    }

}
