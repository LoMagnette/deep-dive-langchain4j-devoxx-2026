/* Application: routing, the pattern catalogue, live runs over SSE, the dock and the
   layout chrome. Rendering primitives live in render.js, which loads first. */

const CAT_LABELS = {"workflow":"Workflows","pure-agent":"Pure agents","pattern-zoo":"Pattern zoo"};
let patterns = [], current = null, es = null;

async function boot(){
  patterns = await (await fetch('/api/patterns')).json();
  const rail = document.getElementById('rail');
  const groups = {};
  patterns.forEach(p => (groups[p.category] ||= []).push(p));
  Object.keys(CAT_LABELS).forEach(cat => {
    if(!groups[cat]) return;
    const h = document.createElement('h3'); h.textContent = CAT_LABELS[cat]; rail.appendChild(h);
    groups[cat].forEach(p => {
      const b = document.createElement('button'); b.textContent = p.name; b.dataset.id = p.id;
      b.onclick = () => navigate(p.id); rail.appendChild(b);
    });
  });
  buildGallery();
  route();
}

/* ---------- routing: #/ is the gallery, #/<id> is one pattern ----------
   Real URLs rather than a JS-only view switch, so Back returns to the grid and a pattern can
   be deep-linked straight from a slide. */
function routeId(){
  const m = /^#\/(.+)$/.exec(location.hash || '');
  return m ? decodeURIComponent(m[1]) : null;
}
function navigate(id){
  const hash = id ? '#/' + encodeURIComponent(id) : '#/';
  if(location.hash !== hash) location.hash = hash;
  route();    // route now too: don't make callers wait on the hashchange event
}
function route(){
  const id = routeId();
  const p = id && patterns.find(x => x.id === id);
  document.body.classList.toggle('on-grid', !p);
  if(p) select(p.id);
  else {
    if(es){ es.close(); es=null; document.getElementById('run').disabled=false; }
    current = null;
    document.querySelectorAll('.rail button').forEach(b => b.classList.remove('active'));
  }
}
window.addEventListener('hashchange', route);

function buildGallery(){
  const grid = document.getElementById('grid');
  grid.innerHTML = '';
  patterns.forEach(p => {
    const a = document.createElement('a');
    a.className = 'card';
    a.href = '#/' + encodeURIComponent(p.id);
    a.dataset.id = p.id;
    a.innerHTML = `<span class="cat ${escapeHtml(p.category)}">${escapeHtml(CAT_LABELS[p.category]||p.category)}</span>`
      + `<h3>${escapeHtml(p.name)}</h3><p>${escapeHtml(p.useful)}</p>`
      + `<svg class="thumb" viewBox="0 0 ${W} ${H}" preserveAspectRatio="xMidYMid meet" aria-hidden="true"></svg>`;
    grid.appendChild(a);
    drawThumb(a.querySelector('.thumb'), p.topology);
  });
}

/** The topology at a glance: shapes and links only, no labels — a signature, not a diagram. */

function select(id){
  if(es){ es.close(); es=null; }
  current = patterns.find(p => p.id===id);
  document.querySelectorAll('.rail button').forEach(b=>b.classList.toggle('active', b.dataset.id===id));
  document.getElementById('p-name').textContent = current.name;
  document.getElementById('p-useful').textContent = current.useful;
  const cav = document.getElementById('p-caveat');
  cav.style.display='block'; cav.innerHTML = '⚠ <b>Caveat:</b> ' + current.caveat;
  document.getElementById('input').value = current.defaultInput || '';
  reset();
  // Result and scope were just cleared, so land on the tab that has something to show.
  showPane('console');
  drawGraph(current.topology);
}

function reset(){
  document.getElementById('console').innerHTML='';
  document.getElementById('scope').innerHTML='<span class="empty">Empty until an agent writes to it.</span>';
  lastScope={}; expandedVars.clear();
  document.getElementById('result').innerHTML='<span class="empty">No run yet.</span>';
  document.getElementById('result-dot').hidden=true;
  document.querySelectorAll('.node').forEach(n=>n.classList.remove('active','done'));
}

/* ---------- run / SSE ---------- */
function log(ev){
  const c=document.getElementById('console');
  const colors={'run-start':'--c-start','agent-before':'--c-before','agent-after':'--c-after','agent-error':'--c-error','run-result':'--c-result','run-done':'--c-done'};
  const div=document.createElement('div'); div.className='line';
  const col=`var(${colors[ev.type]||'--c-done'})`;
  div.innerHTML=`<span class="seq">[${ev.seq}]</span> <span style="color:${col};font-weight:700">${ev.type}</span> <span style="color:var(--accent2)">${ev.agent||''}</span> — <span style="color:${col}">${escapeHtml(ev.message||'')}</span>`;
  c.appendChild(div); c.scrollTop=c.scrollHeight;
}


/* Which rows changed on the last event, so the table reads like a debugger stepping: you can
   see exactly which variable an agent just wrote. Keyed by name -> value. */
