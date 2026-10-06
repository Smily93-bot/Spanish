from PIL import Image
import numpy as np
from scipy import ndimage as nd
# Builds Nilo's walk-cycle sprite sheet from Lía's: teal suit -> orange, clear visor -> mirrored gold pilot visor.
# Usage: python3 tools/make_nilo.py   (needs pillow, numpy, scipy)
import os
ROOT=os.path.join(os.path.dirname(os.path.abspath(__file__)),'..')
SRC=os.path.join(ROOT,'app/src/main/res/drawable-nodpi/explorer_walk.webp')
DST=os.path.join(ROOT,'app/src/main/res/drawable-nodpi/nilo_walk.webp')
im=Image.open(SRC).convert('RGBA')
A=np.asarray(im).astype(np.float32)/255
FR=[(164,33,387,582),(727,34,375,582),(144,630,420,583),(729,630,390,580)]

def visor_mask(c):
    rgb=c[...,:3]; al=c[...,3]
    mx=rgb.max(-1); mn=rgb.min(-1); sat=np.where(mx>0,(mx-mn)/np.maximum(mx,1e-6),0)
    white=(mn>0.68)&(sat<0.2)&(al>0.4)
    r,g,b=rgb[...,0],rgb[...,1],rgb[...,2]
    glass=(b>0.75)&(b>r+0.08)&(g>0.6)&(al>0.5)
    for it in range(3,12):
        w2=white.copy(); w2[300:]=False
        w2=nd.binary_closing(w2,iterations=it)
        holes=nd.binary_fill_holes(w2)&~w2
        lab,n=nd.label(holes)
        counts=np.bincount(lab[glass],minlength=n+1); counts[0]=0
        v=lab==counts.argmax()
        if 20000<v.sum()<40000: return nd.binary_fill_holes(v), it
    raise SystemExit('visor not found')

def rgb_to_hsv(rgb):
    r,g,b=rgb[...,0],rgb[...,1],rgb[...,2]
    mx=rgb.max(-1); mn=rgb.min(-1); d=mx-mn
    h=np.zeros_like(mx)
    m=d>1e-6
    rr=(mx==r)&m; gg=(mx==g)&m&~rr; bb=m&~rr&~gg
    h[rr]=((g-b)[rr]/d[rr])%6; h[gg]=((b-r)[gg]/d[gg])+2; h[bb]=((r-g)[bb]/d[bb])+4
    h=h/6; s=np.where(mx>0,d/np.maximum(mx,1e-6),0)
    return h,s,mx
def hsv_to_rgb(h,s,v):
    i=np.floor(h*6)%6; f=h*6-np.floor(h*6)
    p=v*(1-s); q=v*(1-f*s); t=v*(1-(1-f)*s)
    out=np.zeros(h.shape+(3,))
    for k,(a,b,c) in enumerate([(v,t,p),(q,v,p),(p,v,t),(p,q,v),(t,p,v),(v,p,q)]):
        m=i==k; out[m,0]=a[m]; out[m,1]=b[m]; out[m,2]=c[m]
    return out

out=A.copy()
for idx,(x,y,w,h) in enumerate(FR):
    c=A[y:y+h,x:x+w].copy()
    rgb=c[...,:3]; al=c[...,3]
    H,S,V=rgb_to_hsv(rgb)
    # 1) Teal suit -> warm orange (Nilo's pilot suit)
    teal=(H>0.43)&(H<0.56)&(S>0.25)&(al>0.1)
    H2=np.where(teal,0.08,H); S2=np.where(teal,np.clip(S*0.95,0,1),S); V2=np.where(teal,np.clip(V*1.3+0.04,0,1),V)
    rgb2=hsv_to_rgb(H2,S2,V2)
    # 2) Gold mirrored visor
    v,it=visor_mask(c)
    paint=nd.binary_erosion(v,iterations=2)
    ys,xs=np.nonzero(paint); y0,y1,x0,x1=ys.min(),ys.max(),xs.min(),xs.max()
    yy,xx=np.mgrid[0:h,0:w]
    t=np.clip(((xx-x0)/(x1-x0+1)*0.45+(yy-y0)/(y1-y0+1)*0.55),0,1)
    top=np.array([1.0,0.88,0.48]); mid=np.array([0.95,0.66,0.16]); bot=np.array([0.55,0.30,0.06])
    grad=np.where(t[...,None]<0.5, top+(mid-top)*(t[...,None]/0.5), mid+(bot-mid)*((t[...,None]-0.5)/0.5))
    # diagonal reflection band and a soft highlight blob
    u=(xx-x0)/(x1-x0+1)-(yy-y0)/(y1-y0+1)
    band=np.exp(-((u-0.15)/0.07)**2)*0.55+np.exp(-((u+0.25)/0.03)**2)*0.35
    cx,cy=x0+(x1-x0)*0.3,y0+(y1-y0)*0.25
    blob=np.exp(-(((xx-cx)/((x1-x0)*0.16))**2+((yy-cy)/((y1-y0)*0.12))**2))*0.6
    vis=grad+(1-grad)*np.clip(band+blob,0,1)[...,None]
    # small reflected stars
    rng=np.random.default_rng(7+idx)
    for _ in range(5):
        sy=rng.integers(y0+10,y1-10); sx=rng.integers(x0+10,x1-10)
        if paint[sy,sx]:
            rr=np.hypot(yy-sy,xx-sx); vis=np.where((rr<2.2)[...,None],1.0,vis)
    rgb2=np.where(paint[...,None],vis,rgb2)
    # thin dark edge between visor and rim
    edge=v&~paint
    rgb2=np.where(edge[...,None],rgb2*0.35,rgb2)
    out[y:y+h,x:x+w,:3]=rgb2
    print('frame',idx,'visor px',v.sum(),'closing',it,'teal px',teal.sum())
res=Image.fromarray((np.clip(out,0,1)*255).astype(np.uint8),'RGBA')
res.save(DST,quality=90,method=6)
