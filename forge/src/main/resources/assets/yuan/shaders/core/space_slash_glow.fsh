#version 150

uniform float Progress;
uniform vec4 GlowColor;
uniform float GlowStrength;
uniform float EdgeBrightness;
uniform float TipFade;
uniform float SweepSpeed;
uniform float SweepSoftness;
uniform float FadeStart;
uniform float FadeDuration;
uniform float HoldFraction;
uniform float EdgeWidth;
uniform float GlowWidth;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float t = texCoord0.x;
    float v = texCoord0.y;
    float d = abs(v - 0.5) * 2.0;

    float endFade = smoothstep(0.0, TipFade, t) * smoothstep(1.0, 1.0 - TipFade, t);
    float revealEnd = min(Progress / SweepSpeed, 1.0);
    float sweep = 1.0 - smoothstep(revealEnd - SweepSoftness, revealEnd, t);
    float fadeFrom = max(FadeStart, min(1.0, SweepSpeed + HoldFraction));
    float fadeOut = 1.0 - smoothstep(fadeFrom, fadeFrom + FadeDuration, Progress);

    float edge = smoothstep(EdgeWidth, min(1.0, EdgeWidth + 0.06), d)
            * (1.0 - smoothstep(0.94, 0.98, d));
    float glow = smoothstep(GlowWidth, 0.98, d) * (1.0 - smoothstep(0.99, 1.0, d));
    float strength = glow * GlowStrength + edge * 0.12 * EdgeBrightness;

    float alpha = vertexColor.a * endFade * sweep * fadeOut * strength;
    fragColor = vec4(GlowColor.rgb * strength, alpha);
}
