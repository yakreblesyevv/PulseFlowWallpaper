package com.pulseflow.wallpaper

import android.graphics.*
import android.os.Build
import kotlin.math.cos
import kotlin.math.sin

class FlowRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val start = System.currentTimeMillis()
    private var runtimeShader: RuntimeShader? = null

    var speed = 1.0f
    var scale = 1.0f
    var brightness = 1.0f
    var blur = 0.72f
    var colors = intArrayOf(Color.rgb(80,40,190), Color.rgb(15,125,210), Color.rgb(220,45,130))

    private val shaderCode = """
        uniform float2 resolution;
        uniform float time;
        uniform float scale;
        uniform float brightness;
        uniform float softness;
        layout(color) uniform half4 colorA;
        layout(color) uniform half4 colorB;
        layout(color) uniform half4 colorC;

        float hash(float2 p) {
            return fract(sin(dot(p, float2(127.1,311.7))) * 43758.5453);
        }
        float noise(float2 p) {
            float2 i=floor(p); float2 f=fract(p);
            f=f*f*(3.0-2.0*f);
            return mix(mix(hash(i),hash(i+float2(1,0)),f.x),
                       mix(hash(i+float2(0,1)),hash(i+float2(1,1)),f.x),f.y);
        }
        float fbm(float2 p) {
            float v=0.0; float a=0.5;
            for (int i=0;i<4;i++) { v += a*noise(p); p=p*2.03+17.17; a*=0.5; }
            return v;
        }
        half4 main(float2 fragCoord) {
            float2 uv=(fragCoord-0.5*resolution)/min(resolution.x,resolution.y);
            uv /= max(scale,0.35);
            float t=time;
            float2 q=float2(fbm(uv*1.35+float2(t*0.055,-t*0.038)),
                            fbm(uv*1.35+float2(-t*0.044,t*0.052)+4.7));
            float2 r=float2(fbm(uv*1.55+q*2.35+float2(t*0.032,1.7)),
                            fbm(uv*1.55+q*2.10+float2(8.3,-t*0.029)));
            float field=fbm(uv*1.18+r*2.65+q*0.8);
            float wave=0.5+0.5*sin((uv.x*1.25+uv.y*0.82+r.x*1.9-r.y*1.5)*3.14159+t*0.16);
            float blend=smoothstep(0.12+softness*0.08,0.88-softness*0.08,field);
            half3 ab=mix(colorA.rgb,colorB.rgb,half(blend));
            half3 col=mix(ab,colorC.rgb,half(smoothstep(0.22,0.82,wave)*0.55));
            float shade=0.82+0.30*fbm(uv*0.75+r+t*0.012);
            col *= half(brightness*shade);
            return half4(col,1.0);
        }
    """.trimIndent()

    fun draw(canvas: Canvas) {
        if (Build.VERSION.SDK_INT >= 33 && canvas.isHardwareAccelerated) {
            drawGpu(canvas)
        } else {
            drawFallback(canvas)
        }
    }

    private fun drawGpu(canvas: Canvas) {
        val shader = runtimeShader ?: RuntimeShader(shaderCode).also { runtimeShader=it }
        val t=((System.currentTimeMillis()-start)/1000f)*speed
        shader.setFloatUniform("resolution",canvas.width.toFloat(),canvas.height.toFloat())
        shader.setFloatUniform("time",t)
        shader.setFloatUniform("scale",scale)
        shader.setFloatUniform("brightness",brightness)
        shader.setFloatUniform("softness",blur)
        shader.setColorUniform("colorA",colors[0])
        shader.setColorUniform("colorB",colors[1 % colors.size])
        shader.setColorUniform("colorC",colors[2 % colors.size])
        paint.shader=shader
        canvas.drawRect(0f,0f,canvas.width.toFloat(),canvas.height.toFloat(),paint)
        paint.shader=null
    }

    private fun drawFallback(canvas: Canvas) {
        val w=canvas.width.toFloat(); val h=canvas.height.toFloat(); canvas.drawColor(Color.rgb(3,4,9))
        val t=((System.currentTimeMillis()-start)/1000f)*speed; val base=maxOf(w,h)*scale
        for(i in 0 until 9){
            val raw=colors[i%colors.size]; val rr=(Color.red(raw)*brightness).toInt().coerceIn(0,255); val gg=(Color.green(raw)*brightness).toInt().coerceIn(0,255); val bb=(Color.blue(raw)*brightness).toInt().coerceIn(0,255)
            val p=t*(0.055f+i*0.0037f)+i*0.91f; val q=t*(0.041f+i*0.0029f)+i*1.37f
            val x=w*(0.5f+0.48f*sin(p.toDouble()).toFloat()); val y=h*(0.5f+0.44f*cos(q.toDouble()).toFloat()); val radius=base*(0.68f+0.14f*sin((p+q).toDouble()).toFloat())
            paint.shader=RadialGradient(x,y,radius,Color.argb(if(i<3)180 else 105,rr,gg,bb),Color.TRANSPARENT,Shader.TileMode.CLAMP); canvas.drawCircle(x,y,radius,paint)
        }
        paint.shader=null
    }
}
