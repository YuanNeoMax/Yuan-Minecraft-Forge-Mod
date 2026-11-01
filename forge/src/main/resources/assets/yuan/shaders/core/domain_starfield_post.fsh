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

// How far a ring travels before it dissolves, in blocks. Slightly beyond CORE_RADIUS so the rings
// leave the glow before they fade out -- a ring dying inside the lit area is invisible.
const float RING_MAX_RADIUS = 62.0;

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

/**
 * Smoothed value noise, 0..1.
 *
 * <p>Three octaves are enough for a slow, drifting cloud: the nebula sits UNDER the stars and is
 * meant to be noticed second, so it wants broad soft shapes rather than detail. Detail at this scale
 * would read as static once the field rotates.
 */
float nebula(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    float freq = 1.0;
    for (int i = 0; i < 3; i++) {
        vec2 cell = floor(p * freq);
        vec2 f = fract(p * freq);
        // Smoothstep interpolation rather than linear: linear leaves the lattice visible as
        // diamond-shaped creases between cells.
        vec2 w = f * f * (3.0 - 2.0 * f);
        float a = hash21(cell);
        float b = hash21(cell + vec2(1.0, 0.0));
        float c = hash21(cell + vec2(0.0, 1.0));
        float d = hash21(cell + vec2(1.0, 1.0));
        sum += amp * mix(mix(a, b, w.x), mix(c, d, w.x), w.y);
        amp *= 0.5;
        freq *= 2.03;
    }
    return sum;
}

/**
 * A four-point diffraction spike, 0..1, from the star's centred distance.
 *
 * <p>Real starlight through an aperture throws a cross, and it is the single detail that separates
 * "a dot in a grid" from "a star". The arms are thin and fall off fast (pow 6) so they read as
 * glints rather than as a glow, and the diagonal pair is deliberately absent -- six-point spikes at
 * this density would turn the floor into a carpet of fireworks.
 *
 * <p>[arm] is the distance projected onto one axis, [along] the distance along the other; a tap is
 * only sharp when its arm is near zero AND it is close to the star, which is exactly the cross shape.
 */
