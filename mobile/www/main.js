import * as THREE from './three.module.js';
const $=s=>document.querySelector(s),rnd=(a,b)=>a+Math.random()*(b-a);
const R=new THREE.WebGLRenderer({canvas:$('#c'),antialias:true});R.setPixelRatio(Math.min(devicePixelRatio,2));
const S=new THREE.Scene(),C=new THREE.PerspectiveCamera(60,1,.1,200);
const rs=()=>{R.setSize(innerWidth,innerHeight);C.aspect=innerWidth/innerHeight;C.updateProjectionMatrix()};addEventListener('resize',rs);rs();
S.add(new THREE.HemisphereLight(0xaac4ff,0x201040,1.4));const dl=new THREE.DirectionalLight(0xffffff,1.6);dl.position.set(3,8,5);S.add(dl);
let st;try{st=JSON.parse(localStorage.getItem('al'))}catch(e){}st=st||{c:1000,b:{}};
const save=()=>{try{localStorage.setItem('al',JSON.stringify(st))}catch(e){}};
let ac;const beep=(f=440,d=.1,t='sine')=>{try{ac=ac||new AudioContext();const o=ac.createOscillator(),g=ac.createGain();o.type=t;o.frequency.value=f;g.gain.value=.12;g.gain.exponentialRampToValueAtTime(.001,ac.currentTime+d);o.connect(g);g.connect(ac.destination);o.start();o.stop(ac.currentTime+d)}catch(e){}};
const vib=n=>navigator.vibrate&&navigator.vibrate(n);
const bgc=(c,f)=>{S.background=new THREE.Color(c);S.fog=f?new THREE.Fog(c,f[0],f[1]):null};
const rc=new THREE.Raycaster(),v2=new THREE.Vector2();
const pick=(x,y,o)=>{v2.set(x/innerWidth*2-1,-(y/innerHeight)*2+1);rc.setFromCamera(v2,C);return rc.intersectObjects(o,true)[0]};
const idle=new THREE.Group();S.add(idle);
for(let i=0;i<16;i++){const c=[0x9b8cff,0x70f0c2,0xff4d6d,0xffd166][i%4];const m=new THREE.Mesh(new THREE.IcosahedronGeometry(rnd(.3,.8)),new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.4,flatShading:true}));m.position.set(rnd(-6,6),rnd(-6,6),rnd(-8,-1));idle.add(m)}
let cur=null,root=null,score=0,over=true,T=0;
// ---- Skyline Crash: 3D neon-city flyer
const bx=new THREE.BoxGeometry(1,1,1),ed=new THREE.EdgesGeometry(bx),cols=[0x9b8cff,0x70f0c2,0xff4d6d,0xffd166];
const bm=cols.map(c=>new THREE.MeshStandardMaterial({color:0x101830,emissive:c,emissiveIntensity:.25})),lm=cols.map(c=>new THREE.LineBasicMaterial({color:c}));
const tower=(x,z,w,h,k)=>{const m=new THREE.Mesh(bx,bm[k]);m.scale.set(w,h,w);m.position.set(x,h/2,z);m.add(new THREE.LineSegments(ed,lm[k]));root.add(m);return m};
const sky={i:'🚀',n:'Skyline Crash',d:'Fly the neon city. Tap left/right to switch lanes, grab rings to boost your multiplier.',
init(){bgc(0x080b14,[20,80]);C.position.set(0,3.4,6.5);C.lookAt(0,1.2,-12);this.lane=1;this.x=0;this.mul=1;this.bon=0;this.sp=14;this.sd=8;this.o=[];
 this.jet=new THREE.Mesh(new THREE.ConeGeometry(.4,1.6,4),new THREE.MeshStandardMaterial({color:0xffffff,emissive:0x70f0c2,emissiveIntensity:.6}));this.jet.rotation.x=-Math.PI/2;this.jet.position.y=1.5;root.add(this.jet);
 this.grid=new THREE.GridHelper(300,150,0x9b8cff,0x29354a);root.add(this.grid);for(let z=-30;z>-90;z-=14)this.spawn(z)},
spawn(z){const l=Math.random()*3|0;this.o.push({m:tower((l-1)*3,z,2.2,rnd(3,8),Math.random()*4|0),hit:1,x:(l-1)*3});
 if(Math.random()<.7){const rl=(l+1+(Math.random()*2|0))%3,r=new THREE.Mesh(new THREE.TorusGeometry(.7,.12,8,20),new THREE.MeshStandardMaterial({color:0xffd166,emissive:0xffd166,emissiveIntensity:.8}));r.position.set((rl-1)*3,1.5,z-6);root.add(r);this.o.push({m:r,ring:1,x:(rl-1)*3})}
 for(const s of[-1,1])this.o.push({m:tower(s*rnd(7,13),z-rnd(0,8),rnd(2,4),rnd(5,16),Math.random()*4|0)})},
update(dt){this.sp=14+T*.3;this.mul=1+T*.04+this.bon;score+=this.sp*dt*this.mul*.6;
 this.x+=((this.lane-1)*3-this.x)*Math.min(1,dt*10);this.jet.position.x=this.x;C.position.x=this.x*.5;this.grid.position.z=(this.grid.position.z+this.sp*dt)%2;
 this.sd-=this.sp*dt;if(this.sd<=0){this.sd=rnd(11,16);this.spawn(-90)}
 for(const q of this.o){q.m.position.z+=this.sp*dt;const z=q.m.position.z;
  if(q.ring){q.m.rotation.z+=dt*3;if(Math.abs(z)<.8&&Math.abs(q.x-this.x)<1.4){q.got=1;this.bon+=.2;score+=50;beep(880,.12,'triangle');vib(15)}}
  else if(q.hit&&Math.abs(z)<1.6&&Math.abs(q.x-this.x)<1.5)return end('Crashed at '+this.mul.toFixed(2)+'x!')}
 this.o=this.o.filter(q=>{if(q.got||q.m.position.z>12){root.remove(q.m);return false}return true})},
hud(){return this.mul.toFixed(2)+'x · '+Math.floor(score)},
onDown(x){this.lane=x<innerWidth/2?Math.max(0,this.lane-1):Math.min(2,this.lane+1);beep(300,.05)}};
// ---- Neon Sequence: 3D memory pads
const seq={i:'🎵',n:'Neon Sequence',d:'Watch the glowing pads, then repeat the order. Each round grows longer.',
init(){bgc(0x080b14);C.position.set(0,7,6);C.lookAt(0,0,0);this.seq=[];
 this.pads=[0xff4d6d,0x70f0c2,0x9b8cff,0xffd166].map((c,i)=>{const m=new THREE.Mesh(new THREE.BoxGeometry(2.4,.6,2.4),new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.15}));m.position.set((i%2?1:-1)*1.5,0,(i<2?-1:1)*1.5);m.userData.i=i;root.add(m);return m});this.next()},
