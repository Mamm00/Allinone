package com.hyperlauncher.glass

import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.View

/** GPU-composited, static uniforms: no frame loop and no per-frame CPU blur. */
class GlassView(context: Context) : View(context) {
    private var bitmap: Bitmap?=null
    private var fallback: Bitmap?=null
    private var radius=16f
    private var intensity=.4f
    private var overlay=.2f
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    fun configure(source: Bitmap?,blur: Float,strength: Float,darkOverlay: Float) {
        if(bitmap !== source) {
            bitmap=source
            // Pre-31 static downsampled fallback. Only rebuilt when the cached crop changes.
            fallback=if(Build.VERSION.SDK_INT<31 && source!=null)
                Bitmap.createScaledBitmap(source,(source.width/12).coerceAtLeast(1),(source.height/12).coerceAtLeast(1),true) else null
        }
        radius=blur.coerceIn(0f,50f);intensity=strength.coerceIn(0f,1f);overlay=darkOverlay.coerceIn(0f,.85f)
        if(Build.VERSION.SDK_INT>=31) updateEffect()
        invalidate()
    }
    override fun onSizeChanged(w: Int,h: Int,oldw: Int,oldh: Int) { if(Build.VERSION.SDK_INT>=31) updateEffect() }
    @android.annotation.TargetApi(31)
    private fun updateEffect() {
        val blur=RenderEffect.createBlurEffect(radius.coerceAtLeast(.1f),radius.coerceAtLeast(.1f),Shader.TileMode.CLAMP)
        if(Build.VERSION.SDK_INT>=33 && intensity>0f) {
            val shader=RuntimeShader("""
                uniform shader content;
                uniform float2 resolution;
                uniform float intensity;
                half4 main(float2 p) {
                    float2 uv=p/max(resolution,float2(1));
                    float2 normal=(uv-float2(0.5))*2.0;
                    float edge=pow(clamp(length(normal)*0.7,0.0,1.0),4.0);
                    float2 offset=normal*edge*intensity*12.0;
                    half4 color=content.eval(p+offset);
                    color.r=content.eval(p+offset*1.12).r;
                    color.b=content.eval(p+offset*0.88).b;
                    color.rgb+=half3(edge*intensity*0.1);
                    return color;
                }
            """.trimIndent())
            shader.setFloatUniform("resolution",width.toFloat().coerceAtLeast(1f),height.toFloat().coerceAtLeast(1f))
            shader.setFloatUniform("intensity",intensity)
            setRenderEffect(RenderEffect.createChainEffect(RenderEffect.createRuntimeShaderEffect(shader,"content"),blur))
        } else setRenderEffect(blur)
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val source=fallback ?: bitmap
        if(source!=null) canvas.drawBitmap(source,null,Rect(0,0,width,height),paint)
        else canvas.drawColor(Color.rgb(61,81,94))
        canvas.drawColor(Color.argb((overlay*255).toInt(),0,0,0))
    }
}
