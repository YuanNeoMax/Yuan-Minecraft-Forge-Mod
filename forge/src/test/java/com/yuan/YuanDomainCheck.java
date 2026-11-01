package com.yuan;

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
        String render = stripComments(Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainRender.java")));
        // Stripped for the same reason as the render source: this file explains why depth testing is
        // off by naming it, and the negative assertion below would match the explanation.
        String state = stripComments(Files.readString(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainRenderState.java")));
        String vsh = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield_post.vsh"));
        String fsh = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield_post.fsh"));
        String json = Files.readString(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield_post.json"));
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

        // ---- render-side conventions --------------------------------------------
        // AFTER_LEVEL, not AFTER_PARTICLES. The pass reads the depth buffer and needs the level
        // fully drawn, and it must run before the hand and the HUD -- both of which happen later in
        // GameRenderer. AFTER_PARTICLES is inside LevelRenderer, where the level is still being
        // built, and would sample an incomplete depth buffer.
        assert render.contains("RenderLevelStageEvent") && render.contains("Stage.AFTER_LEVEL")
                : "the pass must draw at AFTER_LEVEL: the level is complete there and the hand and "
                        + "HUD are not drawn yet";
        assert !render.contains("Stage.AFTER_PARTICLES")
                : "AFTER_PARTICLES is inside LevelRenderer, where the depth buffer is incomplete";
        assert render.contains("getMainCamera()") && render.contains(".getPosition()")
                : "the domain centre must be resolved relative to the camera";
        assert render.contains("findHolder") && render.contains("level.players()")
                : "holder position must be resolved from the local player list; Level.getPlayerByUUID "
                        + "is defined in terms of the server's player list and is wrong on a client";

        // ---- the depth-reconstruction contract ------------------------------------
        // This is the whole technique: the pixel's NDC position plus its depth, mapped back through
        // the inverse of the view-projection the terrain was drawn with. Get any of these wrong and
        // the field lands in the wrong place, which is indistinguishable from "nothing happened".
        assert render.contains("getDepthTextureId()")
                : "the pass reconstructs from the depth attachment; without it there is no world "
                        + "position to compute a star field in";
        // ---- the view-projection must be built from the camera, and only from the camera ----
        //
        // Both convenient sources were tried on screen and both are wrong at AFTER_LEVEL, and the
        // logged values say why rather than leaving it to inference:
        //
        //   RenderSystem.getModelViewMatrix()   logged as the identity. The model-view is reset once
        //                                       the level renderer finishes, so multiplying by it
        //                                       collapses the pair to the bare projection.
        //   event.getPoseStack().last().pose()  logged bit-identical to event.getProjectionMatrix(),
        //                                       because GameRenderer seeds that stack with the
        //                                       projection. Multiplying by it computes P x P.
        //
        // Both left |normal.y| near zero, so no ground was ever detected. The working construction is
        // the camera's own basis through lookAt, which maps forward onto -Z as the projection expects.
        // These assertions exist so neither shortcut can be reintroduced.
        assert !render.contains("RenderSystem.getModelViewMatrix()")
                : "RenderSystem's model-view is the identity at AFTER_LEVEL (verified by logging it); "
                        + "multiplying by it drops the view rotation entirely";
        assert !render.contains("getPoseStack()")
                : "the event's pose stack carries the projection matrix at AFTER_LEVEL (verified: "
                        + "bit-identical to getProjectionMatrix), so `P x pose` is `P x P`";
        assert render.contains("getLookVector()") && render.contains("getUpVector()")
                && render.contains("lookAt(")
                : "the view must be reconstructed from the camera's look and up vectors via lookAt";
        assert render.contains("VIEW_PROJECTION.set(RenderSystem.getProjectionMatrix())")
                : "the projection still comes from RenderSystem; only the view is rebuilt";
        // ---- THE SHADER'S STRUCTURE, because it only compiles at runtime ------------------
        //
        // A GLSL error is not a compile error in this build: the shader is compiled when the game
        // loads it, and a failure there turns the whole floor into a silent no-op. The build can
        // still check the three things that break it most often, all of which are cheap here and
        // expensive to discover in-game.
        {
            String body = stripShaderComments(fsh);

            // 1. EVERY DECLARED UNIFORM MUST BE READ. GLSL 150 drops a uniform that is declared but
            //    never used, and the Java lookup then returns null and the pass quietly skips.
            java.util.regex.Matcher decls = java.util.regex.Pattern
                    .compile("uniform\\s+\\w+\\s+(\\w+);")
                    .matcher(body);
            java.util.List<String> unread = new java.util.ArrayList<>();
            while (decls.find()) {
                String name = decls.group(1);
                String withoutDecl = body.replaceFirst("uniform\\s+\\w+\\s+" + name + ";", "");
                java.util.regex.Matcher uses = java.util.regex.Pattern
                        .compile("\\b" + name + "\\b").matcher(withoutDecl);
                if (!uses.find()) unread.add(name);
            }
            assert unread.isEmpty()
                    : "these uniforms are declared but never read, so GLSL 150 drops them and the "
                    + "Java lookup returns null: " + String.join(", ", unread);

            // 2. NO FUNCTION MAY BE CALLED BEFORE IT IS DEFINED. GLSL 150 has no forward declaration,
            //    so a helper placed below its first use fails to compile -- and the natural place to
            //    add a helper is right after the code that wanted it.
            java.util.regex.Matcher defs = java.util.regex.Pattern
                    .compile("(?m)^\\s*(?:float|vec2|vec3|vec4|void|int)\\s+(\\w+)\\s*\\(")
                    .matcher(body);
            java.util.Map<String, Integer> definedAt = new java.util.LinkedHashMap<>();
            while (defs.find()) definedAt.put(defs.group(1), defs.start());
            for (java.util.Map.Entry<String, Integer> e : definedAt.entrySet()) {
                int firstCall = body.indexOf(e.getKey() + "(");
                assert firstCall < 0 || firstCall >= e.getValue()
                        : e.getKey() + " is called before it is defined, which GLSL 150 rejects";
            }

            // 3. BRACES BALANCE. Trivial, and it caught a real fault last time this file was edited.
            int open = 0;
            int close = 0;
            for (char c : body.toCharArray()) {
                if (c == '{') open++;
                else if (c == '}') close++;
            }
            assert open == close : "unbalanced braces in the domain shader: " + open + " open, "
                    + close + " close";

            // 4. THE COLOUR TEMPERATURE IS STILL WIRED, because it is the layer easiest to lose in a
            //    refactor: it is a multiplier on the star contribution, so removing it compiles fine
            //    and simply reverts the field to flat white.
            assert body.contains("starTint(")
                    : "each star must carry its own colour temperature";
            assert body.contains("starColour")
                    : "the temperature must actually reach the composited colour";
        }

        assert !fsh.contains("InverseViewProjectionAlt")
                : "the diagnostic's second candidate uniform must not survive into the real shader";
        assert render.contains("invert()")
                : "the view-projection must be inverted; the pass maps pixels back to world space";
        assert render.contains("setShaderTexture(0, depthCopy.getDepthTextureId())")
                : "the depth attachment must be bound to sampler 0, which is what the fragment stage "
                        + "declares";
        // Sampling a texture that is attached to the framebuffer being drawn into is undefined
        // behaviour, and at this point the main target is both. The depth is blitted into a private
        // target first, and that copy is what gets sampled.
        assert render.contains("copyDepthFrom(")
                : "the depth must be copied out of the main target before it is sampled; reading the "
                        + "attachment of the framebuffer being written is undefined";
        // copyDepthFrom and resize both end by binding framebuffer 0 -- the screen, not the target
        // the frame is being composed into. Without the re-bind, this pass and everything the level
        // renderer draws after it bypass the post-processing chain.
        assert render.contains("bindWrite(true)")
                : "the main target must be re-bound after the depth blit, or the rest of the frame "
                        + "renders to the default framebuffer";
        assert render.contains("TextureTarget(")
                : "RenderTarget is abstract in 1.20.1; the copy needs a concrete TextureTarget";
        // ---- the field must be anchored to the world, not to the caster ----
        //
        // The star grid is indexed by `worldPos.xz - DomainCenter.xz`. Both operands must be absolute
        // world coordinates for the grid to stay still: the first version passed both camera-relative,
        // so the camera position cancelled and the grid was indexed by (world - caster) -- which
        // translates with the caster, and read as the whole field sliding and stuttering underfoot.
        assert render.contains("player.getX()") && render.contains("CAMERA_WORLD_POS.set((float) camera.x")
                : "both the caster and the camera must be passed as absolute world positions";
        assert !render.contains("player.getX() - camera.x")
                : "a camera-relative centre is subtracted twice in the shader and slides the field "
                        + "with the player";
        assert fsh.contains("CameraWorldPos") && fsh.contains("worldPos = relPos + CameraWorldPos")
                : "the reconstruction is camera-relative; only adding the camera's world position "
                        + "yields a world-anchored grid";
        // ---- the grid and the glow use different coordinates, deliberately ----
        //
        // The glow follows the caster, so it is indexed by worldXZ - casterXZ. The grid must NOT be:
        // subtracting any position that itself moves translates the whole cell array by that
        // movement. The first version shared one `radial` for both, which is exactly the slide the
        // user reported -- the grid has to be indexed by absolute world XZ and rotated about a fixed
        // point, because a rotation about a moving centre is a rotation plus a translation.
        assert fsh.contains("vec2 grid = worldPos.xz;")
                : "the star grid must be indexed by absolute world XZ, not by a caster-relative "
                        + "offset, or the whole field slides with the player";
        assert fsh.contains("vec2 fromCaster = worldPos.xz - DomainCenter.xz;")
                : "the glow follows the caster, so it alone is indexed by the caster-relative offset";
        assert fsh.contains("grid.x * ca - grid.y * sa")
                : "the rotation must be applied to the world-anchored grid, not to the "
                        + "caster-relative offset";
        assert !fsh.contains("radial")
                : "the shared `radial` variable is what let the grid and the glow collapse into one "
                        + "coordinate space; if it returns, so does the slide";
        // ---- full coverage -------------------------------------------------------
        //
        // The mask used to keep only upward-facing surfaces, judged from screen-space derivatives of
        // the reconstructed position. That rejected every entity outright (their normals point
        // sideways), left patchy holes wherever the depth buffer quantises, and took the horizontal
        // cloud layer while rejecting the cliffs beside it. The mask is now simply "anything the
        // level renderer drew", which is also what the user asked for: the domain is the whole world.
        assert !fsh.contains("dFdx") && !fsh.contains("dFdy")
                : "the derivative-based ground test is gone: it rejected entities outright and left "
                        + "patchy holes where the depth buffer quantises";
        assert fsh.contains("float mask = sky * domainIntensity;")
                : "coverage must be everything the level renderer drew, not just upward-facing faces";
        // The vertex stage emits clip-space positions, so both matrices must be identity. Passing
        // RenderSystem's matrices would transform an already-clip-space triangle a second time.
        assert render.contains("drawWithShader(IDENTITY, IDENTITY")
                : "the full-screen triangle is already in clip space; both matrices must be identity";

        // ---- the submission path -------------------------------------------------
        // The pass is one full-screen triangle: three vertices, no geometry to rebuild, no terrain
        // sampling, no per-axis cell cap. These assertions exist so a later edit cannot quietly
        // reintroduce the mesh -- whose step had to grow with the radius, which is exactly why the
        // floor stopped hugging the ground once the world got large.
        assert !render.contains("getBuffer(")
                : "the pass must not go through MultiBufferSource.getBuffer";
        assert !render.contains("endBatch(")
                : "the pass must not go through MultiBufferSource.endBatch";
        assert !render.contains("YuanDomainRenderTypes")
                : "the pass must not depend on the removed custom RenderType";
        assert !render.contains("YuanDomainFloorMesh")
                : "the mesh builder is gone: a capped grid cannot cover the render distance, and its "
                        + "step grows with the radius until the floor cuts through terrain";
        assert !render.contains("YuanDomainTerrain")
                : "terrain sampling is gone: the depth buffer already carries the surface";
        assert render.contains("RenderSystem.setShader")
                : "the submission must select the program explicitly";
        assert render.contains("new VertexBuffer(") && render.contains("VertexBuffer.Usage.DYNAMIC")
                : "the pass needs its own GPU buffer";
        assert render.contains("upload(") && render.contains("drawWithShader(")
                : "the triangle must be uploaded to the buffer and drawn with the program";
        assert !render.contains("starfieldShader.apply()")
                : "the render path must NOT call apply() itself: it uploads to whatever program is "
                        + "bound at that moment and clears CCUniform's dirty flag, after which the real "
                        + "apply() skips the upload and the shader keeps its json defaults";

        // ---- GL state ------------------------------------------------------------
        // Written out explicitly next to the draw that needs it, and undone afterwards -- otherwise
        // the hand and the HUD render with blending or depth writes left in the wrong state.
        assert state.contains("enableBlend") && state.contains("defaultBlendFunc")
                : "the pass composites over the ground, so it needs a real alpha blend";
        assert state.contains("disableCull")
                : "the full-screen triangle's winding depends on the projection's handedness, so "
                        + "culling must be off";
        assert state.contains("disableDepthTest")
                : "the pass IS the ground: testing it against the depth buffer it was reconstructed "
                        + "from compares it against itself";
        assert state.contains("depthMask(false)")
                : "the pass must not write depth, or it would occlude the sky and everything drawn "
                        + "after it";
        assert render.contains("YuanDomainRenderState.apply()")
                && render.contains("YuanDomainRenderState.restore()")
                : "the GL state must be both applied and restored around the draw";
        assert render.contains("finally")
                : "the restore must run even if the draw throws, or the frame is left in a broken "
                        + "blend/depth state";
        // Both are created lazily from the render-side draw path. A VertexBuffer constructor issues
        // GL calls and asserts it is on the render thread, so it must not be created anywhere else.
        assert !render.contains("close()")
                : "reset() must not close the GPU buffer: logout is a network-side event, and a "
                        + "VertexBuffer close issues GL deletes that belong on the render thread";
        assert render.contains("private static BufferBuilder builder()")
                && render.contains("private static VertexBuffer triangleBuffer()")
                : "the scratch builder and the GPU buffer must be created lazily from the draw path, "
                        + "which is the render thread";
        // end() must still be called on every path, or the builder stays in its building state and
        // the next begin() throws "Already building!". The result is null-checked because
        // BufferBuilder offers two end variants and only one of them is guaranteed non-null.
        assert render.contains("RenderedBuffer rendered") && render.contains("if (rendered == null)")
                : "the builder's end() result must be null-checked before use";

        // ---- shader wiring ------------------------------------------------------
        assert shaders.contains("onApply")
                : "uniforms must be written from onApply, or a later apply overwrites them with the json defaults";
        assert shaders.contains("advanceClock") && shaders.contains("elapsedSeconds")
                : "the shader clock must accumulate rather than read an absolute clock, which a float cannot hold";
        assert !shaders.contains("currentTimeMillis() / 1000")
                : "an absolute-seconds float quantises to ~128s steps and stalls the animation";
        // There is no radius uniform any more: the field covers the whole visible world, so a
        // cut-off would only be a smaller version of the effect. What still has a size of its own is
        // the glow around the caster, and that constant lives in the fragment stage.
        assert !fsh.contains("domainRadius")
                : "the field is unbounded now; a radius uniform would reintroduce the circle that "
                        + "made the domain look like a patch around the player";
        assert !json.contains("domainRadius")
                : "the json must not declare a radius uniform the shader no longer reads";
        assert fsh.contains("InverseViewProjection") && fsh.contains("screenUv")
                : "the fragment stage must reconstruct from the depth buffer through the inverse "
                        + "view-projection";
        assert vsh.contains("UV0") && fsh.contains("DomainCenter")
                : "the triangle's UV0 carries the screen coordinate and the centre arrives as a "
                        + "camera-relative uniform";
        assert json.contains("domain_starfield_post") && json.contains("\"Position\"")
                : "shader json must name both stages and declare the vertex attributes";
        assert json.contains("\"name\": \"Sampler0\"")
                : "the json must declare the depth sampler the fragment stage reads";
        // The json defaults are the values the shader runs with whenever the uniform push does not
        // land. A default of 0 for domainIntensity discards every fragment and the floor silently
        // disappears -- with the geometry still submitted and nothing in the log. The defaults must
        // therefore be a usable image on their own.
        // Matched against the json with its whitespace collapsed: the assertion is about the value,
        // not about how the file happens to be indented.
        String jsonFlat = json.replaceAll("\\s+", " ");
        assert jsonFlat.contains("\"name\": \"domainIntensity\", \"type\": \"float\", \"count\": 1, \"values\": [ 1.0 ]")
                : "domainIntensity must default to 1.0, not 0.0: a zero default discards every "
                        + "fragment if the uniform push ever fails, and the failure is invisible";

        // ---- the terrain sampler and the mesh builder are gone -------------------
        // Both existed to make a grid follow the ground, and both are obsolete: the pass reads the
        // depth buffer, which already carries the exact surface at any distance. Keeping them would
        // leave two sources of truth for "where is the ground" -- and the mesh one is the one that
        // could not cover the render distance.
        assert !Files.exists(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainTerrain.java"))
                : "the terrain sampler is obsolete: the depth buffer carries the surface";
        assert !Files.exists(Path.of(
                "src/main/java/com/yuan/domain/render/YuanDomainFloorMesh.java"))
                : "the mesh builder is obsolete: a capped grid cannot cover the render distance";
        // The old pair would still be loaded by the shader registry if it were left on disk, and a
        // stale shader that nothing draws is a compile error waiting to happen on someone else's GPU.
        assert !Files.exists(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield.fsh"))
                : "the mesh-era fragment stage must be removed, not left registered and unused";
        assert !Files.exists(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield.json"))
                : "the mesh-era shader json must be removed with its stages";
        assert !Files.exists(Path.of(
                "src/main/resources/assets/yuan/shaders/core/domain_starfield.vsh"))
                : "the mesh-era vertex stage must be removed with its siblings";

        System.out.println("YuanDomainCheck OK");
    }

    /**
     * Removes {@code //} line comments and {@code /* ... *}{@code /} block comments.
     *
     * <p>Source assertions are a cheap way to pin a contract, but they match text, and a comment that
     * explains why something is wrong reads exactly like the thing being wrong. Negative assertions
     * ("this must not appear") are the ones that break: the file that documents a fixed bug mentions
     * the very token that must never come back. Stripping comments first keeps the assertion about
     * the code.
     *
     * <p>Not a parser, and does not need to be. It does not track string literals, so a {@code //}
     * inside a string would be treated as a comment; no assertion here reads a string that contains
     * one.
     */
    /** Strips // and /* ... *\/ from a GLSL source, so a mention inside a comment is not a "use". */
    private static String stripShaderComments(String text) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = text.length();
        while (i < n) {
            if (text.startsWith("/*", i)) {
                int j = text.indexOf("*/", i + 2);
                i = j < 0 ? n : j + 2;
                continue;
            }
            if (text.startsWith("//", i)) {
                int j = text.indexOf('\n', i);
                i = j < 0 ? n : j;
                continue;
            }
            out.append(text.charAt(i));
            i++;
        }
        return out.toString();
    }

    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '/' && i + 1 < n && source.charAt(i + 1) == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < n && source.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < n && !(source.charAt(i) == '*' && source.charAt(i + 1) == '/')) {
                    i++;
                }
                i = Math.min(n, i + 2);
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }
}
