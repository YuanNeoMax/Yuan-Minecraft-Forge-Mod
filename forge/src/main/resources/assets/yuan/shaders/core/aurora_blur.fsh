#version 150

// Aurora backdrop blur, fragment stage.
//
// A 5-tap (9-effective-tap) separable Gaussian along Dir. One pass direction per draw: the Java
// side runs horizontal and vertical passes through half-resolution ping-pong targets, then a final
// composite pass with Dir = (0, 0) -- with a zero step every tap lands on the same texel and the
// weights sum to 1, so the composite is an exact copy of the blurred source under Tint.
//
// Every declared uniform is genuinely read here. GLSL 150 drops uniforms that are declared but
// never read: the Java lookup then returns null, ready() goes false and the whole pass silently
// skips with nothing but a "could not find uniform" line in the log. The domain shader already
// paid for that lesson once.

in vec2 screenUv;

uniform sampler2D Sampler0;
uniform vec2 Dir;
uniform vec4 Tint;
uniform vec4 ColorModulator;

out vec4 fragColor;

void main() {
    vec2 texel = Dir;
    vec3 sum = texture(Sampler0, screenUv).rgb * 0.227027;
    sum += texture(Sampler0, screenUv + texel * 1.3846).rgb * 0.316216;
    sum += texture(Sampler0, screenUv - texel * 1.3846).rgb * 0.316216;
    sum += texture(Sampler0, screenUv + texel * 3.2308).rgb * 0.070270;
    sum += texture(Sampler0, screenUv - texel * 3.2308).rgb * 0.070270;
    fragColor = vec4(sum, 1.0) * Tint * ColorModulator;
}
