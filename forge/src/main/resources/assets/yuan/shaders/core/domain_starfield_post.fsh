#version 150

// Star-abyss floor, as a full-screen post-process.
//
// Why a post-process rather than a mesh. The floor has to cover the whole visible world, and a mesh
// cannot: the floor is a grid, its per-axis cell count is capped to bound the vertex budget, so
// covering 512 blocks pushes the step to ~11 blocks and the floor stops hugging the terrain. A
// post-process has no such limit -- it runs once per screen pixel and reads the depth buffer, so
// its accuracy is the depth buffer's accuracy at any distance, and its cost is the same whether the
// world is 50 blocks across or 500.
//
// The technique is depth reconstruction. For each pixel the depth buffer gives a non-linear depth
// in 0..1; that plus the pixel's NDC position is a clip-space point, and InverseViewProjection takes
// it back to a world position. Everything below is then computed in real world space, which is what
// keeps the star field anchored to the world instead of swimming with the camera.
//
// The ground is REPLACED, not covered. Upward-facing surfaces are rewritten and everything else --
// sky, cliffs, trunks, entities -- is left exactly as the level renderer drew it. That is the
// difference between "the world's surface has become the abyss" and "a starfield has been laid over
// the screen".

uniform sampler2D Sampler0;          // scene depth, bound from the main render target

uniform mat4 InverseViewProjection;  // NDC -> camera-relative world
uniform vec3 CameraWorldPos;         // the camera's world position, to anchor the field
uniform vec3 DomainCenter;           // caster position in world space

uniform float time;
uniform float domainSeed;
uniform float domainIntensity;  // 0..1 master fade, driven by the open/close animation
uniform vec4 domainColorBase;   // abyss body under the stars
uniform vec4 domainColorStar;   // star tint
uniform vec4 domainColorCore;   // glow around the caster
uniform vec4 ColorModulator;

in vec2 screenUv;

out vec4 fragColor;

const float CELL = 1.0;          // one star cell per block
const float STAR_RADIUS = 0.13;  // star half-size within its cell
const float TWINKLE_HZ = 0.35;

// Radians per second. The field turns rigidly about the world origin, so a point R blocks out
// travels R * SPIN blocks per second and the horizon is the fastest part of the image. SPIN scales
// inversely with the domain radius R: SPIN = 1.28 / R. The radius slider was removed (fixed at
// R = 128), so the value is pinned at 1.28 / 128 = 0.01 -- ~2 blocks per second at 200 blocks out,
// visible motion underfoot, and not a lurch at the horizon.
// The value that reads well in a small circle is nausea once the floor reaches the fog.
const float SPIN = 1.28 / 128.0;

// Distance over which the glow around the caster falls off, in blocks. This is the only thing left
// that has a size at all; the floor itself extends to the horizon.
const float CORE_RADIUS = 48.0;

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

