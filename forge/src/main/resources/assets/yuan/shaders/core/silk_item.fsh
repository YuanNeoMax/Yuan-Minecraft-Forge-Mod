#version 150

uniform float Time;
uniform vec4 SilkColor0;
uniform vec4 SilkColor1;
uniform vec4 SilkColor2;
uniform vec4 SilkColor3;
uniform float SilkBrightness;
uniform float SilkContrast;
uniform float SilkSaturation;
uniform float SilkScale;
uniform float SilkIntensity;
uniform float SilkWarp;
uniform float SilkDetail;
uniform float SilkHue;
uniform float SilkSeed;
uniform float SilkRotation;
uniform float SilkDrift;
uniform float SilkVignette;
uniform float SilkGrain;
uniform vec4 SpriteBounds;
uniform float ViewYaw;
uniform float ViewPitch;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(234.34, 435.345));
    p += dot(p, p + 34.23);
    return fract(p.x * p.y);
}

float grainHash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash21(i), hash21(i + vec2(1.0, 0.0)), u.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), u.x),
        u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * noise(p);
        p = p * 2.03 + vec2(17.0, 9.2);
        a *= 0.5;
    }
    return v;
}

vec3 hueRotate(vec3 col, float a) {
    const mat3 toYIQ = mat3(0.299, 0.596, 0.211,
                            0.587, -0.274, -0.523,
                            0.114, -0.322, 0.312);
    const mat3 toRGB = mat3(1.0, 1.0, 1.0,
                            0.956, -0.272, -1.106,
                            0.621, -0.647, 1.703);
    vec3 yiq = toYIQ * col;
    float ca = cos(a), sa = sin(a);
    yiq = vec3(yiq.x, yiq.y * ca - yiq.z * sa, yiq.y * sa + yiq.z * ca);
    return toRGB * yiq;
}

vec3 palette(float x) {
    float f = clamp(x, 0.0, 1.0) * 3.0;
    vec3 col = SilkColor0.rgb;
    if (f < 1.0) {
        col = mix(SilkColor0.rgb, SilkColor1.rgb, smoothstep(0.0, 1.0, f));
    } else if (f < 2.0) {
        col = mix(SilkColor1.rgb, SilkColor2.rgb, smoothstep(0.0, 1.0, f - 1.0));
    } else {
        col = mix(SilkColor2.rgb, SilkColor3.rgb, smoothstep(0.0, 1.0, f - 2.0));
    }
    return col;
}

vec3 shade(vec2 p, float t) {
    vec2 q = p * 1.6;
    float amp = 0.25 + SilkIntensity * 0.85;
    for (float i = 1.0; i < 5.0; i += 1.0) {
        q.x += amp / i * cos(i * 2.4 * q.y + t * 0.8 + SilkSeed);
        q.y += amp / i * cos(i * 1.7 * q.x + t * 0.6);
    }
    return palette(0.5 + 0.5 * sin(q.x + q.y));
}

void main() {
    vec2 uv = (texCoord0 - SpriteBounds.xy) / max(SpriteBounds.zw, vec2(0.0001));
    vec2 p = uv * 2.0 - 1.0;
    p *= SilkScale;
    if (abs(SilkRotation + ViewYaw) > 0.0001) {
        float total = SilkRotation + ViewYaw;
        float cr = cos(total), sr = sin(total);
        p = mat2(cr, -sr, sr, cr) * p;
    }
    if (abs(ViewPitch) > 0.0001) {
        p.y += ViewPitch * 0.8;
    }
    if (SilkDrift > 0.0001) {
        p += SilkDrift * vec2(sin(Time * 0.31), cos(Time * 0.23));
    }
    if (SilkWarp > 0.0) {
        p += SilkWarp * (vec2(
            fbm(p * SilkDetail + SilkSeed),
            fbm(p * SilkDetail + vec2(5.2, 1.3))) - 0.5);
    }

    vec3 col = shade(p, Time);
    if (abs(SilkContrast - 1.0) > 0.0001) {
        col = (col - 0.5) * SilkContrast + 0.5;
    }
    if (abs(SilkSaturation - 1.0) > 0.0001) {
        float luma = dot(col, vec3(0.299, 0.587, 0.114));
        col = mix(vec3(luma), col, SilkSaturation);
    }
    if (abs(SilkHue) > 0.0001) {
        col = hueRotate(col, SilkHue);
    }
    if (abs(SilkBrightness) > 0.0001) {
        col += SilkBrightness;
    }
    if (SilkVignette > 0.0001) {
        float vd = length(uv - 0.5) * 1.41421356;
        col *= 1.0 - SilkVignette * smoothstep(0.35, 1.0, vd);
    }
    if (SilkGrain > 0.0001) {
        col += (grainHash(uv * 512.0 + vec2(SilkSeed * 17.0, SilkSeed * 31.0)) - 0.5) * SilkGrain;
    }
    fragColor = vec4(clamp(col, 0.0, 1.0) * vertexColor.rgb, 1.0);
}