next(){this.seq.push(Math.random()*4|0);this.pos=0;this.k=0;this.tm=1;this.state='show'},
flash(i,d=.35){this.pads[i].userData.f=d;beep(260+i*130,.25,'triangle')},
update(dt){for(const p of this.pads){const f=p.userData.f||0;p.userData.f=Math.max(0,f-dt);p.material.emissiveIntensity=f>0?1.6:.15;p.position.y=f>0?.3:0}
 if(this.state=='show'){this.tm-=dt;if(this.tm<=0){if(this.k<this.seq.length){this.flash(this.seq[this.k++]);this.tm=.7}else this.state='in'}}},
hud(){return 'Round '+this.seq.length+' · '+Math.floor(score)},
onDown(x,y){if(this.state!='in')return;const h=pick(x,y,this.pads);if(!h)return;const i=h.object.userData.i;this.flash(i,.2);
 if(i!=this.seq[this.pos])return end('Wrong pad! Reached round '+this.seq.length);
 score+=10*this.seq.length;if(++this.pos==this.seq.length)this.next()}};
// ---- Reef Ranger: 3D underwater tapping
const fish=c=>{const g=new THREE.Group(),m=new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.3}),b=new THREE.Mesh(new THREE.SphereGeometry(.6,12,10),m);b.scale.set(1.5,.8,.5);
 const t=new THREE.Mesh(new THREE.ConeGeometry(.4,.7,3),m);t.rotation.z=Math.PI/2;t.position.x=-1.1;g.add(b,t,new THREE.Mesh(new THREE.SphereGeometry(1.2),new THREE.MeshBasicMaterial({visible:false})));return g};
