package com.yuan;

import com.yuan.domain.render.YuanDomainFloorMesh;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards the star-abyss floor (batch 20).
 *
 * <p>Mixes source assertions with a few direct calls into the mesh builder. The source assertions
 * pin the contracts that a later edit could silently break -- packet field order, uniform wiring, the
 * camera-relative convention -- and the direct calls pin the numeric behaviour that has no other
 * witness.
 */
public final class YuanDomainCheck {

    public static void main(String[] args) throws Exception {
        String packet = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/YuanDomainStatePacket.java"));
        String client = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/YuanDomainClient.java"));
        String server = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/YuanDomainServerState.java"));
        String request = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/YuanDomainRequestPacket.java"));
        String shaders = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainShaders.java"));
        String render = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainRender.java"));
        String terrain = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainTerrain.java"));
        String mesh = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainFloorMesh.java"));
        String types = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainRenderTypes.java"));
        String vsh = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield.vsh"));
        String fsh = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield.fsh"));
        String json = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield.json"));
        String sections = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/gui/V9Sections.kt"));
        String chrome = Files.readString(Path.of(
                "src/main/kotlin/com/yuan/client/gui/V9Chrome.kt"));

        // ---- the domain must be reachable from the G panel -----------------------
        // It was originally nested under the time-stop page's "领域" sub-tab, which put a
        // separate feature behind an unrelated one. It is a top-level entry now.
        assert chrome.contains("\"领域\"") && chrome.contains("listOf(\"时停\", \"领域\"")
                : "the domain must be a top-level sidebar entry in the G panel";
        assert sections.contains("\"领域\" to setOf(\"领域\")")
                : "the domain needs its own nav section";
        assert !sections.contains("\"时停\" to setOf(\"基础\", \"动画\", \"画面\", \"音效\", \"光球\", \"时长\", \"冻结\", \"领域\", \"预设\")")
                : "the domain section must not also be listed under the time-stop page";
        assert !sections.contains("listOf(\"基础\", \"效果\", \"音效\", \"冻结\", \"领域\", \"预设\")")
                : "the domain must not remain a time-stop sub-tab";

        // ---- the star-field flag must reach other clients ------------------------
        // The toggle lives on the holder's sword and a client cannot read another player's sword, so
        // the packet is the only way it can travel. Dropping it makes every remote floor fall back to
        // a guess.
        assert packet.contains("starfield") && packet.contains("writeBoolean(message.starfield)")
                : "domain state packet must carry the star-field toggle for other clients";
        assert packet.contains("readBoolean()")
                : "domain state packet must read the star-field toggle back";
        assert client.contains("starfieldOf") && client.contains("starfield")
                : "domain client cache must keep the star-field flag per holder";
        assert server.contains("boolean starfield")
                : "server broadcast must carry the star-field setting";
        assert request.contains("config.domainStarfield")
                : "domain request must read the toggle from the holder's sword";

        // ---- the floor follows the terrain --------------------------------------
        assert terrain.contains("getHeight") && terrain.contains("MOTION_BLOCKING_NO_LEAVES")
                : "the floor must sample real terrain height, not sit on a fixed plane";
        assert terrain.contains("heightAt") && !terrain.contains("CACHE_TTL_MS")
                : "terrain sampling stays uncached; a window smaller than the max radius is a second, "
                        + "inconsistent code path";
        assert mesh.contains("YuanDomainTerrain.heightAt")
                : "the mesh must take vertex heights from the terrain sampler";
        // The floor must sit ABOVE the sampled surface. getHeight returns the Y of the first air
        // block, so a floor at or below it is buried in the ground and LEQUAL depth testing hides it
        // entirely -- which is exactly what shipped in the first attempt.
        assert mesh.contains("+ LIFT") && !mesh.contains("- 0.02f")
                : "the floor must be lifted above the terrain, not sunk into it";
        assert mesh.contains("LIFT = 0.05f")
                : "the lift constant must stay positive";

        // ---- render-side conventions --------------------------------------------
        assert render.contains("RenderLevelStageEvent") && render.contains("AFTER_PARTICLES")
                : "the floor must draw at AFTER_PARTICLES like the other world-space effects";
        assert render.contains("getMainCamera().getPosition()")
                : "the floor must be positioned relative to the camera";
        assert render.contains("findHolder") && render.contains("level.players()")
                : "holder position must be resolved from the local player list; Level.getPlayerByUUID "
                        + "is defined in terms of the server's player list and is wrong on a client";
        assert !render.contains("starfieldShader.apply()")
                : "the render path must NOT call apply() itself: it uploads to whatever program is "
                        + "bound at that moment and clears CCUniform's dirty flag, after which the real "
                        + "apply() skips the upload and the shader keeps its json defaults";
        assert types.contains("TRANSLUCENT_TRANSPARENCY") && types.contains("LEQUAL_DEPTH_TEST")
                : "the floor needs a real alpha blend and a depth test; an additive layer cannot darken";
        assert types.contains("NO_CULL")
                : "the terrain-following mesh folds on steep ground, so culling must be off";

        // ---- shader wiring ------------------------------------------------------
        assert shaders.contains("onApply")
                : "uniforms must be written from onApply, or a later apply overwrites them with the json defaults";
        assert shaders.contains("advanceClock") && shaders.contains("elapsedSeconds")
                : "the shader clock must accumulate rather than read an absolute clock, which a float cannot hold";
        assert !shaders.contains("currentTimeMillis() / 1000")
                : "an absolute-seconds float quantises to ~128s steps and stalls the animation";
        assert fsh.contains("discard") && fsh.contains("domainRadius")
                : "the fragment stage must fade the field out at the real radius";
        assert vsh.contains("UV0") && fsh.contains("worldXZ")
                : "the mesh must pass the centre-relative offset through UV0";
        assert !json.contains("domainCenter")
                : "the centre is carried per-vertex, so a centre uniform would be a second source of truth";
        assert json.contains("domain_starfield") && json.contains("\"Position\"")
                : "shader json must name both stages and declare the vertex attributes";
        // The json defaults are the values the shader runs with whenever the uniform push does not
        // land. A default of 0 for domainIntensity discards every fragment and the floor silently
        // disappears -- with the geometry still submitted and nothing in the log. The defaults must
        // therefore be a usable image on their own.
        assert json.contains("\"name\": \"domainIntensity\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ]")
                : "domainIntensity must default to 1.0, not 0.0: a zero default discards every "
                        + "fragment if the uniform push ever fails, and the failure is invisible";

        // ---- numeric behaviour of the mesh builder ------------------------------
        // The grid step must grow with the radius, or a large domain explodes the vertex count.
        double smallStep = YuanDomainFloorMesh.stepFor(20.0f);
        double largeStep = YuanDomainFloorMesh.stepFor(128.0f);
        assert largeStep > smallStep
                : "grid step must grow with the radius (20 -> " + smallStep + ", 128 -> " + largeStep + ")";
        assert smallStep >= 1.0
                : "grid step must never go below one block, got " + smallStep;

        // The vertex budget must stay bounded no matter how far the player pushes the slider.
        for (float r : new float[]{4.0f, 20.0f, 64.0f, 128.0f}) {
            double step = YuanDomainFloorMesh.stepFor(r);
            int cells = (int) Math.ceil(r / step);
            int quads = (cells * 2) * (cells * 2);
            assert quads <= 40_000
                    : "radius " + r + " would emit " + quads + " quads, past the budget";
        }

        // Worst-case vertices at the largest radius must fit the render type's buffer, or the mesh
        // flushes repeatedly mid-draw.
        double maxStep = YuanDomainFloorMesh.stepFor(128.0f);
        int maxSpan = (int) Math.ceil(128.0 / maxStep) + 1;
        int maxQuads = (maxSpan * 2) * (maxSpan * 2);
        int maxVerts = maxQuads * YuanDomainFloorMesh.vertsPerQuad();
        assert types.contains("65536")
                : "starfield buffer must be sized above the worst-case vertex count (" + maxVerts + ")";
        assert maxVerts < 65536
                : "worst-case vertex count " + maxVerts + " exceeds the declared buffer";

        assert YuanDomainFloorMesh.vertsPerQuad() == 6
                : "each quad is two triangles, so six vertices";

        System.out.println("YuanDomainCheck OK");
    }
}
