#version 150

// Star-abyss floor fragment stage.
//
// A cell-based star field in world space: the ground is divided into one-block cells, each hashing
// to at most one star with a stable position, size and twinkle phase.
//
// The whole field rotates rigidly about the domain centre. That was chosen over an outward drift:
// drifting means re-sampling each cell's contents as it travels, so stars pop in and out at cell
// boundaries. A rigid rotation moves the pattern continuously with no seams, and it reads as the
// abyss slowly turning underfoot.

uniform float time;
uniform float domainRadius;     // real cut-off radius, in blocks
uniform float domainFade;       // width of the soft rim, in blocks
uniform float domainSeed;
uniform float domainIntensity;  // 0..1 master fade, driven by the open/close animation
uniform vec4 domainColorBase;   // abyss body under the stars
uniform vec4 domainColorStar;   // star tint
uniform vec4 domainColorCore;   // glow at the domain origin
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 worldXZ;   // offset from the domain centre, in blocks

out vec4 fragColor;

const float CELL = 1.0;          // one star cell per block
const float STAR_RADIUS = 0.13;  // star half-size within its cell
const float SPIN = 0.035;        // radians per second
const float TWINKLE_HZ = 0.35;

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
    // worldXZ is the vertex's offset from the domain centre, so its length is the radial distance.
    // Deriving it here rather than interpolating keeps the rim a true circle on uneven ground.
    float radialDistance = length(worldXZ);

    // 1 inside the domain, 0 beyond the rim. smoothstep keeps the edge off a hard circle.
    float t = clamp((domainRadius - radialDistance) / max(domainFade, 0.001), 0.0, 1.0);
    float edge = t * t * (3.0 - 2.0 * t);

    float intensity = domainIntensity * vertexColor.a;
    if (edge * intensity <= 0.004) discard;

    // ---- rigid rotation ------------------------------------------------------
    // Rotating the sample position spins the whole field about the centre. Because the pattern is
    // sampled from a fixed grid, this is continuous: no cell is entered or left abruptly.
    float angle = time * SPIN;
    float ca = cos(angle);
    float sa = sin(angle);
    vec2 samplePos = vec2(
        worldXZ.x * ca - worldXZ.y * sa,
        worldXZ.x * sa + worldXZ.y * ca);

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

    // ---- abyss body ----------------------------------------------------------
    vec3 col = domainColorBase.rgb;

    // Glow at the origin, so the centre of the domain reads as its source.
    float core = pow(clamp(1.0 - radialDistance / max(domainRadius * 0.45, 0.001), 0.0, 1.0), 2.2);
    col += domainColorCore.rgb * core * domainColorCore.a;

    col += domainColorStar.rgb * star * domainColorStar.a;

    float alpha = clamp(domainColorBase.a + star * 0.9, 0.0, 1.0) * edge * intensity;

    fragColor = vec4(col * vertexColor.rgb, alpha) * ColorModulator;
}