const reef={i:'🐠',n:'Reef Ranger',d:'45 seconds. Tap fish to catch them — gold is worth more, avoid the red puffers!',
init(){bgc(0x04334a,[8,36]);C.position.set(0,0,10);C.lookAt(0,0,0);this.t=45;this.sp=0;this.f=[];
 const sand=new THREE.Mesh(new THREE.PlaneGeometry(80,40),new THREE.MeshStandardMaterial({color:0xc9a66b}));sand.rotation.x=-Math.PI/2;sand.position.y=-6;root.add(sand);
 const p=new Float32Array(180);for(let i=0;i<60;i++){p[i*3]=rnd(-12,12);p[i*3+1]=rnd(-6,8);p[i*3+2]=rnd(-6,6)}
 const g=new THREE.BufferGeometry();g.setAttribute('position',new THREE.BufferAttribute(p,3));this.b=new THREE.Points(g,new THREE.PointsMaterial({color:0xbff6ff,size:.18,transparent:true,opacity:.7}));root.add(this.b)},
spawn(){const r=Math.random(),k=r<.15?'p':r<.35?'g':'n',col=k=='p'?0xff4d6d:k=='g'?0xffd166:[0x70f0c2,0x9b8cff,0x5ec8ff][Math.random()*3|0],f=fish(col),d=Math.random()<.5?1:-1;
 f.position.set(-d*(5.77*C.aspect+2),rnd(-4,4),rnd(-3,1));f.rotation.y=d>0?0:Math.PI;if(k=='p')f.scale.setScalar(1.3);
 f.userData={fish:1,k,v:d*rnd(2,3.5)*(k=='g'?1.7:1),ph:rnd(0,6)};root.add(f);this.f.push(f)},
update(dt){this.t-=dt;if(this.t<=0)return end("Time's up!");this.sp-=dt;if(this.sp<=0){this.sp=rnd(.35,.9);this.spawn()}
 const hw=5.77*C.aspect+3;this.f=this.f.filter(f=>{const u=f.userData;
  if(u.pop){f.scale.multiplyScalar(1+dt*10);u.pop-=dt;if(u.pop<=0){root.remove(f);return false}return true}
  f.position.x+=u.v*dt;f.position.y+=Math.sin(T*3+u.ph)*dt*.6;f.children[1].rotation.y=Math.sin(T*12+u.ph)*.5;
  if(Math.abs(f.position.x)>hw){root.remove(f);return false}return true});
 const a=this.b.geometry.attributes.position;for(let i=0;i<a.count;i++){let y=a.getY(i)+dt*.8;if(y>8)y=-6;a.setY(i,y)}a.needsUpdate=true},
hud(){return Math.ceil(this.t)+'s · '+Math.floor(score)},
onDown(x,y){const h=pick(x,y,this.f.filter(f=>!f.userData.pop));if(!h)return;let f=h.object;while(!f.userData.fish)f=f.parent;const u=f.userData;u.pop=.2;
 if(u.k=='p'){score=Math.max(0,score-15);beep(150,.2,'sawtooth');vib(40)}else{score+=u.k=='g'?30:10;beep(u.k=='g'?900:600,.1,'triangle');vib(10)}}};
