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
const games={sky,seq,reef};
function menu(){cur=null;if(root){S.remove(root);root=null}over=true;idle.visible=true;bgc(0x080b14);C.position.set(0,0,9);C.lookAt(0,0,0);
 $('#hud').hidden=$('#over').hidden=true;$('#menu').hidden=false;$('#chip').textContent='Demo Credits: '+st.c;
 $('#cards').innerHTML=Object.entries(games).map(([k,g])=>`<button class="card" data-g="${k}"><i>${g.i}</i><div><b>${g.n}</b><small>${g.d}</small><em>Best ${st.b[k]||0}</em></div></button>`).join('');
 document.querySelectorAll('.card').forEach(b=>b.onclick=()=>start(b.dataset.g))}
function start(id){if(root)S.remove(root);cur=games[id];cur.id=id;root=new THREE.Group();S.add(root);idle.visible=false;score=0;over=false;T=0;
 $('#menu').hidden=$('#over').hidden=true;$('#hud').hidden=false;$('#h1').textContent=cur.n;cur.init()}
function end(msg){if(over)return;over=true;vib([80,40,120]);beep(120,.5,'sawtooth');const s=Math.floor(score),e=Math.floor(s/10);st.c+=e;st.b[cur.id]=Math.max(st.b[cur.id]||0,s);save();
 $('#ot').textContent=msg;$('#os').textContent=s;$('#oe').textContent='+'+e+' demo credits · best '+st.b[cur.id];$('#over').hidden=false}
$('#c').addEventListener('pointerdown',e=>{if(cur&&!over)cur.onDown(e.clientX,e.clientY)});
$('#bk').onclick=$('#mn').onclick=menu;$('#again').onclick=()=>start(cur.id);
const clk=new THREE.Clock();
R.setAnimationLoop(()=>{const dt=Math.min(clk.getDelta(),.05);T+=dt;if(cur&&!over){cur.update(dt);if(!over)$('#h2').textContent=cur.hud()}
 if(idle.visible){idle.rotation.y+=dt*.1;idle.children.forEach((m,i)=>{m.rotation.x+=dt*.5;m.rotation.y+=dt*.3;m.position.y+=Math.sin(T+i)*.003})}R.render(S,C)});
menu();
