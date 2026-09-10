package com.pulseflow.wallpaper

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,42,30,28);setBackgroundColor(Color.rgb(7,8,14))}
        root.addView(TextView(this).apply{text="PULSEFLOW";textSize=27f;setTextColor(Color.WHITE);gravity=Gravity.CENTER})
        root.addView(TextView(this).apply{text="Fluid Wallpaper Lab";textSize=14f;setTextColor(Color.LTGRAY);gravity=Gravity.CENTER;setPadding(0,4,0,14)})
        val preview=FlowPreviewView(this); root.addView(preview,LinearLayout.LayoutParams(-1,0,1f))

        fun slider(title:String,min:Float,max:Float,current:Float,onChange:(Float)->Unit){
            val label=TextView(this).apply{setTextColor(Color.WHITE);textSize=15f;setPadding(4,12,4,0);text="$title  %.2f".format(current)};root.addView(label)
            root.addView(SeekBar(this).apply{this.max=100;progress=(((current-min)/(max-min))*100).toInt().coerceIn(0,100);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
                override fun onProgressChanged(s:SeekBar?,p:Int,f:Boolean){val v=min+(p/100f)*(max-min);label.text="$title  %.2f".format(v);onChange(v);preview.reload()}
                override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}
            })},LinearLayout.LayoutParams(-1,-2))
        }

        slider("Akış Hızı",0.12f,2.5f,FlowSettings.loadSpeed(this)){FlowSettings.saveSpeed(this,it)}
        slider("Fluid Scale",0.65f,1.65f,FlowSettings.loadScale(this)){FlowSettings.saveScale(this,it)}
        slider("Blur / Yumuşaklık",0.25f,1f,FlowSettings.loadBlur(this)){FlowSettings.saveBlur(this,it)}
        slider("Brightness",0.35f,1.35f,FlowSettings.loadBrightness(this)){FlowSettings.saveBrightness(this,it)}

        root.addView(Button(this).apply{text="CANLI DUVAR KAĞIDI OLARAK AYARLA";setOnClickListener{startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,ComponentName(this@MainActivity,PulseWallpaperService::class.java))) }},LinearLayout.LayoutParams(-1,-2))
        setContentView(root)
    }
}