void main() {
    float depth = texture(Sampler0, screenUv).r;

    // ---- reconstruct the world position this pixel shows ----------------------
    // Evaluated unconditionally, never behind a branch: the reconstruction feeds every pixel of the
    // frame, and an early return on the sky would leave the neighbouring ground pixels
    // underivable.
    vec4 clip = vec4(screenUv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 view = InverseViewProjection * clip;
    // max() rather than a plain divide: a pixel on the far plane has w == 0, and the huge-but-finite
    // result it produces is masked out below. A bare divide would be a NaN, and a NaN in a
    // derivative blanks the whole quad it lands in.
    vec3 relPos = view.xyz / max(view.w, 1e-4);

    // ---- anchor to the world --------------------------------------------------
    // The reconstruction is camera-relative, so relPos shifts by the camera's own translation every
    // frame. The star grid is indexed by absolute world XZ: hashing on the camera-relative position
    // instead would move the whole grid with the player, which reads as the field sliding and
    // stuttering underfoot -- exactly what the previous version did. DomainCenter is the caster's
    // world position for the same reason: the glow has to follow the caster, not the camera.
    vec3 worldPos = relPos + CameraWorldPos;

    // One mask: anything the level renderer actually drew is inside the domain. The earlier version
    // kept only upward-facing surfaces, judged from screen-space derivatives of the reconstructed
    // position, and that failed in three visible ways at once -- entities are vertical so their
    // normal pointed sideways and they were never taken, the derivative degenerates wherever the
    // depth buffer quantises (the whole far half of the frame) leaving patchy holes, and the cloud
    // layer, being horizontal, was taken while the cliffs beside it were not. Dropping the normal
    // test also drops the screen-space derivative cost entirely.
    float sky = step(depth, 0.99999);
    float mask = sky * domainIntensity;

    // ---- the star field, anchored to the world --------------------------------
    // The grid is indexed by ABSOLUTE world XZ. An earlier version subtracted the caster here "to
    // keep float precision near the origin", but subtracting any position that itself moves
    // translates the entire cell array by that movement, and the field slides underfoot -- which is
    // exactly what it did. Absolute coordinates are fine: a block stays representable to about
    // 2^24, far past anywhere a player can walk.
    vec2 grid = worldPos.xz;

    // Rigid rotation about the world origin -- about a FIXED point, not the caster. A rotation
    // about a moving centre is a rotation plus a translation, and the translation is the slide;
    // about a fixed one the pattern only turns. Locally the two are indistinguishable, since the
    // field is infinite and has no visible pivot. Rotation rather than an outward drift: drifting
    // re-samples each cell's contents as it travels, so stars pop in and out at cell boundaries,
    // while a rigid rotation moves the pattern continuously with no seams.
    float angle = time * SPIN;
    float ca = cos(angle);
    float sa = sin(angle);
    vec2 samplePos = vec2(
        grid.x * ca - grid.y * sa,
        grid.x * sa + grid.y * ca);

    vec2 cell = floor(samplePos / CELL);
    vec2 local = fract(samplePos / CELL);

    // One star per cell at most, placed in the cell's interior rather than anywhere in [0,1). A
    // star at the very edge of its cell would be clipped by the cell border; keeping it inside
    // (0.16..0.84, plus the 0.13 radius, stays within 0.03..0.97) means a single tap is enough and
    // no star is ever cut in half.
    vec2 o = 0.16 + hash22(cell + domainSeed) * 0.68;
    float present = hash21(cell * 1.37 + domainSeed);

    float star = 0.0;
    if (present > 0.74) {          // ~26% of cells hold a star
        float d = length(local - o);
        float core = 1.0 - smoothstep(STAR_RADIUS * 0.3, STAR_RADIUS, d);
        float phase = hash11(dot(cell, vec2(12.9898, 78.233)) + domainSeed);
        float tw = 0.65 + 0.35 * sin(time * TWINKLE_HZ * 6.2831 + phase * 6.2831);
        star = core * tw;
    }

    // ---- abyss body -----------------------------------------------------------
    vec3 col = domainColorBase.rgb;

    // A glow around the caster, so the domain reads as sourced from them rather than as a global
    // filter. This is the one thing that still has a size of its own, and the one place the caster's
    // position belongs: the glow follows the caster, while the star grid does not. Deliberately a
    // separate variable from the grid -- collapsing the two back into one is how the field ended up
    // sliding with the player last time.
    vec2 fromCaster = worldPos.xz - DomainCenter.xz;
    float core = pow(clamp(1.0 - length(fromCaster) / CORE_RADIUS, 0.0, 1.0), 2.2);
    col += domainColorCore.rgb * core * domainColorCore.a;

    col += domainColorStar.rgb * star * domainColorStar.a;

    // The scene colour is NOT sampled. The composite is done by alpha blending against whatever the
    // level renderer left in the target, which avoids reading the framebuffer that is currently
    // being written -- a feedback loop whose result is undefined -- and with it any need to blit the
    // scene into a second target first. alpha 1 replaces the ground outright; the ramp keeps the
    // mask edge from aliasing into a hard circle.
    float alpha = clamp(domainColorBase.a + star * 0.9, 0.0, 1.0) * mask;

    fragColor = vec4(col * ColorModulator.rgb, alpha);
}
