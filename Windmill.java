/**
 * Windmill is a class that uses a vertex and fragment shader to create a windmill out of polygons
 * that can have different numbers for blades, the period of the blades, and the period of the cameras rotation entered at the command line.
 *
 * @author Noah Weatherton
 * @version 1.0
 */

import java.nio.*;
import javax.swing.*;
import java.lang.Math;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.*;
import com.jogamp.common.nio.Buffers;

import org.joml.*;

public class Windmill extends JFrame implements GLEventListener {
    // Constants
    private static final int WINDOW_WIDTH = 600, WINDOW_HEIGHT = 600;
    private static final String WINDOW_TITLE = "Windmill";
    private static final String VERTEX_SHADER_FILE = "windmill-vertex.glsl",
            FRAGMENT_SHADER_FILE = "windmill-fragment.glsl";
    private static final Vector3f FOCUS_POINT = new Vector3f(0.0f, 1.0f, 0.0f);
    private static final Vector3f STARTING_CAMERA_POSITION = new Vector3f(0.0f, 1.0f, 8.0f);


    // Window Fields
    private GLCanvas glCanvas;
    private int renderingProgram;
    private int[] vao = new int[1];
    private int[] vbo = new int[2];
    private float cameraX, cameraY, cameraZ;
    private float baseLocationX, baseLocationY, baseLocationZ;

    // Allocate variables for display() function
    private FloatBuffer matrixStorage = Buffers.newDirectFloatBuffer(16); //
    private Matrix4f perspectiveMat = new Matrix4f();
    private Matrix4f viewMat = new Matrix4f();
    private Matrix4f modelMat = new Matrix4f();
    private Matrix4f modelViewMat = new Matrix4f();
    private int modelViewLocation, perspectiveLocation;
    private float aspect;
    //private double timeFactor;
    private double startTime;
    private double elapsedTime;

    // Blade and camera command line settings
    private int numberOfBlades;
    private float bladePeriod;
    private float cameraPeriod;

    private float cameraAngle;
    private float bladeAngle;
    /**
     * Constructor for the containing window
     */
    public Windmill(int numberOfBlades, float bladePeriod, float cameraPeriod) {
        // Settings
        this.numberOfBlades = numberOfBlades;
        this.bladePeriod = bladePeriod;
        this.cameraPeriod = cameraPeriod;

        // Graphics
        setTitle(WINDOW_TITLE);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        // Tells GL about EventListener
        glCanvas = new GLCanvas();
        glCanvas.addGLEventListener(this);
        this.add(glCanvas);
        this.setVisible(true);
        // Starts animating!
        Animator animator = new Animator(glCanvas);
        animator.start();
    }

