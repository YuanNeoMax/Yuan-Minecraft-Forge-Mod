package com.yuan;

import com.yuan.domain.render.YuanDomainFloorMesh;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards the star-abyss floor (batch 20).
 *
 * <p>Mixes source assertions with a few direct calls into the mesh builder. The source assertions
 * pin the contracts that a later edit could silently break -- packet field order, uniform wiring, the
 * camera-relative convention, the submission path -- and the direct calls pin the numeric behaviour
 * that has no other witness.
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
        String state = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainRenderState.java"));
        String terrain = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainTerrain.java"));
        String mesh = Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainFloorMesh.java"));
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

        // ---- the submission path -------------------------------------------------
        // The first version wrapped the floor in a custom RenderType drawn through the shared buffer
        // source. Geometry was submitted and uniforms landed -- both confirmed in the log -- and
        // nothing reached the screen. Every other world-space renderer in this project that owns a
        // custom shader falls back to a vanilla shader when a shader pack is present; this was the
        // only one with no fallback and no precedent. The floor is now submitted directly: the
        // program is selected explicitly, the GL state is set explicitly, and the geometry goes
        // through a private BufferBuilder into a VertexBuffer that is drawn with the matrices and
        // program that are actually current. These assertions exist so a later edit cannot quietly
        // reintroduce the layer that did not work.
        assert !render.contains("getBuffer(")
                : "the floor must not go back through MultiBufferSource.getBuffer";
        assert !render.contains("endBatch(")
                : "the floor must not go back through MultiBufferSource.endBatch";
        assert !render.contains("YuanDomainRenderTypes")
                : "the floor must not depend on the removed custom RenderType";
        assert render.contains("RenderSystem.setShader")
                : "the submission must select the program explicitly";
        assert render.contains("getShader()")
                : "the submission must read back the selected program; drawing with a null program "
                        + "would submit geometry against whatever was bound";
        assert render.contains("new VertexBuffer(") && render.contains("VertexBuffer.Usage.DYNAMIC")
                : "the floor needs its own GPU buffer, re-uploaded each frame as it follows terrain";
        assert render.contains("upload(") && render.contains("drawWithShader(")
                : "the geometry must be uploaded to the buffer and drawn with the current matrices";
        assert render.contains("RenderSystem.getModelViewMatrix()")
                && render.contains("RenderSystem.getProjectionMatrix()")
                : "drawWithShader needs the model-view and projection matrices that are current; "
                        + "passing anything else draws the mesh somewhere off screen";
        assert !render.contains("starfieldShader.apply()")
                : "the render path must NOT call apply() itself: it uploads to whatever program is "
                        + "bound at that moment and clears CCUniform's dirty flag, after which the real "
                        + "apply() skips the upload and the shader keeps its json defaults";

        // ---- GL state ------------------------------------------------------------
        // The state shards used to arrange this. It is explicit now, next to the draw that needs it,
        // and it is undone afterwards -- otherwise the rest of the frame renders with blending or
        // depth writes left in the wrong state.
        assert state.contains("enableBlend") && state.contains("defaultBlendFunc")
                : "the floor needs a real alpha blend; an additive layer cannot darken anything";
        assert state.contains("disableCull")
                : "the terrain-following mesh folds on steep ground, so culling must be off";
        assert state.contains("depthMask(false)")
                : "the floor must not write depth, or it would occlude the sky and later passes";
        assert state.contains("depthFunc") && state.contains("GL_LEQUAL")
                : "the floor must be depth-tested against the terrain it lies on";
        assert render.contains("YuanDomainRenderState.apply()")
                && render.contains("YuanDomainRenderState.restore()")
                : "the GL state must be both applied and restored around the draw";
        assert render.contains("finally")
                : "the restore must run even if the draw throws, or the frame is left in a broken "
                        + "blend/depth state";

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

        // Worst-case vertices at the largest radius must fit the geometry buffer, or the builder
        // reallocates mid-mesh every frame.
        double maxStep = YuanDomainFloorMesh.stepFor(128.0f);
        int maxSpan = (int) Math.ceil(128.0 / maxStep) + 1;
        int maxQuads = (maxSpan * 2) * (maxSpan * 2);
        int maxVerts = maxQuads * YuanDomainFloorMesh.vertsPerQuad();
        assert YuanDomainFloorMesh.vertsPerQuad() == 6
                : "each quad is two triangles, so six vertices";
        // POSITION_COLOR_TEX is position (3 floats), colour (4 unsigned bytes) and UV0 (2 floats).
        int vertexBytes = 3 * 4 + 4 + 2 * 4;
        long worstCaseBytes = (long) maxVerts * vertexBytes;
        assert render.contains("1 << 21")
                : "the geometry buffer must stay declared as a named size, not a bare literal";
        assert worstCaseBytes < (1L << 21)
                : "worst-case geometry " + worstCaseBytes + " bytes at " + maxVerts
                        + " vertices exceeds the declared 2 MB buffer";

        System.out.println("YuanDomainCheck OK");
    }
}
