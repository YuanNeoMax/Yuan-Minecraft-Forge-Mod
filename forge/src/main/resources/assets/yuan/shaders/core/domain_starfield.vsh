#version 150

// Star-abyss floor: the terrain surface replaced by a star field.
//
// The floor is NOT a flat plane. Terrain height is sampled per vertex on the CPU and
// written into Position.y, so the rendered surface follows the ground -- that is what
// "the world's surface has been replaced" means here.
//
// Position is camera-relative (world minus camera), matching the AFTER_PARTICLES stage.
// UV0 carries the vertex's offset from the domain centre in blocks, so the fragment stage can
// place a stable world-space star field AND measure the radial distance in one value. Passing the
// offset rather than an absolute world position also keeps float precision usable far from the
// world origin.

in vec3 Position;
in vec4 Color;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 worldXZ;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    worldXZ = UV0;
    vertexColor = Color;
}
