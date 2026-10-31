#version 150

// Aurora liquid glass, vertex stage.
//
// One quad per panel, in NORMALISED SCREEN SPACE. Unlike the backdrop blur -- whose triangle is
// emitted in clip space, covering the viewport exactly once -- this pass draws inside an arbitrary
// rectangle, so the vertex stage forwards both the screen UV and the quad's own 0..1 coordinates.
// The fragment stage needs the latter to know where it sits inside the panel, which is what shapes
// the rounded corners and the rim.
//
// Position arrives already in -1..1 clip space and UV0 in 0..1 across the quad, both built by the
// Java side from the panel's rectangle. ProjMat and ModelViewMat stay identity no-ops, as in the
// blur pass.

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 screenUv;
out vec2 quadUv;
/** The quad's size in screen pixels, so the fragment stage can work in pixels rather than in a
 *  0..1 space whose aspect ratio changes with the panel. */
flat out vec2 quadSizePx;

uniform vec2 QuadSizePx;

void main() {
    screenUv = UV0;
    quadUv = UV0;
    quadSizePx = QuadSizePx;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
