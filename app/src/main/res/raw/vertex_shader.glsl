attribute vec4 aTextureCoordinate;
attribute vec4 aPosition;

uniform mat4 uTextureMatrix;
uniform mat4 uPositionMatrix;

varying highp vec2 vTextureCoordinate;

void main() {
    vTextureCoordinate = (uTextureMatrix * aTextureCoordinate).xy;
    gl_Position = aPosition * uPositionMatrix;
}