float starSpike(vec2 d, float reach) {
    float armX = abs(d.y);   // thin in y -> a horizontal arm
    float armY = abs(d.x);   // thin in x -> a vertical arm
    float lenX = abs(d.x);
    float lenY = abs(d.y);
    if (lenX > reach && lenY > reach) return 0.0;
    float hx = (lenY > reach) ? 0.0 : pow(clamp(1.0 - armX / (STAR_RADIUS * 0.42), 0.0, 1.0), 1.4)
             * pow(clamp(1.0 - lenX / reach, 0.0, 1.0), 3.0);
    float vy = (lenX > reach) ? 0.0 : pow(clamp(1.0 - armY / (STAR_RADIUS * 0.42), 0.0, 1.0), 1.4)
             * pow(clamp(1.0 - lenY / reach, 0.0, 1.0), 3.0);
    return clamp(hx + vy, 0.0, 1.0);
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
    float spike = 0.0;
    if (present > 0.74) {          // ~26% of cells hold a star
        vec2 dv = local - o;
        float d = length(dv);
        float core = 1.0 - smoothstep(STAR_RADIUS * 0.3, STAR_RADIUS, d);
        float phase = hash11(dot(cell, vec2(12.9898, 78.233)) + domainSeed);
        float tw = 0.65 + 0.35 * sin(time * TWINKLE_HZ * 6.2831 + phase * 6.2831);
        star = core * tw;

        // SPIKES ONLY ON THE BRIGHT ONES. Every star getting a cross would make the floor busy and
        // flatten the difference between an ordinary star and a bright one -- the cross is what
        // marks the few that are worth looking at. `present` doubles as the per-cell seed here, so
        // the same cells stay bright as the field turns rather than flickering between roles.
        if (present > 0.93) {
            // The spike belongs to the brighter half of the twinkle, so it blooms and fades with
            // its star instead of sitting at constant strength over a dimming core.
            spike = starSpike(dv, STAR_RADIUS * 6.5) * tw;
        }
    }

    // ---- abyss body -----------------------------------------------------------
    // A drifting nebula UNDER the star field: broad, soft, slow. It gives the floor the depth a
    // flat colour cannot -- the abyss is meant to look like something with volume, not a painted
    // plane -- while staying low-contrast enough that it never competes with the stars.
    //
    // Sampled in the SAME rotated space as the stars, so it turns with the field instead of
    // sliding underneath it. A different rate would read as two layers fighting.
    float cloud = nebula(samplePos * 0.045 + domainSeed);
    float cloud2 = nebula(samplePos * 0.012 - domainSeed * 0.5);
    vec3 col = domainColorBase.rgb
            + domainColorStar.rgb * (cloud * 0.055 + cloud2 * 0.035);

    // A glow around the caster, so the domain reads as sourced from them rather than as a global
    // filter. This is the one thing that still has a size of its own, and the one place the caster's
    // position belongs: the glow follows the caster, while the star grid does not. Deliberately a
    // separate variable from the grid -- collapsing the two back into one is how the field ended up
    // sliding with the player last time.
    vec2 fromCaster = worldPos.xz - DomainCenter.xz;
    float distFromCaster = length(fromCaster);
    float core = pow(clamp(1.0 - distFromCaster / CORE_RADIUS, 0.0, 1.0), 2.2);
    col += domainColorCore.rgb * core * domainColorCore.a;

    // ---- RINGS AROUND THE CASTER ------------------------------------------------------------
    // Three thin rings expanding outward, each fading as it travels. This is the one part of the
    // effect that MOVES toward the player rather than turning around them, and that is deliberate:
    // a rotating field with a static centre reads as a backdrop, while rings leaving the caster read
    // as something being emitted. It is the difference between "the ground is a star field" and
    // "THE CASTER IS DOING THIS".
    //
    // The rims are narrow (a tenth of the ring radius) because a wide band is a glow, and a glow at
    // this distance just brightens the middle of the screen. A thin ring is legible as a shape even
    // when the camera is close enough for the floor to fill the view.
    //
    // Phase is offset per ring so they never line up: three rings arriving together would pulse like
    // a strobe instead of expanding like a ripple.
    float ringGlow = 0.0;
    for (int i = 0; i < 3; i++) {
        float offset = float(i) * 0.3333;
        float travel = fract(time * 0.30 + offset);     // 0 at the caster, 1 at the rim
        float radius = RING_MAX_RADIUS * travel;
        float d = distFromCaster - radius;
        // Width grows slightly with travel: a ring keeps its apparent thickness as it expands, so it
        // does not thin into a hairline by the time it reaches the edge of the effect.
        float width = RING_MAX_RADIUS * (0.030 + 0.020 * travel);
        float band = pow(clamp(1.0 - abs(d) / width, 0.0, 1.0), 2.0);
        // Fade in over the first fifth (so rings do not pop into being at the caster's feet) and out
        // over the rest, so the outermost rings dissolve instead of vanishing at a hard radius.
        float life = smoothstep(0.0, 0.18, travel) * pow(1.0 - travel, 1.6);
        ringGlow += band * life;
    }
    col += domainColorCore.rgb * ringGlow * 1.35;

    col += domainColorStar.rgb * star * domainColorStar.a;
    // The spike is added separately and more strongly than the core: a glint is a highlight, and
    // blending it at the star's own weight would make it a second smudge instead of a line.
    col += domainColorStar.rgb * spike * 1.6;

    // The scene colour is NOT sampled. The composite is done by alpha blending against whatever the
    // level renderer left in the target, which avoids reading the framebuffer that is currently
    // being written -- a feedback loop whose result is undefined -- and with it any need to blit the
    // scene into a second target first. alpha 1 replaces the ground outright; the ramp keeps the
    // mask edge from aliasing into a hard circle.
    float alpha = clamp(domainColorBase.a + star * 0.9 + spike * 0.7 + ringGlow * 0.8, 0.0, 1.0) * mask;

    fragColor = vec4(col * ColorModulator.rgb, alpha);
}
