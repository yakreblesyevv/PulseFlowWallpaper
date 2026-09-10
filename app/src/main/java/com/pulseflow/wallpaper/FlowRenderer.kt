package com.pulseflow.wallpaper

import android.graphics.*
import android.os.Build
import kotlin.math.cos
import kotlin.math.sin

class FlowRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val start = System.currentTimeMillis()
    private var runtimeShader: RuntimeShader? = null
    private var gpuShaderFailed = false

    var speedMin = 0.22f
    var speedMax = 1.10f
    var scale = 1.0f
    var brightness = 1.0f
    var blur = 0.72f
    var graphicsMode = "blur"
    var colors = intArrayOf(Color.rgb(80,40,190), Color.rgb(15,125,210), Color.rgb(220,45,130))

    private val shaderCode = """
        uniform float2 resolution;
        uniform float time;
        uniform float scale;
        uniform float brightness;
        uniform float softness;
        uniform float graphicsMode;
        layout(color) uniform half4 colorA;
        layout(color) uniform half4 colorB;
        layout(color) uniform half4 colorC;

        float2 rot(float2 p, float a) {
            float s=sin(a); float c=cos(a);
            return float2(c*p.x-s*p.y,s*p.x+c*p.y);
        }

        float2 flowWarp(float2 p,float t) {
            float2 q=p;
            q.x += 0.20*sin(p.y*1.55+t*0.22)+0.09*sin(p.y*3.10-t*0.12);
            q.y += 0.18*cos(p.x*1.45-t*0.19)+0.08*sin(p.x*2.85+t*0.10);
            float2 r=rot(q,0.16*sin(t*0.09));
            r.x += 0.07*sin((q.x+q.y)*2.20+t*0.08);
            r.y += 0.06*cos((q.x-q.y)*2.05-t*0.07);
            return r;
        }

        float blob(float2 p,float2 c,float rx,float ry,float angle) {
            float2 d=rot(p-c,angle)/float2(rx,ry);
            return 1.0/(0.085+dot(d,d));
        }

        half4 main(float2 fragCoord) {
            float2 uv=(fragCoord-0.5*resolution)/min(resolution.x,resolution.y);
            uv/=max(scale,0.42);
            float t=time*0.42;
            float2 p=flowWarp(uv,t);

            if (graphicsMode>0.5) {
                float ribs=sin((uv.x+0.05*sin(t*0.05))*34.0);
                p.x += ribs*0.028;
                p.y += sin(uv.x*17.0+t*0.035)*0.008;
            }

            float2 c1=float2(-0.46+0.20*sin(t*0.31),-0.24+0.23*cos(t*0.23));
            float2 c2=float2( 0.39+0.23*cos(t*0.27),-0.17+0.18*sin(t*0.29));
            float2 c3=float2(-0.18+0.25*cos(t*0.19), 0.38+0.19*sin(t*0.21));
            float2 c4=float2( 0.34+0.19*sin(t*0.17), 0.36+0.22*cos(t*0.16));
            float2 c5=float2( 0.02+0.17*sin(t*0.13), 0.02+0.17*cos(t*0.15));

            float f1=blob(p,c1,0.55,0.90,0.35+0.20*sin(t*0.14));
            float f2=blob(p,c2,0.62,0.82,-0.42+0.17*cos(t*0.12));
            float f3=blob(p,c3,0.74,0.58,0.68+0.18*sin(t*0.10));
            float f4=blob(p,c4,0.58,0.76,-0.16+0.15*cos(t*0.11));
            float f5=blob(p,c5,0.92,0.92,0.0);

            float a=f1+0.72*f3+0.28*f5;
            float b=f2+0.74*f4+0.26*f5;
            float c=0.52*f1+0.44*f2+0.58*f5;
            float wa=smoothstep(1.05,3.25,a);
            float wb=smoothstep(1.00,3.15,b);
            float wc=smoothstep(0.95,2.95,c);
            float shear=0.5+0.5*sin((p.x*0.72-p.y*0.48)*3.14159+t*0.16);
            wa=clamp(wa+(shear-0.5)*0.16,0.0,1.0);
            wb=clamp(wb-(shear-0.5)*0.13,0.0,1.0);

            half3 base=mix(colorA.rgb,colorB.rgb,half(wb));
            half3 col=mix(base,colorC.rgb,half(wc*0.72));
            col=mix(col,colorA.rgb,half(wa*0.52));
            float density=clamp((a+b+c)/7.5,0.0,1.0);
            float shade=0.76+0.28*density+0.07*sin(p.y*2.1-t*0.09);
            float soft=mix(0.90,1.05,clamp(softness,0.0,1.0));
            if (graphicsMode>0.5) shade *= 0.96+0.06*sin(uv.x*34.0);
            col*=half(brightness*shade*soft);
            col*=half3(0.94,0.95,0.97);
            return half4(col,1.0);
        }
    """.trimIndent()

    private fun integratedTime(): Float {
        val s=(System.currentTimeMillis()-start)/1000f
        val lo=minOf(speedMin,speedMax)
        val hi=maxOf(speedMin,speedMax)
        val mid=(lo+hi)*0.5f
        val amp=(hi-lo)*0.5f
        val omega=0.18f
        return mid*s + (amp/omega)*(1f-cos((omega*s).toDouble()).toFloat())
    }

    fun draw(canvas: Canvas) {
        if (Build.VERSION.SDK_INT>=33 && canvas.isHardwareAccelerated && !gpuShaderFailed) {
            try { drawGpu(canvas); return } catch (_:Throwable) { gpuShaderFailed=true; runtimeShader=null }
        }
        drawFallback(canvas)
    }

    private fun drawGpu(canvas: Canvas) {
        val shader=runtimeShader ?: RuntimeShader(shaderCode).also{runtimeShader=it}
        shader.setFloatUniform("resolution",canvas.width.toFloat(),canvas.height.toFloat())
        shader.setFloatUniform("time",integratedTime())
        shader.setFloatUniform("scale",scale)
        shader.setFloatUniform("brightness",brightness)
        shader.setFloatUniform("softness",blur)
        shader.setFloatUniform("graphicsMode",if(graphicsMode=="fluted")1f else 0f)
        shader.setColorUniform("colorA",colors[0])
        shader.setColorUniform("colorB",colors[1%colors.size])
        shader.setColorUniform("colorC",colors[2%colors.size])
        paint.shader=shader
        canvas.drawRect(0f,0f,canvas.width.toFloat(),canvas.height.toFloat(),paint)
        paint.shader=null
    }

    private fun drawFallback(canvas: Canvas) {
        val w=canvas.width.toFloat(); val h=canvas.height.toFloat(); canvas.drawColor(Color.rgb(3,4,9))
        val t=integratedTime()*0.42f; val base=maxOf(w,h)*scale
        for(i in 0 until 8){
            val raw=colors[i%colors.size]
            val rr=(Color.red(raw)*brightness).toInt().coerceIn(0,255)
            val gg=(Color.green(raw)*brightness).toInt().coerceIn(0,255)
            val bb=(Color.blue(raw)*brightness).toInt().coerceIn(0,255)
            val p=t*(0.10f+i*0.006f)+i*0.93f; val q=t*(0.075f+i*0.005f)+i*1.31f
            var x=w*(0.5f+0.42f*sin(p.toDouble()).toFloat()); val y=h*(0.5f+0.38f*cos(q.toDouble()).toFloat())
            if(graphicsMode=="fluted") x += 12f*sin((y/h*24f).toDouble()).toFloat()
            val radius=base*(0.82f+0.12f*sin((p+q).toDouble()).toFloat())
            paint.shader=RadialGradient(x,y,radius,Color.argb(if(i<3)175 else 100,rr,gg,bb),Color.TRANSPARENT,Shader.TileMode.CLAMP)
            canvas.drawCircle(x,y,radius,paint)
        }
        paint.shader=null
    }
}
