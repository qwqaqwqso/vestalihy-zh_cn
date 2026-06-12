#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

uniform float Time;

float rand(vec2 co) {
    return fract(sin(dot(co + Time, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    // "Зашакаливание" — пиксельная размытость
    float pixelScale = 2.5;
    vec2 pixCoord = floor(texCoord * (InSize / pixelScale)) / (InSize / pixelScale);
    
    // Blur: усредняем 3x3 соседних пикселей вокруг пиксельной координаты
    vec2 texelSize = pixelScale / InSize;
    vec3 blurred = vec3(0.0);
    for (float x = -1.0; x <= 1.0; x++) {
        for (float y = -1.0; y <= 1.0; y++) {
            blurred += texture(DiffuseSampler, pixCoord + vec2(x, y) * texelSize).rgb;
        }
    }
    blurred /= 9.0;
    
    // Calculate basic luminance
    float luma = dot(blurred, vec3(0.2126, 0.7152, 0.0722));
    
    // Shifted luma for background to keep it dark but visible
    float backgroundLuma = pow(luma, 0.6) * 0.25;
    vec3 baseColor = vec3(backgroundLuma);
    
    // Detect "hot" objects (Entities at light level 15)
    // We use a steep step to make them pop as solid white
    float heat = smoothstep(0.55, 0.75, luma);
    
    // Combine base and heat (entities become white)
    vec3 finalColor = mix(baseColor, vec3(1.0), heat);
    
    // Add light dynamic noise (animated film grain)
    float noise = (rand(pixCoord) - 0.5) * 0.03;
    finalColor += noise;
    
    // Scanlines effect (old TV)
    float scanline = sin(texCoord.y * InSize.y * 1.5) * 0.1 + 0.9;
    finalColor *= scanline;
    
    // Vignette for more "optics" feel
    float dist = distance(texCoord, vec2(0.5));
    finalColor *= smoothstep(0.8, 0.4, dist);
    
    fragColor = vec4(finalColor, 1.0);
}
