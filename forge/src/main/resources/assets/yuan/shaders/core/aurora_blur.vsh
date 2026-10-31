#version 150

// Aurora backdrop blur, vertex stage.
//
// One full-screen triangle, positions emitted in clip space so the model-view and projection
// matrices are identity no-ops at draw time. Same shape as the domain star-field pass: three
// vertices cover the viewport with no diagonal seam, and UV0 interpolates to 0..1 across the
// screen so the fragment stage never needs a screen-size uniform.

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 screenUv;

void main() {
    screenUv = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
