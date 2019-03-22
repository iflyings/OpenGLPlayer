#extension GL_OES_EGL_image_external : require
precision mediump float;

uniform samplerExternalOES sTexture;
uniform int uTransType;
uniform vec4 uTransData;
uniform int uDrawType;
uniform vec4 uDrawData;

varying highp vec2 vTextureCoordinate;

