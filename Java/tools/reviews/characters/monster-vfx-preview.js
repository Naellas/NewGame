// VFX strips are exported by the same Java function used during battle.
const monsterVfxImages=new Map();
function createMonsterVfxPreview(figure,name){
 const entries=window.monsterVfx?.[name]||[];
 const controls=document.createElement('p'),label=document.createElement('label'),enabled=document.createElement('input'),select=document.createElement('select'),status=document.createElement('small');
 enabled.type='checkbox';enabled.checked=true;label.style.margin='0';label.append(enabled,' Ability VFX');
 select.style.cssText='display:block;width:100%;margin-top:8px';select.setAttribute('aria-label',name.replaceAll('_',' ')+' ability');
 for(const [i,entry] of entries.entries())select.add(new Option(entry.name,i));
 controls.append(label,select,status);figure.append(controls);
 const state={enabled,select,entries,image:new Image(),entry:null};
 function change(){state.entry=entries[Number(select.value)];if(!state.entry){enabled.checked=false;enabled.disabled=true;status.textContent='No VFX data';return;}
  if(!monsterVfxImages.has(state.entry.id)){const image=new Image();image.src='monster-vfx/'+state.entry.id+'.png';monsterVfxImages.set(state.entry.id,image);}
  state.image=monsterVfxImages.get(state.entry.id);status.textContent=state.entry.motion==='SELF'?'Self buff':state.entry.motion==='MELEE'?'Contact hit on player':'Projectile toward player';
 }
 state.image.onerror=()=>{status.textContent='Could not load effect preview';};
 select.onchange=change;change();return state;
}
function drawMonsterVfxPreview(ctx,monster,state,t){
 const self=state.entry?.motion==='SELF';
 const pose=self?0:Math.min(11,Math.floor((t<.4?7/12*t/.4:t<.6?7/12+2/12*(t-.4)/.2:.75+.25*(t-.6)/.4)*12));
 ctx.fillStyle='#93a7b6';ctx.font='12px system-ui';ctx.textAlign='center';ctx.fillText('Player team',80,286);ctx.fillText('Monster',245,286);
 ctx.strokeStyle='#93a7b6';ctx.lineWidth=2;ctx.beginPath();ctx.arc(80,205,25,0,Math.PI*2);ctx.moveTo(45,205);ctx.lineTo(115,205);ctx.moveTo(80,170);ctx.lineTo(80,240);ctx.stroke();
 const width=monster.naturalWidth/12;
 ctx.drawImage(monster,pose*width,0,width,monster.naturalHeight,175,140,140,140);
 if(state.image.complete&&state.image.naturalWidth){const frame=Math.min(23,Math.floor(t*24)),size=state.image.naturalHeight;ctx.drawImage(state.image,frame*size,0,size,size,0,0,320,320);}
 ctx.fillStyle='#bec7cc';ctx.fillText(t<.4?'Wind-up':t<.6?'Release':'Impact / recovery',160,35);
 ctx.textAlign='start';
}
