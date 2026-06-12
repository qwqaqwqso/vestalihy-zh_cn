#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // Parameters to match Shad style while staying safe within bounds
    float ZOOM = 0.82; // Safe zoom in to avoid black gaps or sky repetition
    float DIST_STRENGTH = 0.18; // Pincushion distortion factor
    
    // Move coordinates to [-0.5, 0.5]
    vec2 p = texCoord - 0.5;
    
    // Aspect ratio correction to keep distortion circular
    float aspectRatio = InSize.x / InSize.y;
    p.x *= aspectRatio;
    
    // Apply zoom before distortion
    p *= ZOOM;
    
    float distanceSq = dot(p, p);
    
    // Fisheye/Lens distortion formula
    vec2 distortedP = p * (1.0 + DIST_STRENGTH * distanceSq);
    
    // Map back to UV space [0, 1]
    distortedP.x /= aspectRatio;
    vec2 uv = distortedP + 0.5;
    
    // Final check to prevent any out-of-bounds artifacts
    if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0) {
        fragColor = vec4(0.0, 0.0, 0.0, 1.0);
        return;
    }

    // Chromatic aberration at the edges
    float abAmount = 0.012 * length(distortedP);
    float r = texture(DiffuseSampler, uv + vec2(abAmount, 0.0)).r;
    float g = texture(DiffuseSampler, uv).g;
    float b = texture(DiffuseSampler, uv - vec2(abAmount, 0.0)).b;
    float a = texture(DiffuseSampler, uv).a;
    
    vec3 color = vec3(r, g, b);
    
    // Subtle vignette to focus the view
    float v = smoothstep(0.85, 0.5, length(distortedP));
    color *= mix(0.9, 1.0, v);
    
    fragColor = vec4(color, a);
}
