#version 300 es
precision mediump float;

uniform vec4 u_Color;

layout(location = 0) out vec4 o_FragColor;

void main() {
  o_FragColor = u_Color;
}