// ---- Roulette Practice: 3D wheel, demo chips only (true odds, no real money)
const RED=new Set([1,3,5,7,9,12,14,16,18,19,21,23,25,27,30,32,34,36]);
const EU=[0,32,15,19,4,21,2,25,17,34,6,27,13,36,11,30,8,23,10,5,24,16,33,1,20,14,31,9,22,18,29,7,28,12,35,3,26],US=[0,28,9,26,30,11,7,20,32,17,5,22,34,15,3,24,36,13,1,'00',27,10,25,29,12,8,19,31,18,6,21,33,16,4,23,35,14,2];
const BETS={Red:[n=>RED.has(n),1],Black:[n=>n>0&&!RED.has(n),1],Odd:[n=>n>0&&n%2==1,1],Even:[n=>n>0&&n%2==0,1],'1-18':[n=>n>=1&&n<=18,1],'19-36':[n=>n>=19&&n<=36,1],'1st 12':[n=>n>=1&&n<=12,2],'2nd 12':[n=>n>=13&&n<=24,2],'3rd 12':[n=>n>=25&&n<=36,2]};
const roul={i:'🎡',n:'Roulette Practice',d:'European / American 3D wheel with practice chips. True odds, no real money, no cash-out.',B:BETS,
init(){bgc(0x080b14);C.position.set(0,7,6.5);C.lookAt(0,0,0);this.am=false;this.bets={};this.D=6;this.spinT=6;this.ang=0;this.res=null;this.w=this.ball=null;this.build();this.panel()},
build(){if(this.w)root.remove(this.w);const P=this.P=this.am?US:EU,n=P.length,w=this.w=new THREE.Group();
 P.forEach((v,i)=>{const a=i/n*Math.PI*2,c=v===0||v==='00'?0x1fa35c:RED.has(v)?0xd62839:0x151a26,m=new THREE.Mesh(new THREE.BoxGeometry(.75,.3,.9),new THREE.MeshStandardMaterial({color:c,emissive:c,emissiveIntensity:.3}));m.position.set(Math.cos(a)*3,0,Math.sin(a)*3);m.rotation.y=-a;w.add(m)});
 const h=new THREE.Mesh(new THREE.CylinderGeometry(2.4,2.4,.2,40),new THREE.MeshStandardMaterial({color:0x2a1f10,emissive:0xffd166,emissiveIntensity:.15}));h.position.y=-.1;w.add(h);root.add(w);
 if(!this.ball){this.ball=new THREE.Mesh(new THREE.SphereGeometry(.18,12,12),new THREE.MeshStandardMaterial({color:0xffffff,emissive:0xffffff,emissiveIntensity:.6}));root.add(this.ball)}this.ball.position.set(3.7,.3,0)},
panel(){const p=this.p=document.createElement('div');p.id='rp';p.style.cssText='position:fixed;left:0;right:0;bottom:0;padding:10px 12px calc(10px + env(safe-area-inset-bottom));background:rgba(8,11,20,.88);display:flex;flex-direction:column;gap:8px';
 p.innerHTML='<div id="rm" style="text-align:center;font-weight:800;min-height:20px"></div><div id="rn" style="display:flex;gap:6px;overflow-x:auto"></div><div id="rb" style="display:flex;flex-wrap:wrap;gap:6px"></div><div style="display:flex;gap:8px"><button class="g" id="ra" style="flex:2">European · 2.70% edge</button><button class="g" id="rx" style="flex:1">Clear</button><button class="p" id="rs" style="flex:2">SPIN</button></div>';
 document.body.appendChild(p);const q=s=>p.querySelector(s);this.nums();
 for(const k of Object.keys(BETS)){const b=document.createElement('button');b.className='g';b.textContent=k;b.style.cssText='padding:9px 11px';b.onclick=()=>this.add(k);q('#rb').appendChild(b)}
 q('#rx').onclick=()=>{if(this.spinT>=this.D)this.clear()};q('#rs').onclick=()=>this.go();
 q('#ra').onclick=()=>{if(this.spinT<this.D)return;this.clear();this.am=!this.am;this.res=null;this.build();this.nums();q('#ra').textContent=this.am?'American · 5.26% edge':'European · 2.70% edge'};this.msg()},
nums(){const r=this.p.querySelector('#rn');r.innerHTML='';for(const v of[0,...(this.am?['00']:[]),...Array.from({length:36},(_,i)=>i+1)]){const b=document.createElement('button');b.className='g';b.textContent=v;b.style.cssText='padding:8px 10px;min-width:40px';b.onclick=()=>this.add('n'+v);r.appendChild(b)}},
msg(t){this.p.querySelector('#rm').textContent=t||'Bet '+Object.values(this.bets).reduce((a,b)=>a+b,0)+' · Chips '+st.c+' · 10 per tap'},
add(k){if(this.spinT<this.D)return;if(st.c<10){st.c=1000;this.msg('Practice chips reset to 1000');return}st.c-=10;this.bets[k]=(this.bets[k]||0)+10;save();this.msg();beep(500,.05)},
clear(){for(const b of Object.values(this.bets))st.c+=b;this.bets={};save();this.msg()},
go(){if(this.spinT<this.D||!Object.keys(this.bets).length)return;const r=new Uint32Array(1);crypto.getRandomValues(r);this.res=Math.floor(r[0]/2**32*this.P.length);
 this.tg=this.ang+.6*this.D;this.th0=Math.atan2(this.ball.position.z,this.ball.position.x);this.spinT=0;this.msg('No more bets…');beep(200,.3,'triangle')},
settle(){const v=this.P[this.res];let w=0,s=0;for(const[k,b]of Object.entries(this.bets)){s+=b;const q=k[0]=='n'?[x=>String(x)==k.slice(1),35]:BETS[k];if(q[0](v))w+=b*(q[1]+1)}
 st.c+=w;this.bets={};save();vib(w>s?[30,30,60]:20);beep(w>s?880:220,.2);this.msg(v+' '+(v===0||v==='00'?'green':RED.has(v)?'red':'black')+' · '+(w>=s?'+':'−')+Math.abs(w-s)+' chips · Chips '+st.c)},
update(dt){this.ang+=dt*.6;this.w.rotation.y=-this.ang;
 if(this.spinT<this.D){this.spinT+=dt;const k=Math.min(1,this.spinT/this.D),e=1-Math.pow(1-k,3),a=this.res/this.P.length*Math.PI*2,th=this.th0+(a+this.tg-6*Math.PI-this.th0)*e,r=3.7-.75*e;
  this.ball.position.set(Math.cos(th)*r,.3,Math.sin(th)*r);if(k>=1){this.ball.position.set(Math.cos(a+this.ang)*3,.3,Math.sin(a+this.ang)*3);this.settle()}}
 else if(this.res!=null){const a=this.res/this.P.length*Math.PI*2;this.ball.position.set(Math.cos(a+this.ang)*3,.3,Math.sin(a+this.ang)*3)}},
hud(){return 'Chips '+st.c},onDown(){}};
// ---- shared card helpers (practice card games, demo chips only)
const SU='♠♥♦♣',PS='position:fixed;left:0;right:0;bottom:0;padding:10px 12px calc(10px + env(safe-area-inset-bottom));background:rgba(8,11,20,.88);display:flex;flex-direction:column;gap:8px';
const panel=h=>{const p=document.createElement('div');p.id='rp';p.style.cssText=PS;p.innerHTML=h;document.body.appendChild(p);return p};
const ri=n=>{const a=new Uint32Array(1);crypto.getRandomValues(a);return Math.floor(a[0]/2**32*n)};
const deck=()=>{const d=[];for(let s=0;s<4;s++)for(let r=2;r<=14;r++)d.push({r,s});for(let i=51;i>0;i--){const j=ri(i+1);[d[i],d[j]]=[d[j],d[i]]}return d};
const RN=r=>({11:'J',12:'Q',13:'K',14:'A'}[r]||r),tex={},CG=new THREE.BoxGeometry(1,1.4,.02);
const face=c=>{const k=c.r+'_'+c.s;if(tex[k])return tex[k];const cv=document.createElement('canvas');cv.width=128;cv.height=180;const x=cv.getContext('2d');x.fillStyle='#fff';x.fillRect(0,0,128,180);x.fillStyle=c.s==1||c.s==2?'#d62839':'#151a26';x.font='bold 40px Arial';x.fillText(RN(c.r),8,42);x.font='34px Arial';x.fillText(SU[c.s],10,80);x.font='64px Arial';x.textAlign='center';x.fillText(SU[c.s],64,135);return tex[k]=new THREE.CanvasTexture(cv)};
const back=()=>{if(tex.b)return tex.b;const cv=document.createElement('canvas');cv.width=128;cv.height=180;const x=cv.getContext('2d');x.fillStyle='#2b1d63';x.fillRect(0,0,128,180);x.strokeStyle='#9b8cff';x.lineWidth=4;for(let i=0;i<9;i++){x.strokeRect(10+i*4,10+i*4,108-i*8,160-i*8)}return tex.b=new THREE.CanvasTexture(cv)};
const mkCard=(c,sc=1)=>{const f=new THREE.MeshBasicMaterial({map:face(c)}),b=new THREE.MeshBasicMaterial({map:back()}),e=new THREE.MeshBasicMaterial({color:0xdddddd}),m=new THREE.Mesh(CG,[e,e,e,e,f,b]);m.scale.setScalar(sc);m.rotation.set(-.15,Math.PI,0);root.add(m);return m};
const tw=[],mv=(m,p,ry,d=.4,cb)=>tw.push({m,p,ry,d,t:0,x:m.position.clone(),r:m.rotation.y,cb});
const tws=dt=>{for(let i=tw.length-1;i>=0;i--){const q=tw[i];q.t+=dt;const k=Math.min(1,q.t/q.d),e=k*k*(3-2*k);q.m.position.lerpVectors(q.x,q.p,e);q.m.position.z+=Math.sin(k*Math.PI)*.4;q.m.rotation.y=q.r+(q.ry-q.r)*e;if(k>=1){tw.splice(i,1);q.cb&&q.cb()}}};
const table=()=>{bgc(0x04201a);C.position.set(0,3,7.5);C.lookAt(0,.4,0);tw.length=0;const f=new THREE.Mesh(new THREE.PlaneGeometry(30,30),new THREE.MeshBasicMaterial({color:0x0b3d2e}));f.position.z=-1;root.add(f);const r=new THREE.Mesh(new THREE.TorusGeometry(2.6,.04,8,60),new THREE.MeshBasicMaterial({color:0xffd166}));r.position.set(0,.4,-.9);r.scale.y=1.6;root.add(r)};
// ---- Teen Patti Practice: 3-card showdown vs 2 bots
const HN=['','High card','Pair','Color','Sequence','Pure sequence','Trail'];
const hv=h=>{const r=h.map(c=>c.r).sort((a,b)=>b-a),f=h.every(c=>c.s==h[0].s),tr=r[0]==r[2],sq=r[0]-r[1]==1&&r[1]-r[2]==1,a=r[0]==14&&r[1]==3&&r[2]==2;let t,k;
 if(tr){t=6;k=[r[0]]}else if((sq||a)&&f){t=5;k=[a?13.5:r[0]]}else if(sq||a){t=4;k=[a?13.5:r[0]]}else if(f){t=3;k=r}else if(r[0]==r[1]||r[1]==r[2]){t=2;const p=r[1];k=[p,r.find(x=>x!=p)]}else{t=1;k=r}
 const q=[...k,0,0,0].slice(0,3);return t*1e6+q[0]*2e4+q[1]*2e2+q[2]*2};
