package com.archiaipro.threeD

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max

class Facade3DView(context: Context) : GLSurfaceView(context) {
    private val buildingRenderer = BuildingRenderer()

    init {
        setEGLContextClientVersion(2)
        setRenderer(buildingRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setSectionMaterial(section: Int, colorIndex: Int) {
        buildingRenderer.setMaterial(section, colorIndex)
        requestRender()
    }

    private var lastX = 0f
    private var lastY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { lastX = event.x; lastY = event.y; return true }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                val dy = event.y - lastY
                lastX = event.x
                lastY = event.y
                buildingRenderer.rotate(dx * 0.55f, dy * 0.45f)
                requestRender()
                return true
            }
        }
        return true
    }
}

private class BuildingRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var positionHandle = 0
    private var colorHandle = 0
    private var matrixHandle = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val vp = FloatArray(16)
    private val mvp = FloatArray(16)
    private var angleX = -12f
    private var angleY = -24f
    private val colors = IntArray(5)

    private val palette = arrayOf(
        floatArrayOf(0.88f, 0.85f, 0.78f),
        floatArrayOf(0.70f, 0.62f, 0.49f),
        floatArrayOf(0.48f, 0.52f, 0.56f),
        floatArrayOf(0.22f, 0.27f, 0.32f),
        floatArrayOf(0.82f, 0.75f, 0.62f)
    )

    fun setMaterial(section: Int, colorIndex: Int) {
        if (section in colors.indices) colors[section] = colorIndex.coerceIn(0, palette.lastIndex)
    }

    fun rotate(dx: Float, dy: Float) {
        angleY = (angleY + dx) % 360f
        angleX = (angleX + dy).coerceIn(-75f, 75f)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.91f, 0.93f, 0.95f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vertex = "attribute vec3 aPosition; attribute vec3 aColor; uniform mat4 uMvp; varying vec3 vColor; void main(){gl_Position=uMvp*vec4(aPosition,1.0);vColor=aColor;}"
        val fragment = "precision mediump float; varying vec3 vColor; void main(){gl_FragColor=vec4(vColor,1.0);}"
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vs); GLES20.glAttachShader(it, fs); GLES20.glLinkProgram(it)
        }
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { GLES20.glGetProgramInfoLog(program) }
        positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        colorHandle = GLES20.glGetAttribLocation(program, "aColor")
        matrixHandle = GLES20.glGetUniformLocation(program, "uMvp")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / max(1, height)
        Matrix.frustumM(projection, 0, -ratio, ratio, -1f, 1f, 3f, 45f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        Matrix.setLookAtM(view, 0, 0f, 5.2f, 18f, 0f, 0.4f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, angleX, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, angleY, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)

        drawBox(0f, 0f, 0f, 8f, 5.8f, 2.5f, palette[colors[0]])
        drawBox(-3.55f, 0f, 1.42f, 0.62f, 6.1f, 0.38f, palette[colors[1]])
        drawBox(3.55f, 0f, 1.42f, 0.62f, 6.1f, 0.38f, palette[colors[2]])
        drawBox(0f, 3.08f, 1.42f, 7.8f, 0.42f, 0.42f, palette[colors[3]])
        drawBox(0f, -2.92f, 1.42f, 7.8f, 0.42f, 0.42f, palette[colors[4]])
        val glass = floatArrayOf(0.22f, 0.42f, 0.54f)
        drawBox(-1.7f, 0.55f, 1.34f, 1.35f, 1.8f, 0.13f, glass)
        drawBox(1.7f, 0.55f, 1.34f, 1.35f, 1.8f, 0.13f, glass)
        drawBox(0f, -1.55f, 1.38f, 1.25f, 2.25f, 0.18f, floatArrayOf(0.20f,0.25f,0.31f))
        drawBox(0f, -3.25f, 0f, 9.2f, 0.25f, 3.3f, floatArrayOf(0.43f,0.45f,0.47f))
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        check(status[0] == GLES20.GL_TRUE) { GLES20.glGetShaderInfoLog(shader) }
        return shader
    }

    private fun drawBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, color: FloatArray) {
        val x0 = cx-sx/2; val x1 = cx+sx/2
        val y0 = cy-sy/2; val y1 = cy+sy/2
        val z0 = cz-sz/2; val z1 = cz+sz/2
        val faces = arrayOf(
            floatArrayOf(x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1),
            floatArrayOf(x1,y0,z0, x0,y0,z0, x0,y1,z0, x1,y1,z0),
            floatArrayOf(x0,y1,z1, x1,y1,z1, x1,y1,z0, x0,y1,z0),
            floatArrayOf(x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1),
            floatArrayOf(x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0),
            floatArrayOf(x1,y0,z1, x1,y0,z0, x1,y1,z0, x1,y1,z1)
        )
        val shade = floatArrayOf(1f, 0.80f, 1.12f, 0.74f, 0.87f, 0.91f)
        val data = FloatArray(6*6*6)
        var p = 0
        val order = intArrayOf(0,1,2,0,2,3)
        for (f in faces.indices) for (index in order) {
            val q = faces[f]
            data[p++] = q[index*3]; data[p++] = q[index*3+1]; data[p++] = q[index*3+2]
            val factor = shade[f]
            data[p++] = (color[0]*factor).coerceIn(0f,1f)
            data[p++] = (color[1]*factor).coerceIn(0f,1f)
            data[p++] = (color[2]*factor).coerceIn(0f,1f)
        }
        val buffer: FloatBuffer = ByteBuffer.allocateDirect(data.size*4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().put(data).also { it.position(0) }
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvp, 0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(3)
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 3, GLES20.GL_FLOAT, false, 24, buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36)
    }
}
