#version 150

// Aurora liquid glass, fragment stage.
//
// THE DIFFERENCE FROM THE BACKDROP BLUR, which is the whole reason this shader exists. The blur pass
// smears the entire screen by the same amount and then composites it back flat -- every pixel of the
// panel shows the same kind of frosted field. That reads as fog.
//
// A sheet of glass does two things fog does not:
//
//   1. It REFRACTS. The image behind it is displaced, and the displacement follows the panel's own
//      shape -- near the rim the surface turns away from the viewer, so the image is pulled outward
//      and the background is magnified along the edge. That is what makes a real pane look thick,
//      and it is why the offset here is a function of DISTANCE FROM THE EDGE rather than a constant.
//
//   2. It catches light at the rim. The bevel faces the viewer's light from a different angle than
//      the flat face does, so a bright hairline runs just inside the border and fades inward.
//
// Both are computed from a rounded-rectangle SDF, so the effect follows the panel's own corner
// radius instead of being stamped on as a rectangle.
//
// The source is the ALREADY-BLURRED backdrop (the blur pass runs first and writes the blurred field
// into the main target), so the glass refracts something soft rather than the raw world. Sampling
// the sharp world through a strong displacement would show its edges as doubled lines, which reads
// as a rendering fault rather than as glass.
//
// Every uniform declared here is genuinely read: GLSL 150 drops unread uniforms, the Java lookup
// then returns null, and the whole pass silently skips. See the same note in aurora_blur.fsh.

in vec2 screenUv;
in vec2 quadUv;
flat in vec2 quadSizePx;

uniform sampler2D Sampler0;
/** x: corner radius in pixels. y: refraction strength. z: rim highlight strength. w: unused. */
uniform vec4 Glass;
/** 1 / screen size in pixels: converts the refraction offset from pixels into the UV space the
 *  blurred backdrop is sampled in. */
uniform vec2 ScreenTexelSize;
uniform vec4 Tint;
uniform vec4 ColorModulator;

out vec4 fragColor;

/** Distance from the point to a rounded rectangle, negative inside. Standard SDF. */
float roundedBoxSdf(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

void main() {
    vec2 size = max(quadSizePx, vec2(1.0));
    vec2 halfSize = size * 0.5;
    float radius = min(Glass.x, min(halfSize.x, halfSize.y));

    // Work in pixels from the panel's centre: the SDF and every falloff below are in pixels, so the
    // rim stays the same thickness whatever size the panel is dragged to.
    vec2 p = (quadUv - 0.5) * size;

    float sdf = roundedBoxSdf(p, halfSize, radius);

    // Outside the rounded shape: discard, so the glass is the panel's shape rather than its bounding
    // box. A hard cut is correct here because the panel's own background is drawn beneath it with the
    // same radius -- this pass supplies the material, not the silhouette.
    if (sdf > 0.0) {
        discard;
    }

    // REFRACTION PROFILE. `edge` is 0 in the middle and 1 at the rim, over a band whose width scales
    // with the panel so a small panel does not end up entirely rim.
    float band = max(radius * 2.0, 24.0);
    float edge = clamp(-sdf / band, 0.0, 1.0);
    // pow() shapes the falloff: a linear ramp makes the whole panel look slightly warped, while a
    // curve keeps the middle optically flat and confines the bending to the edge, as glass does.
    float bend = pow(edge, 2.0);

    // Displace outward along the direction from the centre. Outward rather than inward because the
    // surface turns away from the viewer at the rim, which samples a WIDER part of the background --
    // the magnifying edge of a thick pane.
    vec2 dir = normalize(p + vec2(0.0001));
    vec2 offsetPx = dir * bend * Glass.y;

    // Convert the pixel offset to UV. The blurred source is the main target, whose UV space is the
    // whole screen, so the divisor is the SCREEN size, not the quad size.
    vec2 offsetUv = offsetPx * ScreenTexelSize;
    vec2 uv = clamp(screenUv + offsetUv, vec2(0.0), vec2(1.0));

    vec3 glass = texture(Sampler0, uv).rgb;

    // RIM HIGHLIGHT. A hairline just inside the edge, brightest exactly at the rim and gone by the
    // end of the band. Added rather than blended so it survives the panel tint being dark.
    float rim = pow(1.0 - edge, 6.0);
    glass += vec3(1.0) * rim * Glass.z;

    fragColor = vec4(glass, 1.0) * Tint * ColorModulator;
}