const tp={i:'🃏',n:'Teen Patti Practice',d:'3-card showdown vs 2 bots. Fair pot, practice chips only.',
init(){table();this.cs=[];this.s='idle';this.p=panel('<div id="rm" style="text-align:center;font-weight:800;min-height:20px"></div><div style="display:flex;gap:8px"><button class="g" id="tf" style="flex:1">Fold</button><button class="g" id="tr" style="flex:1">Raise 20</button><button class="g" id="ts" style="flex:1">Show</button><button class="p" id="td" style="flex:2">Deal 10</button></div>');
 const q=s=>this.p.querySelector(s);q('#td').onclick=()=>this.deal();q('#tr').onclick=()=>this.raise();q('#ts').onclick=()=>this.show();q('#tf').onclick=()=>this.fold();this.msg('Ante 10 · Chips '+st.c)},
msg(t){this.p.querySelector('#rm').textContent=t},
deal(){if(this.s!='idle')return;this.cs.forEach(c=>root.remove(c));this.cs=[];if(st.c<10)st.c=1000;st.c-=10;save();const d=deck();this.hd=[0,1,2].map(()=>[d.pop(),d.pop(),d.pop()]);this.al=[1,1,1];this.pot=30;this.rz=0;this.s='live';
 const P=[[0,-.9,.9],[-1,2.3,.5],[1,2.3,.5]];this.hd.forEach((h,i)=>h.forEach((c,j)=>{const m=mkCard(c,P[i][2]);m.position.set(0,1.6,0);this.cs.push(m);mv(m,new THREE.Vector3(P[i][0]+(j-1)*P[i][2]*.75,P[i][1],j*.01),i?Math.PI:0,.35+.1*(i*3+j))}));
 beep(400,.1);this.msg('Your hand: '+HN[hv(this.hd[0])/1e6|0]+' · pot '+this.pot)},
raise(){if(this.s!='live'||this.rz)return;if(st.c<20)return this.msg('Not enough chips');this.rz=1;st.c-=20;this.pot+=20;
 for(const i of[1,2])if((hv(this.hd[i])/1e6|0)>=2||ri(10)<5)this.pot+=20;else{this.al[i]=0;for(let j=0;j<3;j++)root.remove(this.cs[i*3+j])}
 if(!this.al[1]&&!this.al[2])return this.fin(this.pot,'Both bots folded');this.msg('Pot '+this.pot+' · tap Show')},
fold(){if(this.s=='live')this.fin(0,'You folded (−10)')},
show(){if(this.s!='live')return;const v=this.hd.map((h,i)=>this.al[i]?hv(h):-1),m=Math.max(...v),w=v.map((x,i)=>x==m?i:-1).filter(i=>i>=0);
 for(const i of[1,2])if(this.al[i])this.cs.slice(i*3,i*3+3).forEach(c=>mv(c,c.position.clone(),0,.5));
 const nm=v.map((x,i)=>x<0?'-':HN[x/1e6|0]).join(' | ');this.fin(w.includes(0)?this.pot/w.length:0,(w.includes(0)?'You win · ':'Bot wins · ')+nm)},
fin(win,t){st.c+=win;save();this.s='idle';this.msg(t+' · Chips '+st.c);beep(win?880:220,.2);vib(win?[30,30,60]:20)},
update(dt){tws(dt)},hud(){return 'Chips '+st.c},onDown(){}};
// ---- Andar Bahar Practice: true-odds side bet, no real money
const PA=0.515006,EA=2.149,EB=3.001;
const ab={i:'🂡',n:'Andar Bahar Practice',d:'Guess the side that matches the joker. Practice chips, edge shown.',
init(){table();this.cs=[];this.b={a:0,b:0};this.s='idle';this.q=[];this.p=panel('<div id="rm" style="text-align:center;font-weight:800;min-height:20px"></div><div style="display:flex;gap:8px"><button class="g" id="ba" style="flex:1">Andar 10<br><small>pays 0.9:1</small></button><button class="g" id="bb" style="flex:1">Bahar 10<br><small>pays 1:1</small></button></div><div style="display:flex;gap:8px"><button class="g" id="bx" style="flex:1">Clear</button><button class="p" id="bd" style="flex:2">DEAL</button></div>');
 const q=s=>this.p.querySelector(s);q('#ba').onclick=()=>this.add('a');q('#bb').onclick=()=>this.add('b');q('#bx').onclick=()=>this.clear();q('#bd').onclick=()=>this.deal();this.msg()},
msg(t){this.p.querySelector('#rm').textContent=t||'Andar '+this.b.a+' · Bahar '+this.b.b+' · Chips '+st.c+' · edge '+EA.toFixed(1)+'% / '+EB.toFixed(1)+'%'},
add(k){if(this.s!='idle')return;if(st.c<10)st.c=1000;st.c-=10;this.b[k]+=10;save();this.msg();beep(500,.05)},
clear(){if(this.s!='idle')return;st.c+=this.b.a+this.b.b;this.b={a:0,b:0};save();this.msg()},
deal(){if(this.s!='idle'||!(this.b.a+this.b.b))return;this.cs.forEach(c=>root.remove(c));this.cs=[];tw.length=0;const d=deck(),j=d.pop();this.q=[];do{this.q.push(d.pop())}while(this.q[this.q.length-1].r!=j.r);
 const m=mkCard(j,.9);m.position.set(0,2.1,0);mv(m,m.position.clone(),0,.5);this.cs.push(m);this.n=0;this.tm=.8;this.s='deal'},
update(dt){tws(dt);if(this.s!='deal')return;this.tm-=dt;if(this.tm>0)return;this.tm=.22;const i=this.n++,c=this.q[i],side=i%2,row=i>>1,m=mkCard(c,.6);m.position.set(0,2.1,0);this.cs.push(m);mv(m,new THREE.Vector3(side?1:-1,1.1-row*.1,row*.01),0,.3);beep(300+side*150,.05);
 if(i==this.q.length-1){this.s='end';const w=side?'b':'a',ret=this.b[w]*(w=='a'?1.9:2);st.c+=ret;const bet=this.b.a+this.b.b;save();this.msg((w=='a'?'ANDAR':'BAHAR')+' wins · '+(ret>=bet?'+':'−')+Math.abs(ret-bet)+' · Chips '+st.c);vib(ret>bet?[30,30,60]:20);this.b={a:0,b:0};this.s='idle'}},
hud(){return 'Chips '+st.c},onDown(){}};
const games={sky,seq,reef,roul,tp,ab};
function menu(){$('#rp')?.remove();cur=null;if(root){S.remove(root);root=null}over=true;idle.visible=true;bgc(0x080b14);C.position.set(0,0,9);C.lookAt(0,0,0);
 $('#hud').hidden=$('#over').hidden=true;$('#menu').hidden=false;$('#chip').textContent='Demo Credits: '+st.c;
 $('#cards').innerHTML=Object.entries(games).map(([k,g])=>`<button class="card" data-g="${k}"><i>${g.i}</i><div><b>${g.n}</b><small>${g.d}</small><em>Best ${st.b[k]||0}</em></div></button>`).join('');
 document.querySelectorAll('.card').forEach(b=>b.onclick=()=>start(b.dataset.g))}