    /**
     * Draw one frame to the screen
     */
    @Override
    public void display(GLAutoDrawable drawable) {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        gl.glClear(GL_DEPTH_BUFFER_BIT); // Clear Z-Buffer
        gl.glClear(GL_COLOR_BUFFER_BIT); // Clear Screen

        gl.glUseProgram(renderingProgram);

        // Returns the locations for the uniform variables mv_matrix and p_matrix so they can be used as transforms
        modelViewLocation = gl.glGetUniformLocation(renderingProgram, "mv_matrix");
        perspectiveLocation = gl.glGetUniformLocation(renderingProgram, "p_matrix");

        aspect = (float) glCanvas.getWidth() / (float) glCanvas.getHeight();
        perspectiveMat.setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

        // Calculate time since start
        elapsedTime = System.currentTimeMillis() - startTime;

        // Calculate angles
        bladeAngle = (float) (elapsedTime * (2 * Math.PI / (bladePeriod * 1000)));
        cameraAngle = (float) (elapsedTime * (2 * Math.PI / (cameraPeriod * 1000)));

        // Update camera position
        float cameraX = (float) Math.sin(cameraAngle) * STARTING_CAMERA_POSITION.z;
        float cameraZ = (float) Math.cos(cameraAngle) * STARTING_CAMERA_POSITION.z;
        cameraY = STARTING_CAMERA_POSITION.y; // Keep the camera height constant

        // Create view matrix looking at the center of the blades
        viewMat.identity();
        viewMat.lookAt(cameraX, cameraY, cameraZ,
                FOCUS_POINT.x, FOCUS_POINT.y, FOCUS_POINT.z,
                0.0f, 1.0f, 0.0f); // Up vector is positive Y
        modelMat.identity();

        // Update the model view matrix
        modelViewMat.identity();
        modelViewMat.mul(viewMat);
        modelViewMat.mul(modelMat);

        // Send the matrices to the GPU
        gl.glUniformMatrix4fv(modelViewLocation, 1, false, modelViewMat.get(matrixStorage));
        gl.glUniformMatrix4fv(perspectiveLocation, 1, false, perspectiveMat.get(matrixStorage));

        // Getting ready to draw vertices in first VBO
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]); // Activate VBO
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0); // The buffer has 3 floats, and we are using the 0th Vertex Attribute
        gl.glEnableVertexAttribArray(0);

        gl.glEnable(GL_DEPTH_TEST);
        gl.glDepthFunc(GL_LEQUAL);

        gl.glDrawArrays(GL_TRIANGLES, 0, 36);

        // Send the matrices to the GPU



        // Draw the blades
        for (int i = 0; i < numberOfBlades; i++) {
            float angle = (float) (i * (2 * Math.PI / numberOfBlades) + bladeAngle);
            modelMat.identity();
            modelMat.translate(0.0f, 0.0f, 0.0f); // Move to the top of the windmill
            modelMat.rotate(angle, 0.0f, 0.0f, 1.0f); // Rotate around the Z-axis
            modelMat.translate(0.0f, 0.5f, 0.0f);

            modelViewMat.identity();
            modelViewMat.mul(viewMat);
            modelViewMat.mul(modelMat);

            gl.glUniformMatrix4fv(modelViewLocation, 1, false, modelViewMat.get(matrixStorage));
            gl.glUniformMatrix4fv(perspectiveLocation, 1, false, perspectiveMat.get(matrixStorage));

            // Getting ready to draw vertices in second VBO
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]); // Activate VBO
            gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0); // The buffer has 3 floats, and we are using the 0th Vertex Attribute
            gl.glEnableVertexAttribArray(0);

            // Make sure things are drawn in the correct order
            gl.glEnable(GL_DEPTH_TEST);
            gl.glDepthFunc(GL_LEQUAL);
            gl.glDrawArrays(GL_TRIANGLES, 0, 3); // Draw each blade
        }
    }

    // build my own rotation matrix rotate(x times time)

    /**
     * Do the intitial setup for drawing
     */

    @Override
    public void init(GLAutoDrawable drawable) {
        GL4 gl= (GL4) drawable.getGL();
        startTime = System.currentTimeMillis();
        renderingProgram = Utils.createShaderProgram(VERTEX_SHADER_FILE, FRAGMENT_SHADER_FILE);
        setupVertices();
        cameraX = 0.0f; cameraY = 0.0f; cameraZ = 8.0f;
        baseLocationX = 0.0f; baseLocationY = -2.0f; baseLocationZ = 0.0f;
    }

    private void setupVertices() {
        GL4 gl = (GL4) GLContext.getCurrentGL();
        float[] vertexPositions = {
                //x     y     z

                0.5f, -2.0f,  0.5f,
                0.5f,  1.5f,  0.5f,
                -0.5f,  1.5f,  0.5f,
                -0.5f, -2.0f,  0.5f,
                0.5f, -2.0f,  0.5f,
                -0.5f,  1.5f,  0.5f,

                0.5f, -2.0f, -0.5f,
                0.5f,  1.5f, -0.5f,
                0.5f,  1.5f,  0.5f,
                0.5f, -2.0f,  0.5f,
                0.5f, -2.0f, -0.5f,
                0.5f,  1.5f,  0.5f,

                -0.5f, -2.0f, -0.5f,
                0.5f, -2.0f, -0.5f,
                -0.5f,  1.5f, -0.5f,
                0.5f, -2.0f, -0.5f,
                -0.5f,  1.5f, -0.5f,
                0.5f,  1.5f, -0.5f,

                -0.5f, -2.0f,  0.5f,
                -0.5f,  1.5f,  0.5f,
                -0.5f,  1.5f, -0.5f,
                -0.5f, -2.0f, -0.5f,
                -0.5f, -2.0f,  0.5f,
                -0.5f,  1.5f, -0.5f,

                // Roof
                // x     y      z
                -0.5f, 1.5f, 0.5f,
                0.5f, 1.5f, 0.5f,
                0.0f, 2.0f, 0.0f,

                0.5f, 1.5f, -0.5f,
                -0.5f, 1.5f, -0.5f,
                0.0f, 2.0f, 0.0f,

                0.5f, 1.5f, 0.5f,
                0.5f, 1.5f, -0.5f,
                0.0f, 2.0f, 0.0f,

                -0.5f, 1.5f, -0.5f,
                -0.5f, 1.5f, 0.5f,
                0.0f, 2.0f, 0.0f,

        };

        float[] bladePositions = {
                //x    y     z
                0.0f, 1.0f, 0.5f,
                3.0f, 0.8f, 0.5f,
                3.0f, 1.2f, 0.5f



        };

        gl.glGenVertexArrays(vao.length, vao, 0); // Make first VAO
        gl.glBindVertexArray(vao[0]); // Activate first VAO
        gl.glGenBuffers(vbo.length, vbo, 0); // Make 2 VBOs

        // Transmit data into one of the VBOs
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertexBuffer = Buffers.newDirectFloatBuffer(vertexPositions); // Allows for efficient transfer of vertex data
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.limit() * 4, vertexBuffer, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer bladeBuffer = Buffers.newDirectFloatBuffer(bladePositions); // Allows for efficient transfer of vertex data
        gl.glBufferData(GL_ARRAY_BUFFER, vertexBuffer.limit() * 4, bladeBuffer, GL_STATIC_DRAW);

    }

    /**
     * main method to create the object
     */

    public static void main(String[] args) {
        int numberOfBlades;
        float bladePeriod;
        float cameraPeriod;

        if (args.length != 3) {
            System.err.println("A positive integer followed by 2 Real Numbers separated by a space is required.");
            System.exit(9); // Abnormal exit to stop from continuing
        } else {
            try {
                numberOfBlades = Integer.parseInt(args[0]);
                bladePeriod = Float.parseFloat(args[1]);
                cameraPeriod = Float.parseFloat(args[2]);

                new Windmill(numberOfBlades, bladePeriod, cameraPeriod);
            } catch (NumberFormatException exc) {
                System.err.println("A positive integer followed by 2 Real Numbers separated by a space is required.");
                System.exit(9);
            }
        }



    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        // THIS IS NECESSARY
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // Empty
    }


}
