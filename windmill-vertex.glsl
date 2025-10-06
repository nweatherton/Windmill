#version 430

layout (location=0) in vec3 position;

uniform mat4 mv_matrix;
uniform mat4 p_matrix;

out vec4 colors;



void main(void)
{
	gl_Position = p_matrix * mv_matrix * vec4(position, 1.0);
	float i = floor((gl_VertexID) / 6);// floor makes it so it is a solid color and not a gradient
	colors = vec4(i, 1, 3, 9) * 0.2;
}