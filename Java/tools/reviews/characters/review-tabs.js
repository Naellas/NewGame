(() => {
 const tabs=[...document.querySelectorAll('.review-tabs [role="tab"]')];
 const frames=[...document.querySelectorAll('[role="tabpanel"] iframe')];
 const routes=['characters','movement-physics','dialogue-motion'];
 function activate(index){
  tabs.forEach((tab,i)=>{tab.setAttribute('aria-selected',String(i===index));tab.tabIndex=i===index?0:-1;document.getElementById(tab.getAttribute('aria-controls')).hidden=i!==index;});
  frames.forEach(frame=>{const active=!frame.closest('[role="tabpanel"]').hidden;if(active&&!frame.hasAttribute('src'))frame.src=frame.dataset.src;frame.contentWindow?.postMessage({type:'movement-study-active',active},'*');});
 }
 tabs.forEach((tab,i)=>{
  tab.addEventListener('click',()=>{history.replaceState(null,'','#'+routes[i]);activate(i);});
  tab.addEventListener('keydown',e=>{const index=e.key==='Home'?0:e.key==='End'?tabs.length-1:e.key==='ArrowRight'?(i+1)%tabs.length:e.key==='ArrowLeft'?(i+tabs.length-1)%tabs.length:-1;if(index<0)return;e.preventDefault();tabs[index].click();tabs[index].focus();});
 });
 const fromHash=()=>activate(Math.max(0,routes.indexOf(['movement-study','cloth-physics'].includes(location.hash.slice(1))?'movement-physics':location.hash.slice(1))));window.addEventListener('hashchange',fromHash);
 window.addEventListener('message',e=>{const frame=frames.find(f=>f.contentWindow===e.source);if(!frame||e.data?.type!=='movement-study-size')return;const h=Number(e.data.height);if(Number.isFinite(h)&&h>200)frame.style.height=Math.ceil(Math.min(6000,h+8))+'px';});
 frames.forEach(frame=>frame.addEventListener('load',()=>frame.contentWindow.postMessage({type:'movement-study-active',active:!frame.closest('[role="tabpanel"]').hidden},'*')));
 fromHash();
})();
