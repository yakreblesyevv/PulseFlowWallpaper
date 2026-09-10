package com.pulseflow.wallpaper

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private var liveBeatsSwitch: Switch? = null
    private val audioPermissionRequest = 1301

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val frame = FrameLayout(this)
        val backdrop = FlowPreviewView(this)
        frame.addView(backdrop, FrameLayout.LayoutParams(-1, -1))

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.argb(106, 7, 8, 14))
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(26), dp(18), dp(30))
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

        fun cardBackground(fillAlpha: Int = 58, strokeAlpha: Int = 70) = GradientDrawable().apply {
            cornerRadius = dp(18).toFloat()
            setColor(Color.argb(fillAlpha, 8, 9, 16))
            setStroke(dp(1), Color.argb(strokeAlpha, 205, 190, 255))
        }

        fun section(title: String, subtitle: String? = null) {
            root.addView(text(title, 13f, Color.rgb(194, 173, 255), 20))
            if (subtitle != null) root.addView(text(subtitle, 11f, Color.rgb(205, 205, 216)))
        }

        val previewCard = FrameLayout(this).apply {
            background = cardBackground(38, 95)
            clipToOutline = true
        }
        val livePreview = FlowPreviewView(this)
        previewCard.addView(livePreview, FrameLayout.LayoutParams(-1, -1))
        previewCard.addView(text("CANLI ÖNİZLEME", 11f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.argb(120, 0, 0, 0))
            }
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
            topMargin = dp(10)
        })

        fun reloadPreviews() {
            backdrop.reload()
            livePreview.reload()
        }

        fun slider(title: String, min: Float, max: Float, current: Float, onChange: (Float) -> Unit) {
            val wrap = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = cardBackground(42, 42)
                setPadding(dp(14), dp(8), dp(14), dp(8))
            }
            val label = text("$title  %.2f".format(current), 14f)
            wrap.addView(label)
            wrap.addView(SeekBar(this).apply {
                this.max = 1000
                progress = (((current - min) / (max - min)) * 1000).toInt().coerceIn(0, 1000)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                        val v = min + (p / 1000f) * (max - min)
                        label.text = "$title  %.2f".format(v)
                        onChange(v)
                        reloadPreviews()
                    }
                    override fun onStartTrackingTouch(s: SeekBar?) {}
                    override fun onStopTrackingTouch(s: SeekBar?) {}
                })
            })
            root.addView(wrap, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }

        fun toggle(title: String, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit): Switch {
            val sw = Switch(this).apply {
                text = title
                textSize = 14f
                setTextColor(Color.WHITE)
                isChecked = checked
                isEnabled = enabled
                setPadding(dp(14), dp(6), dp(14), dp(6))
                background = cardBackground(40, 38)
                setOnCheckedChangeListener { _, value ->
                    onChange(value)
                    reloadPreviews()
                }
            }
            root.addView(sw, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
            return sw
        }

        root.addView(text("PULSEFLOW", 28f).apply { gravity = Gravity.CENTER })
        root.addView(text("Fluid Wallpaper Lab", 14f, Color.LTGRAY).apply { gravity = Gravity.CENTER })
        root.addView(text("v0.13 • Live Beats", 11f, Color.rgb(196, 180, 255), 4).apply { gravity = Gravity.CENTER })
        root.addView(previewCard, LinearLayout.LayoutParams(-1, dp(235)).apply {
            topMargin = dp(12)
            bottomMargin = dp(8)
        })

        section("COLOR PALETTES", "Bir palete dokunduğunda önizleme anında güncellenir.")
        val paletteGrid = GridLayout(this).apply {
            columnCount = 2
            useDefaultMargins = false
        }
        fun rebuildPalettes() {
            paletteGrid.removeAllViews()
            val selected = PaletteStore.selectedName(this)
            PaletteStore.presets.forEachIndexed { index, preset ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    background = GradientDrawable().apply {
                        cornerRadius = dp(16).toFloat()
                        setColor(Color.argb(if (preset.name == selected) 90 else 46, 12, 12, 20))
                        setStroke(dp(if (preset.name == selected) 2 else 1), if (preset.name == selected) Color.rgb(190, 160, 255) else Color.argb(55, 220, 210, 255))
                    }
                    isClickable = true
                    setOnClickListener {
                        PaletteStore.savePreset(this@MainActivity, index)
                        rebuildPalettes()
                        reloadPreviews()
                    }
                }
                val swatches = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER
                }
                preset.colors.forEach { c ->
                    val dot = View(this).apply {
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.OVAL
                            setColor(c)
                            setStroke(dp(1), Color.argb(90, 255, 255, 255))
                        }
                    }
                    swatches.addView(dot, LinearLayout.LayoutParams(dp(30), dp(30)).apply {
                        marginStart = dp(3)
                        marginEnd = dp(3)
                    })
                }
                card.addView(swatches)
                card.addView(text(preset.name, 12f, Color.WHITE, 8).apply { gravity = Gravity.CENTER })
                if (preset.name == selected) card.addView(text("SEÇİLİ", 9f, Color.rgb(201, 184, 255)).apply { gravity = Gravity.CENTER })
                paletteGrid.addView(card, GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(dp(4), dp(4), dp(4), dp(4))
                })
            }
        }
        rebuildPalettes()
        root.addView(paletteGrid)

        section("FLUID SETTINGS", "Akışın hızını ve ölçeğini canlı önizlemeden ayarla.")
        val speedWrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(44, 45)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val rangeLabel = text("Speed  %.2f – %.2f".format(FlowSettings.loadSpeedMin(this), FlowSettings.loadSpeedMax(this)), 14f)
        speedWrap.addView(rangeLabel)
        speedWrap.addView(RangeSliderView(this).apply {
            configure(0.05f, 4.5f, FlowSettings.loadSpeedMin(this@MainActivity), FlowSettings.loadSpeedMax(this@MainActivity))
            setOnRangeChangedListener { lo, hi ->
                rangeLabel.text = "Speed  %.2f – %.2f".format(lo, hi)
                FlowSettings.saveSpeedRange(this@MainActivity, lo, hi)
                reloadPreviews()
            }
        })
        speedWrap.addView(text("Sol tutamak minimum, sağ tutamak maksimum akış hızıdır.", 11f, Color.LTGRAY))
        root.addView(speedWrap, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        slider("Fluid Scale", 0.65f, 1.65f, FlowSettings.loadScale(this)) { FlowSettings.saveScale(this, it) }

        section("GRAPHICS", "Görüntünün yumuşaklığını ve cam efektini değiştir.")
        slider("Blur / Softness", 0.25f, 1f, FlowSettings.loadBlur(this)) { FlowSettings.saveBlur(this, it) }
        val graphicsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBackground(42, 42)
            setPadding(dp(14), dp(8), dp(14), dp(8))
        }
        val graphics = RadioGroup(this).apply { orientation = RadioGroup.HORIZONTAL }
        val blurRadio = RadioButton(this).apply { text = "Blur"; setTextColor(Color.WHITE); id = View.generateViewId() }
        val flutedRadio = RadioButton(this).apply { text = "Fluted Glass"; setTextColor(Color.WHITE); id = View.generateViewId() }
        graphics.addView(blurRadio)
        graphics.addView(flutedRadio)
        graphicsCard.addView(graphics)
        root.addView(graphicsCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        if (FlowSettings.loadGraphicsMode(this) == "fluted") flutedRadio.isChecked = true else blurRadio.isChecked = true
        graphics.setOnCheckedChangeListener { _, id ->
            FlowSettings.saveGraphicsMode(this, if (id == flutedRadio.id) "fluted" else "blur")
            reloadPreviews()
        }

        section("DISPLAY")
        slider("Brightness", 0.35f, 1.35f, FlowSettings.loadBrightness(this)) { FlowSettings.saveBrightness(this, it) }
        toggle("Adaptive Launcher Color Scheme", FlowSettings.loadAdaptiveColors(this)) { FlowSettings.saveAdaptiveColors(this, it) }
        toggle("Performance Mode", FlowSettings.loadPerformanceMode(this)) { FlowSettings.savePerformanceMode(this, it) }

        section("LIVE BEATS", "Müzik çalarken akış gerçek ses verisine göre tepki verir.")
        val liveBeats = Switch(this).apply {
            text = "Live Beats"
            textSize = 14f
            setTextColor(Color.WHITE)
            isChecked = FlowSettings.loadLiveBeats(this@MainActivity)
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background = cardBackground(40, 38)
        }
        liveBeatsSwitch = liveBeats
        liveBeats.setOnCheckedChangeListener { button, enabled ->
            if (enabled && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                button.isChecked = false
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), audioPermissionRequest)
            } else {
                FlowSettings.saveLiveBeats(this@MainActivity, enabled)
                if (enabled) BeatAnalyzer.start(this@MainActivity) else BeatAnalyzer.stop()
                reloadPreviews()
            }
        }
        root.addView(liveBeats, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        root.addView(text("İlk açılışta yalnızca ses görselleştirmesi için mikrofon/ses izni ister.", 11f, Color.LTGRAY))
        slider("Strength", 0f, 1f, FlowSettings.loadBeatStrength(this)) { FlowSettings.saveBeatStrength(this, it) }

        section("PERSISTENCE")
        toggle("Preserve after phone reboot", FlowSettings.loadPreserveReboot(this)) { FlowSettings.savePreserveReboot(this, it) }
        toggle("Preserve after music pause", FlowSettings.loadPreservePause(this)) { FlowSettings.savePreservePause(this, it) }
        toggle("Debug View", FlowSettings.loadDebugView(this)) { FlowSettings.saveDebugView(this, it) }

        root.addView(Button(this).apply {
            text = "VARSAYILAN AYARLARA DÖN"
            setOnClickListener {
                FlowSettings.saveSpeedRange(this@MainActivity, 0.45f, 2.0f)
                FlowSettings.saveScale(this@MainActivity, 1.0f)
                FlowSettings.saveBlur(this@MainActivity, 0.72f)
                FlowSettings.saveBrightness(this@MainActivity, 1.0f)
                FlowSettings.saveGraphicsMode(this@MainActivity, "blur")
                FlowSettings.saveLiveBeats(this@MainActivity, false)
                FlowSettings.saveBeatStrength(this@MainActivity, 0.55f)
                BeatAnalyzer.stop()
                PaletteStore.savePreset(this@MainActivity, 7)
                recreate()
            }
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) })

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
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(10) })

        setContentView(frame)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == audioPermissionRequest) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                FlowSettings.saveLiveBeats(this, true)
                liveBeatsSwitch?.isChecked = true
                BeatAnalyzer.start(this)
            } else {
                FlowSettings.saveLiveBeats(this, false)
                liveBeatsSwitch?.isChecked = false
                Toast.makeText(this, "Live Beats için ses izni gerekiyor.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        if (!FlowSettings.loadLiveBeats(this)) BeatAnalyzer.stop()
        super.onDestroy()
    }
}
