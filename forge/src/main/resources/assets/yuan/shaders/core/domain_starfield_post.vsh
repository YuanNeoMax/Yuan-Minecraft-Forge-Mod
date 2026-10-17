#version 150

// Star-abyss post-process, vertex stage.
//
// One full-screen triangle. Not a quad: three vertices cover the whole viewport with no seam down
// the diagonal and one fewer vertex to transform. The positions are emitted in clip space, so the
// model-view and projection matrices are set to identity before the draw and this stage is a pass
// through. They are still declared and still multiplied, because that is the form vanilla's
// shader loader expects and there is no reason to be clever about two identity multiplies.

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 screenUv;

void main() {
    screenUv = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
