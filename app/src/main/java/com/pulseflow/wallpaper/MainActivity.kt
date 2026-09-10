package com.pulseflow.wallpaper

import android.app.*
import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        val backdrop = FlowPreviewView(this)
        frame.addView(backdrop, FrameLayout.LayoutParams(-1, -1))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.argb(210, 7, 8, 14))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        scroll.addView(root)
        frame.addView(scroll, FrameLayout.LayoutParams(-1, -1))

        fun text(value: String, size: Float, color: Int = Color.WHITE, top: Int = 0): TextView {
            return TextView(this).apply {
                text = value
                textSize = size
                setTextColor(color)
                setPadding(0, dp(top), 0, dp(6))
            }
        }

        fun section(title: String) {
            root.addView(text(title, 13f, Color.rgb(188, 166, 255), 18))
        }

        fun slider(title: String, min: Float, max: Float, current: Float, onChange: (Float) -> Unit) {
            val label = text("$title  %.2f".format(current), 15f, Color.WHITE, 4)
            root.addView(label)
            root.addView(SeekBar(this).apply {
                this.max = 1000
                progress = (((current - min) / (max - min)) * 1000).toInt().coerceIn(0, 1000)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val v = min + (p / 1000f) * (max - min)
                        label.text = "$title  %.2f".format(v)
                        onChange(v)
                        backdrop.reload()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
        }

        fun toggle(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit): Switch {
            return Switch(this).apply {
                text = title
                textSize = 15f
                setTextColor(Color.WHITE)
                isChecked = checked
                isEnabled = enabled
                setPadding(0, dp(4), 0, dp(4))
                setOnCheckedChangeListener { _, value ->
                    onChange(value)
                    backdrop.reload()
                }
                root.addView(this)
            }
        }

        root.addView(text("PULSEFLOW", 28f).apply { gravity = Gravity.CENTER })
        root.addView(text("Fluid Wallpaper Lab", 14f, Color.LTGRAY).apply { gravity = Gravity.CENTER })
        root.addView(text("LIVE FLUID PREVIEW", 11f, Color.rgb(210, 210, 220), 6).apply { gravity = Gravity.CENTER })

        section("FLUID SETTINGS")
        val rangeLabel = text(
            "Speed  %.2f – %.2f".format(FlowSettings.loadSpeedMin(this), FlowSettings.loadSpeedMax(this)),
            15f
        )
        root.addView(rangeLabel)
        root.addView(RangeSliderView(this).apply {
            configure(0.05f, 4.5f, FlowSettings.loadSpeedMin(this@MainActivity), FlowSettings.loadSpeedMax(this@MainActivity))
            setOnRangeChangedListener { lo, hi ->
                rangeLabel.text = "Speed  %.2f – %.2f".format(lo, hi)
                FlowSettings.saveSpeedRange(this@MainActivity, lo, hi)
                backdrop.reload()
            }
        })
        root.addView(text("Daha yüksek değerlerde akış hareketi belirgin şekilde hızlanır.", 12f, Color.LTGRAY))
        slider("Fluid Scale", 0.65f, 1.65f, FlowSettings.loadScale(this)) { FlowSettings.saveScale(this, it) }

        section("GRAPHICS SETTINGS")
        slider("Blur / Softness", 0.25f, 1f, FlowSettings.loadBlur(this)) { FlowSettings.saveBlur(this, it) }
        val graphics = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val blurRadio = RadioButton(this).apply { text = "Blur"; setTextColor(Color.WHITE); id = View.generateViewId() }
        val flutedRadio = RadioButton(this).apply { text = "Fluted Glass"; setTextColor(Color.WHITE); id = View.generateViewId() }
        graphics.addView(blurRadio)
        graphics.addView(flutedRadio)
        root.addView(graphics)
        if (FlowSettings.loadGraphicsMode(this) == "fluted") flutedRadio.isChecked = true else blurRadio.isChecked = true
        graphics.setOnCheckedChangeListener { _, id ->
            FlowSettings.saveGraphicsMode(this, if (id == flutedRadio.id) "fluted" else "blur")
            backdrop.reload()
        }

        section("LIVE BEATS")
        toggle("Live Beats", FlowSettings.loadLiveBeats(this), false) { FlowSettings.saveLiveBeats(this, it) }
        root.addView(text("Audio-reactive engine sonraki adımda bağlanacak.", 12f, Color.GRAY))
        slider("Strength", 0f, 1f, FlowSettings.loadBeatStrength(this)) { FlowSettings.saveBeatStrength(this, it) }

        section("ALBUM ART SETTINGS")
        toggle("Preserve after phone reboot", FlowSettings.loadPreserveReboot(this)) { FlowSettings.savePreserveReboot(this, it) }
        toggle("Preserve after music pause", FlowSettings.loadPreservePause(this)) { FlowSettings.savePreservePause(this, it) }
        val paletteButton = Button(this).apply {
            text = "DEFAULT PALETTE: ${PaletteStore.selectedName(this@MainActivity).uppercase()}"
            setOnClickListener {
                val names = PaletteStore.presets.map { p -> p.name }.toTypedArray()
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Default Color Palette")
                    .setItems(names) { _, which ->
                        PaletteStore.savePreset(this@MainActivity, which)
                        text = "DEFAULT PALETTE: ${PaletteStore.presets[which].name.uppercase()}"
                        backdrop.reload()
                    }
                    .show()
            }
        }
        root.addView(paletteButton)

        section("MISC SETTINGS")
        slider("Brightness", 0.35f, 1.35f, FlowSettings.loadBrightness(this)) { FlowSettings.saveBrightness(this, it) }
        toggle("Adaptive Launcher Color Scheme", FlowSettings.loadAdaptiveColors(this)) { FlowSettings.saveAdaptiveColors(this, it) }
        toggle("Performance Mode", FlowSettings.loadPerformanceMode(this)) { FlowSettings.savePerformanceMode(this, it) }
        toggle("Debug View", FlowSettings.loadDebugView(this)) { FlowSettings.saveDebugView(this, it) }

        root.addView(Button(this).apply {
            text = "CANLI DUVAR KAĞIDI OLARAK AYARLA"
            setOnClickListener {
                startActivity(
                    Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                        WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, PulseWallpaperService::class.java)
                    )
                )
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(18) })

        setContentView(frame)
    }
}
