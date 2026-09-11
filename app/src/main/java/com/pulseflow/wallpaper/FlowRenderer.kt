package com.pulseflow.wallpaper

import android.graphics.*
import android.os.Build
import kotlin.math.cos
import kotlin.math.sin

class FlowRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var lastFrameNanos = 0L
    private var elapsed = 0f
    private var phase = 0f
    private var delta = 0.016f
    private var currentTexture: Bitmap? = null
    private var previousTexture: Bitmap? = null
    private var textureMix = 1f
    var artwork: Bitmap? = null
        set(value) {
            if (field === value) return
            previousTexture = currentTexture
            currentTexture = value
            textureMix = 0f
            field = value
        }
    var liveBeats = false
    var debugView = false
    private val defaultTexture = Bitmap.createBitmap(1,1,Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
    private var inputCurrent: BitmapShader? = null
    private var inputPrevious: BitmapShader? = null
    private var boundCurrent: Bitmap? = null
    private var boundPrevious: Bitmap? = null
    private var runtimeShader: RuntimeShader? = null
    private var gpuShaderFailed = false
    private var smoothBeat = 0f
    private var smoothBass = 0f

    var speedMin = 0.45f
    var speedMax = 2.0f
    var scale = 1.0f
    var brightness = 1.0f
    var blur = 0.72f
    var graphicsMode = "blur"
    var beatStrength = 0.55f
    var colors = intArrayOf(Color.rgb(80,40,190), Color.rgb(15,125,210), Color.rgb(220,45,130))

    private val shaderCode = """
        uniform float2 resolution;
        uniform float time;
        uniform float scale;
        uniform float brightness;
        uniform float softness;
        uniform float graphicsMode;
        uniform float beat;
        uniform float bass;
        uniform shader coverNow;
        uniform shader coverBefore;
        uniform float coverMix;
        uniform float hasCover;
        uniform float hadCover;
        layout(color) uniform half4 colorA;
        layout(color) uniform half4 colorB;
        layout(color) uniform half4 colorC;

        float2 rot(float2 p, float a) { float s=sin(a); float c=cos(a); return float2(c*p.x-s*p.y,s*p.x+c*p.y); }
        float field(float2 p,float t){
            float v=0.0;
            v+=0.50*sin(p.x*1.18+p.y*0.82+t*0.20);
            v+=0.28*sin(p.x*2.05-p.y*1.36-t*0.13+1.7);
            v+=0.14*cos(p.x*3.55+p.y*2.72+t*0.09-0.8);
            v+=0.08*sin(p.x*6.10-p.y*4.30-t*0.055+2.4);
            return v;
        }
        float2 liquidWarp(float2 p,float t,float b,float low){
            float2 q=p;
            float a=field(q*0.82+float2(0.0,t*0.032),t);
            float bb=field(rot(q,1.5708)*0.88+float2(t*0.026,0.0),t+5.0);
            q+=float2(a,bb)*(0.29+b*0.055);
            float c=field(q*1.24+float2(t*0.018,-t*0.023),t+10.0);
            float d=field(rot(q,-0.73)*1.18+float2(-t*0.020,t*0.015),t+16.0);
            q+=float2(c,d)*(0.18+low*0.045);

            // Deliberately gentle music motion: no flash-like radial pumping.
            float phase=t*0.20;
            float2 musicWarp=float2(
                sin(q.y*2.35+phase)+0.35*sin((q.x+q.y)*3.7-phase*0.65),
                cos(q.x*2.20-phase*0.82)+0.35*cos((q.x-q.y)*3.55+phase*0.58)
            );
            q+=musicWarp*(b*0.22+low*0.10);
            q.x+=(0.055+b*0.018)*sin(q.y*3.0+t*0.10)+0.028*sin((q.x+q.y)*5.0-t*0.06);
            q.y+=(0.050+b*0.016)*cos(q.x*2.7-t*0.09)+0.026*cos((q.x-q.y)*4.6+t*0.055);
            return q;
        }
        half4 main(float2 fragCoord){
            float2 uv=(fragCoord-0.5*resolution)/min(resolution.x,resolution.y);
            uv/=max(scale,0.42);
            float t=time*(0.88+beat*0.018);
            float2 p=liquidWarp(uv,t,beat,bass);
            if(graphicsMode>0.5) p.x += 0.045*sin(uv.x*42.0);
            float n1=field(p*0.94,t+2.0);
            float n2=field(rot(p,0.92)*1.03+float2(0.24,-0.11),-t*0.78+7.0);
            float n3=field(rot(p,-0.61)*0.89+float2(-0.18,0.27),t*0.64+13.0);
            float ribbon1=0.5+0.5*sin(p.x*1.45+p.y*0.72+n1*(1.55+beat*0.08)+t*0.075);
            float ribbon2=0.5+0.5*sin(-p.x*0.78+p.y*1.62+n2*(1.42+bass*0.06)-t*0.060+1.9);
            float ribbon3=0.5+0.5*sin(p.x*1.05-p.y*1.12+n3*(1.30+beat*0.06)+t*0.052+4.1);
            float wa=smoothstep(0.18,0.90,ribbon1);
            float wb=smoothstep(0.14,0.92,ribbon2);
            float wc=smoothstep(0.20,0.88,ribbon3);
            half3 col=mix(colorA.rgb,colorB.rgb,half(wb));
            col=mix(col,colorC.rgb,half(wc*0.80));
            col=mix(col,colorA.rgb,half(wa*0.46));
            // Fold the sampled album texture into broad moving liquid regions.
            float2 st=0.5+0.46*sin(p*1.25+float2(n2*0.7+t*0.065,n3*0.7-t*0.052));
            float radius=mix(0.4,7.0,softness);
            float2 coord=clamp(st,0.02,0.98)*64.0;
            half3 now=coverNow.eval(coord).rgb*0.4;
            half3 before=coverBefore.eval(coord).rgb*0.4;
            for(int i=0;i<6;i++) {
                float a=float(i)*1.04719755;
                float2 offset=float2(cos(a),sin(a))*radius;
                now+=coverNow.eval(coord+offset).rgb*0.1;
                before+=coverBefore.eval(coord+offset).rgb*0.1;
            }
            col=mix(mix(col,before,half(hadCover)),mix(col,now,half(hasCover)),half(coverMix));
            float gloss=0.5+0.5*sin((p.x*0.60+p.y*0.82)*3.14159+n1*0.82-t*0.045);
            float depth=0.82+0.18*gloss+0.07*(n2+n3);
            // No beat brightness pumping: avoids strobe/flicker sensation.
            if(graphicsMode>0.5){ float ribs=sin((uv.x+0.035*sin(t*0.07))*34.0); depth*=0.96+0.055*ribs; }
            float softnessMix=mix(0.94,1.04,clamp(softness,0.0,1.0));
            col*=half(brightness*depth*softnessMix);
            col*=mix(half3(0.95,0.96,0.985),half3(1.0),half(mix(hadCover,hasCover,coverMix)));
            return half4(col,1.0);
        }
    """.trimIndent()

    private fun advanceTime() {
        val now=android.os.SystemClock.elapsedRealtimeNanos()
        delta=if(lastFrameNanos==0L) 0.016f else ((now-lastFrameNanos)/1e9f).coerceIn(0f,0.1f)
        lastFrameNanos=now
        elapsed+=delta
        val lo=minOf(speedMin,speedMax); val hi=maxOf(speedMin,speedMax)
        val speed=(lo+hi)*0.5f+(hi-lo)*0.5f*sin(elapsed*0.18f)
        phase+=delta*speed
        textureMix=(textureMix+delta/1.6f).coerceAtMost(1f)
    }
    private fun integratedTime() = phase

    private fun updateMusicMotion() {
        val targetBeat=if(liveBeats && BeatAnalyzer.hasSignal()) (BeatAnalyzer.level*beatStrength).coerceIn(0f,1f) else 0f
        val targetBass=if(liveBeats && BeatAnalyzer.hasSignal()) (BeatAnalyzer.bass*beatStrength).coerceIn(0f,1f) else 0f
        // Slow attack + slower release prevents rapid frame-to-frame flashing.
        val beatRate=1f-kotlin.math.exp(-delta/(if(targetBeat>smoothBeat) 0.08f else 0.35f))
        val bassRate=1f-kotlin.math.exp(-delta/0.24f)
        smoothBeat+=(targetBeat-smoothBeat)*beatRate
        smoothBass+=(targetBass-smoothBass)*bassRate
    }

    fun draw(canvas: Canvas) {
        advanceTime()
        updateMusicMotion()
        if(Build.VERSION.SDK_INT>=33&&canvas.isHardwareAccelerated&&!gpuShaderFailed){
            try{drawGpu(canvas);drawDebug(canvas);return}catch(_:Throwable){gpuShaderFailed=true;runtimeShader=null}
        }
        drawFallback(canvas)
        drawDebug(canvas)
    }

    private fun drawDebug(canvas: Canvas) {
        if(!debugView) return
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE; textSize=28f; setShadowLayer(3f,1f,1f,Color.BLACK) }
        canvas.drawText("${if(gpuShaderFailed || Build.VERSION.SDK_INT<33) "Canvas" else "GPU"} • ${if(artwork==null) "Palette" else "Album"} • ${BeatAnalyzer.statusText()}",20f,50f,p)
    }

    @android.annotation.TargetApi(33)
    private fun drawGpu(canvas:Canvas){
        val shader=runtimeShader?:RuntimeShader(shaderCode).also{runtimeShader=it}
        shader.setFloatUniform("resolution",canvas.width.toFloat(),canvas.height.toFloat())
        shader.setFloatUniform("time",integratedTime()); shader.setFloatUniform("scale",scale)
        shader.setFloatUniform("brightness",brightness); shader.setFloatUniform("softness",blur)
        shader.setFloatUniform("graphicsMode",if(graphicsMode=="fluted")1f else 0f)
        shader.setFloatUniform("beat",smoothBeat); shader.setFloatUniform("bass",smoothBass)
        shader.setColorUniform("colorA",colors[0]); shader.setColorUniform("colorB",colors[1%colors.size]); shader.setColorUniform("colorC",colors[2%colors.size])
        val now=currentTexture?:defaultTexture
        val before=previousTexture?:defaultTexture
        if(boundCurrent !== now) { inputCurrent=BitmapShader(now,Shader.TileMode.MIRROR,Shader.TileMode.MIRROR); boundCurrent=now }
        if(boundPrevious !== before) { inputPrevious=BitmapShader(before,Shader.TileMode.MIRROR,Shader.TileMode.MIRROR); boundPrevious=before }
        shader.setInputShader("coverNow",inputCurrent!!); shader.setInputShader("coverBefore",inputPrevious!!)
        shader.setFloatUniform("coverMix",textureMix)
        shader.setFloatUniform("hasCover",if(currentTexture==null)0f else 1f)
        shader.setFloatUniform("hadCover",if(previousTexture==null)0f else 1f)
        paint.isFilterBitmap=true
        paint.shader=shader; canvas.drawRect(0f,0f,canvas.width.toFloat(),canvas.height.toFloat(),paint); paint.shader=null
    }

    private fun drawFallback(canvas:Canvas){
        val w=canvas.width.toFloat(); val h=canvas.height.toFloat(); canvas.drawColor(Color.rgb(3,4,9))
        val t=integratedTime()*(0.88f+smoothBeat*0.018f); val base=maxOf(w,h)*scale
        for(i in 0 until 10){
            val raw=colors[i%colors.size]
            val rr=(Color.red(raw)*brightness).toInt().coerceIn(0,255); val gg=(Color.green(raw)*brightness).toInt().coerceIn(0,255); val bb=(Color.blue(raw)*brightness).toInt().coerceIn(0,255)
            val phase=i*0.71f; val p=t*(0.070f+i*0.0035f)+phase; val q=t*(0.052f+i*0.0027f)+phase*1.37f
            val wobble=smoothBeat*(0.025f+0.006f*(i%3))+smoothBass*0.012f
            val x=w*(0.50f+(0.48f+wobble)*sin((p+smoothBass*0.10f).toDouble()).toFloat())
            val y=h*(0.50f+(0.44f+wobble*0.8f)*cos((q-smoothBeat*0.08f).toDouble()).toFloat())
            val radius=base*(0.92f+0.18f*sin((p*0.7f+q).toDouble()).toFloat()+smoothBeat*0.025f)
            paint.shader=RadialGradient(x,y,radius,Color.argb(if(i<4)150 else 92,rr,gg,bb),Color.TRANSPARENT,Shader.TileMode.CLAMP)
            canvas.drawCircle(x,y,radius,paint)
        }
        paint.shader=null
    }
}