function start(id){$('#rp')?.remove();if(root)S.remove(root);cur=games[id];cur.id=id;root=new THREE.Group();S.add(root);idle.visible=false;score=0;over=false;T=0;
 $('#menu').hidden=$('#over').hidden=true;$('#hud').hidden=false;$('#h1').textContent=cur.n;cur.init()}
function end(msg){if(over)return;over=true;vib([80,40,120]);beep(120,.5,'sawtooth');const s=Math.floor(score),e=Math.floor(s/10);st.c+=e;st.b[cur.id]=Math.max(st.b[cur.id]||0,s);save();
 $('#ot').textContent=msg;$('#os').textContent=s;$('#oe').textContent='+'+e+' demo credits · best '+st.b[cur.id];$('#over').hidden=false}
$('#c').addEventListener('pointerdown',e=>{if(cur&&!over)cur.onDown(e.clientX,e.clientY)});
$('#bk').onclick=$('#mn').onclick=menu;$('#again').onclick=()=>start(cur.id);
const clk=new THREE.Clock();
R.setAnimationLoop(()=>{const dt=Math.min(clk.getDelta(),.05);T+=dt;if(cur&&!over){cur.update(dt);if(!over)$('#h2').textContent=cur.hud()}
 if(idle.visible){idle.rotation.y+=dt*.1;idle.children.forEach((m,i)=>{m.rotation.x+=dt*.5;m.rotation.y+=dt*.3;m.position.y+=Math.sin(T+i)*.003})}R.render(S,C)});
menu();
