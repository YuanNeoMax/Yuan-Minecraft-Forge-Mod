#version 150

uniform sampler2D BlurTex;
uniform vec2 guiSize;
uniform vec2 framebufferSize;
uniform vec4 tooltipArea;
uniform float cornerRadius;
uniform vec4 tint;
uniform vec4 shadow;
uniform vec4 shadowColor;
uniform vec4 optics0;
uniform vec4 optics1;

in vec4 vertexColor;
in vec2 vertexPos;
out vec4 fragColor;

vec3 sdgBox(vec2 p, vec2 b, float radius) {
    vec2 w = abs(p) - (b - radius);
    vec2 s = vec2(p.x < 0.0 ? -1.0 : 1.0, p.y < 0.0 ? -1.0 : 1.0);
    float g = max(w.x, w.y);
    vec2 q = max(w, 0.0);
    float l = length(q);
    float dist = (g > 0.0) ? l - radius : g - radius;
    vec2 n = (g > 0.0) ? q / max(l, 1e-6) : ((w.x > w.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(dist, s * n);
}

vec2 sceneUV(vec2 guiPosition) {
    return vec2(guiPosition.x / guiSize.x, 1.0 - guiPosition.y / guiSize.y);
}

vec2 clampUV(vec2 uv) {
    vec2 halfTexel = 0.5 / framebufferSize;
    return clamp(uv, halfTexel, vec2(1.0) - halfTexel);
}

void main() {
    vec2 center = tooltipArea.xy + tooltipArea.zw * 0.5;
    vec2 halfSize = tooltipArea.zw * 0.5;
    vec2 p = vertexPos - center;
    vec3 field = sdgBox(p, halfSize, min(cornerRadius, min(halfSize.x, halfSize.y)));
    float merged = field.x;

    vec3 shadowField = sdgBox(p + shadow.zw, halfSize,
            min(cornerRadius, min(halfSize.x, halfSize.y)));
    float shadowAmount = exp(-abs(shadowField.x) / max(shadow.x, 1e-4)) * 0.6 * shadow.y;
    float outsideShadow = shadowAmount * shadowColor.a * smoothstep(-0.75, 0.75, merged);
    if (merged >= 0.0) {
        fragColor = vec4(shadowColor.rgb, outsideShadow);
        return;
    }

    vec2 normal = normalize(field.yz + vec2(1e-6));
    float nmerged = -merged;
    float refThickness = optics0.x;
    float refFactor = optics0.y;
    float refDisp = optics0.z;
    float refFresRange = optics0.w;
    float refFresHard = optics1.x / 100.0;
    float refFresFac = optics1.y / 100.0;
    float glareRange = optics1.z;
    float glareHard = optics1.w / 100.0;

    float xR = 1.0 - nmerged / max(refThickness, 1e-6);
    float thetaI = asin(pow(clamp(xR, 0.0, 1.0), 2.0));
    float thetaT = asin(clamp(sin(thetaI) / max(refFactor, 1e-6), -1.0, 1.0));
    float edgeFactor = -tan(thetaT - thetaI);
    if (nmerged >= refThickness) edgeFactor = 0.0;

    vec2 refrOffset = vec2(-normal.x, normal.y) * edgeFactor * 0.08
            * vec2(framebufferSize.y / framebufferSize.x, 1.0);
    vec2 uv = sceneUV(vertexPos);
    vec4 blurred = texture(BlurTex, clampUV(uv + refrOffset));
    const float NR = 0.985;
    const float NG = 1.000;
    const float NB = 1.015;
    vec3 dispersed = vec3(
        texture(BlurTex, clampUV(uv + refrOffset * (1.0 - (NR - 1.0) * refDisp))).r,
        texture(BlurTex, clampUV(uv + refrOffset * (1.0 - (NG - 1.0) * refDisp))).g,
        texture(BlurTex, clampUV(uv + refrOffset * (1.0 - (NB - 1.0) * refDisp))).b
    );

    vec3 outColor = mix(dispersed, tint.rgb, tint.a * 0.8);
    float fresnelFactor = clamp(pow(1.0 + merged / 1500.0
            * pow(500.0 / max(refFresRange, 1e-6), 2.0) + refFresHard, 5.0), 0.0, 1.0);
    vec3 fresTint = mix(vec3(1.0), tint.rgb, tint.a * 0.5);
    outColor = mix(outColor, fresTint, fresnelFactor * refFresFac * 0.7);

    float glareGeo = clamp(pow(1.0 + merged / 1500.0
            * pow(500.0 / max(glareRange, 1e-6), 2.0) + glareHard, 5.0), 0.0, 1.0);
    vec3 glareMix = mix(blurred.rgb, tint.rgb, tint.a * 0.5);
    outColor = mix(outColor, glareMix, 0.25 * glareGeo);

    float aa = max(fwidth(merged), 0.5);
    float alpha = 1.0 - smoothstep(-aa, aa, merged);
    fragColor = vec4(outColor, alpha) * vertexColor;
}