let lastScope={};
let expandedVars=new Set();

function updateScope(scope){
  if(!scope) return;
  const keys=Object.keys(scope); if(!keys.length) return;
  const el=document.getElementById('scope');
  const rows=keys.map(k=>{
    const v=scope[k]||{};
    const changed = !(k in lastScope) || lastScope[k]!==v.value;
    const expanded = expandedVars.has(k);
    const type = v.type + (v.size!=null ? '(' + v.size + ')' : '');
    return `<tr class="${changed?'changed ':''}expandable" data-var="${escapeHtml(k)}">`
      + `<td class="name">${escapeHtml(k)}</td>`
      + `<td class="type">${escapeHtml(type)}</td>`
      + `<td class="val${expanded?'':' clamped'}">${escapeHtml(v.value)}</td></tr>`;
  }).join('');
  el.innerHTML = '<table class="vars"><thead><tr><th>name</th><th>type</th><th>value</th></tr>'
    + `</thead><tbody>${rows}</tbody></table>`;
  el.querySelectorAll('tr.expandable').forEach(tr=>tr.onclick=()=>{
    const k=tr.dataset.var;
    if(expandedVars.has(k)) expandedVars.delete(k); else expandedVars.add(k);
    tr.querySelector('.val').classList.toggle('clamped');
  });
  lastScope={}; keys.forEach(k=>lastScope[k]=(scope[k]||{}).value);
}

/* The result is a tab now, so a finished run would otherwise be invisible. Switch to it on
   completion — unless the viewer picked a tab themselves during this run, in which case leave
   them where they are and just flag the tab. */
let tabPinned=false;
function revealResult(){
  if(!tabPinned) showPane('result');
  else if(activePane()!=='result') document.getElementById('result-dot').hidden=false;
}

function run(){
  if(!current) return;
  reset();
  tabPinned=false;
  const input=encodeURIComponent(document.getElementById('input').value||'');
  document.getElementById('run').disabled=true;
  es=new EventSource(`/api/patterns/${current.id}/run?input=${input}`);
  es.onmessage=e=>{
    let ev; try{ ev=JSON.parse(e.data); }catch(_){ return; }
    log(ev); updateScope(ev.scope);
    if(ev.type==='agent-before') markNode(ev.agent,'active');
    else if(ev.type==='agent-after') markNode(ev.agent,'done');
    else if(ev.type==='run-result'){
      document.getElementById('result').innerHTML = ev.data!=null
        ? '<div class="md">' + renderMarkdown(ev.data) + '</div>'
        : '<span class="empty">The run produced no output.</span>';
      revealResult();
    } else if(ev.type==='run-done'){
      es.close(); es=null; document.getElementById('run').disabled=false;
    }
  };
  es.onerror=()=>{ if(es){es.close();es=null;} document.getElementById('run').disabled=false; };
}

/* ---------- bottom dock: run events + live server log ---------- */
const LEVEL_RANK={TRACE:0,DEBUG:1,INFO:2,WARN:3,ERROR:4};
let logLines=[];

const PANES=['result','scope','console','log'];
function showPane(name){
  document.querySelectorAll('.tab').forEach(t=>t.classList.toggle('active', t.dataset.pane===name));
  PANES.forEach(p=>document.getElementById(p).hidden = p!==name);
  /* Per-tab controls: a level filter only means something for the log, and "clear" would be
     meaningless on panes that mirror the current run. */
  document.getElementById('log-level').style.display = name==='log' ? '' : 'none';
  document.getElementById('pane-clear').style.display = (name==='log'||name==='console') ? '' : 'none';
  if(name==='log') document.getElementById('log-dot').hidden = true;
  if(name==='result') document.getElementById('result-dot').hidden = true;
}
function activePane(){ return document.querySelector('.tab.active').dataset.pane; }
function minLevel(){ return document.getElementById('log-level').value; }
function passes(l){ const m=minLevel(); return m==='ALL' || (LEVEL_RANK[l.level]??2) >= LEVEL_RANK[m]; }

function logHtml(l){
  return `<div class="line"><span class="time">${l.time}</span> `
    + `<span class="lvl-${l.level}">${String(l.level).padEnd(5)}</span> `
    + `<span class="logger">${escapeHtml(l.logger)}</span> ${escapeHtml(l.message)}</div>`;
}
function renderLog(){
  const el=document.getElementById('log');
  el.innerHTML = logLines.filter(passes).map(logHtml).join('');
  el.scrollTop = el.scrollHeight;
}
function appendLog(l){
  logLines.push(l);
  if(logLines.length>1000) logLines.shift();
  /* A problem you can't see is a problem you debug on stage: flag warnings on the tab. */
  if((l.level==='WARN'||l.level==='ERROR') && activePane()!=='log')
    document.getElementById('log-dot').hidden=false;
  if(!passes(l)) return;
  const el=document.getElementById('log');
  const atBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 40;
  el.insertAdjacentHTML('beforeend', logHtml(l));
  if(atBottom) el.scrollTop = el.scrollHeight;
}
function connectLogs(){
  /* EventSource reconnects by itself, which also covers a dev-mode restart. */
  const src=new EventSource('/api/logs');
  src.onmessage=e=>{ let l; try{ l=JSON.parse(e.data); }catch(_){ return; } appendLog(l); };
}

