"""Portable Skia check of the actual AGSL source. Requires skia-python and numpy.
Not a replacement for device testing of Android audio/media services.
"""
from pathlib import Path
import skia
import numpy as np

source = (Path(__file__).resolve().parents[1] / 'app/src/main/java/com/pulseflow/wallpaper/FlowRenderer.kt').read_text()
code = source.split('private val shaderCode = """')[1].split('""".trimIndent()')[0]
effect = skia.RuntimeEffect.MakeForShader(code)

def render(t=10., beat=0., art=None, glass=0., softness=.72, transition=1.):
    b = skia.RuntimeShaderBuilder(effect)
    vals = dict(resolution=[240.,480.],time=t,scale=1.,brightness=1.,softness=softness,
                graphicsMode=glass,beat=beat,bass=beat,coverMix=transition,
                hasCover=1. if art is not None else 0.,hadCover=0.,
                colorA=[.31,.16,.75,1.],colorB=[.06,.49,.82,1.],colorC=[.86,.18,.51,1.])
    for key,val in vals.items(): b.setUniform(key,val)
    bitmap=skia.Surface(64,64)
    if art=='neutral':
        shader=skia.GradientShader.MakeLinear([(0,0),(64,64)],[skia.ColorBLACK,skia.ColorWHITE])
    else:
        shader=skia.GradientShader.MakeLinear([(0,0),(64,64)],[0xff24241a,0xffbabc59,0xff302319])
    bitmap.getCanvas().drawPaint(skia.Paint(Shader=shader))
    tex=bitmap.makeImageSnapshot().makeShader(skia.TileMode.kMirror,skia.TileMode.kMirror,skia.SamplingOptions(skia.FilterMode.kLinear))
    b.setChild('coverNow',tex);b.setChild('coverBefore',tex)
    surf=skia.Surface(240,480)
    surf.getCanvas().drawPaint(skia.Paint(Shader=b.makeShader()))
    return surf.makeImageSnapshot()

def arr(im): return np.array(im).astype(float)[:,:,:3]
neutral=arr(render(art='neutral'))
assert np.max(np.ptp(neutral,axis=2))<=1, 'Neutral album gained false colors'
a=arr(render(art='olive'));b=arr(render(t=20.,art='olive'))
assert np.abs(a-b).mean()>1, 'Album field did not animate'
near=arr(render(t=10.016,art='olive'))
assert np.abs(a-near).mean()<2, 'Adjacent frames jump'
punch=arr(render(beat=1.,art='olive'))
assert np.abs(a-punch).mean()>1, 'Strength has no visible effect'
assert np.abs(a-arr(render(art='olive',glass=1.))).mean()>.1, 'Fluted mode has no effect'
assert np.isfinite(a).all()
for art in (None,'olive','neutral'):
    render(art=art).save('/tmp/pulse-'+str(art)+'.png',skia.kPNG)
print('PASS: shader compiles; neutral colors, temporal motion/continuity, beat deformation and glass mode verified')