document.querySelectorAll('.tab').forEach(t=>t.onclick=()=>{ tabPinned=true; showPane(t.dataset.pane); });
document.getElementById('log-level').onchange=renderLog;
document.getElementById('pane-clear').onclick=()=>{
  if(activePane()==='log'){ logLines=[]; renderLog(); }
  else document.getElementById('console').innerHTML='';
};

/* ---------- layout chrome: collapsible rail, resizable dock ----------
   Both remembered in localStorage: the speaker arranges the room's view once, and a reload
   (or a dev-mode restart mid-talk) doesn't undo it. */
const DOCK_MIN=120, STAGE_MIN=260, PREFS='dashboard.layout';

function loadPrefs(){ try{ return JSON.parse(localStorage.getItem(PREFS)) || {}; }catch(_){ return {}; } }
function savePrefs(patch){ try{ localStorage.setItem(PREFS, JSON.stringify({...loadPrefs(), ...patch})); }catch(_){} }

/* Prefer the height we last set over a measured rect: repeated drags would otherwise accumulate
   sub-pixel drift, and before the first resize there is no inline value to read. */
function dockHeight(){
  const dock=document.getElementById('dock');
  const inline=parseFloat(dock.style.height);
  return Number.isFinite(inline) ? inline : dock.getBoundingClientRect().height;
}
function dockMax(){ return Math.max(DOCK_MIN, window.innerHeight - STAGE_MIN); }
function setDockHeight(px, persist){
  const h = Math.round(Math.min(dockMax(), Math.max(DOCK_MIN, px)));
  document.getElementById('dock').style.height = h + 'px';
  if(persist) savePrefs({dock:h});
  return h;
}
function prefersDark(){
  try{ return window.matchMedia && matchMedia('(prefers-color-scheme: dark)').matches; }
  catch(_){ return false; }
}
function currentTheme(){ return loadPrefs().theme || (prefersDark() ? 'dark' : 'light'); }
function applyTheme(theme){
  document.documentElement.setAttribute('data-theme', theme);
  // aria-pressed doubles as the styling hook, so the switch can never show the wrong side lit.
  document.querySelectorAll('[data-theme-choice]').forEach(b =>
    b.setAttribute('aria-pressed', String(b.dataset.themeChoice === theme)));
}
document.querySelectorAll('[data-theme-choice]').forEach(b => b.onclick = () => {
  savePrefs({theme:b.dataset.themeChoice});
  applyTheme(b.dataset.themeChoice);
});

function setRailHidden(hidden, persist){
  document.body.classList.toggle('rail-hidden', hidden);
  document.getElementById('rail-toggle').setAttribute('aria-expanded', String(!hidden));
  if(persist) savePrefs({railHidden:hidden});
}

document.getElementById('rail-toggle').onclick = () =>
  setRailHidden(!document.body.classList.contains('rail-hidden'), true);

(function dockResizing(){
  const grip=document.getElementById('grip');
  let startY=0, startH=0, active=false;
  grip.addEventListener('pointerdown', e=>{
    active=true; startY=e.clientY; startH=dockHeight();
    grip.setPointerCapture?.(e.pointerId);
    document.body.classList.add('resizing');
    e.preventDefault();
  });
  grip.addEventListener('pointermove', e=>{
    if(active) setDockHeight(startH + (startY - e.clientY), false);   // drag up = taller
  });
  const stop = e => {
    if(!active) return;
    active=false;
    document.body.classList.remove('resizing');
    try{ grip.releasePointerCapture?.(e.pointerId); }catch(_){}
    savePrefs({dock:Math.round(dockHeight())});
  };
  grip.addEventListener('pointerup', stop);
  grip.addEventListener('pointercancel', stop);
  grip.addEventListener('dblclick', ()=> setDockHeight(window.innerHeight*0.34, true));
  grip.addEventListener('keydown', e=>{
    const step = e.shiftKey ? 60 : 15;
    if(e.key==='ArrowUp') setDockHeight(dockHeight()+step, true);
    else if(e.key==='ArrowDown') setDockHeight(dockHeight()-step, true);
    else return;
    e.preventDefault();
  });
  // A shrinking window must never let the dock swallow the diagram.
  window.addEventListener('resize', ()=> setDockHeight(dockHeight(), false));
})();

(function applySavedLayout(){
  const p=loadPrefs();
  applyTheme(currentTheme());
  if(typeof p.dock === 'number') setDockHeight(p.dock, false);
  setRailHidden(!!p.railHidden, false);
})();

document.getElementById('run').onclick=run;
document.getElementById('reset').onclick=()=>{ if(es){es.close();es=null;} document.getElementById('run').disabled=false; reset(); };
boot();
connectLogs();
