/* Application: routing, the pattern catalogue, live runs over SSE, the dock and the
   layout chrome. Rendering primitives live in render.js, which loads first. */

const CAT_LABELS = {"workflow":"Workflows","pure-agent":"Pure agents",
                    "pattern-zoo":"Pattern zoo","composite":"Putting it together",
                    "production":"Running it for real"};
/* One line per group, in the talk's own words (see the through-line diagram in the root README).
   The gallery separates the categories physically instead of tagging every card, and a heading
   that says what the group MEANS is the reason the separation is worth having — otherwise it is
   just the same cards with more whitespace. */
const CAT_NOTES = {"workflow":"You decide the path",
                   "pure-agent":"The model decides the path",
                   "pattern-zoo":"The middle ground — a planner decides the turns",
                   "composite":"Several patterns wired into one system",
                   /* Not a position on the dial — a modifier you can bolt onto any of the above,
                      which is why this group sits outside the ordering rather than inside it. */
                   "production":"Not where on the dial — what it takes to run it"};
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
  const n = patterns.filter(p => p.category !== 'composite').length;
  const composites = patterns.length - n;
  document.querySelector('.gallery-head h2').textContent = `${n} agentic patterns`;
  document.querySelector('.gallery-head p').textContent = composites
    ? `Each one runs live against a real model — plus ${composites === 1 ? 'a system that combines'
        : composites + ' systems that combine'} them. Pick one to try it.`
    : 'Every one of them runs live against a real model. Pick one to try it.';
  /* Grouped into sections rather than a flat grid with a category chip on every card. The chip
     made the reader do the sorting that the layout can do for them, and it competed with the
     pattern's own name for the top-left of the card. Same categories and same order as the rail,
     so moving between the two does not re-teach the arrangement. */
  const groups = document.getElementById('grid');
  groups.innerHTML = '';
  const byCat = {};
  patterns.forEach(p => (byCat[p.category] ||= []).push(p));
  // CAT_LABELS first (the talk's order), then anything a future category adds, so a new
  // category appears in the gallery even before it is named here.
  const order = [...Object.keys(CAT_LABELS), ...Object.keys(byCat)]
    .filter((c, i, a) => byCat[c] && a.indexOf(c) === i);

  order.forEach(cat => {
    const section = document.createElement('section');
    section.className = 'group';
    const head = document.createElement('div');
    head.className = 'group-head';
    head.innerHTML = `<span class="dot ${escapeHtml(cat)}"></span>`
      + `<h3>${escapeHtml(CAT_LABELS[cat] || cat)}</h3>`
      + (CAT_NOTES[cat] ? `<span class="note">${escapeHtml(CAT_NOTES[cat])}</span>` : '')
      + `<span class="count">${byCat[cat].length}</span>`;
    section.appendChild(head);
    const cards = document.createElement('div');
    cards.className = 'cards';
    section.appendChild(cards);
    groups.appendChild(section);

    byCat[cat].forEach(p => {
      const a = document.createElement('a');
      a.className = 'card';
      a.href = '#/' + encodeURIComponent(p.id);
      a.dataset.id = p.id;
      /* The card shows the STORY, not the `useful` line: scanned top to bottom the gallery is
         then the narration itself, and the tester page carries the explanation. */
      a.innerHTML = `<h3>${escapeHtml(p.name)}</h3>`
        + `<p class="story">${escapeHtml(p.story || p.useful)}</p>`
        + `<svg class="thumb" viewBox="0 0 ${W} ${H}" preserveAspectRatio="xMidYMid meet" aria-hidden="true"></svg>`;
      cards.appendChild(a);
      drawThumb(a.querySelector('.thumb'), p.topology);
    });
  });
}

/** The topology at a glance: shapes and links only, no labels — a signature, not a diagram. */

function select(id){
  if(es){ es.close(); es=null; }
  current = patterns.find(p => p.id===id);
  document.querySelectorAll('.rail button').forEach(b=>b.classList.toggle('active', b.dataset.id===id));
  document.getElementById('p-name').textContent = current.name;
  document.getElementById('p-story').textContent = current.story || '';
  const builds = document.getElementById('p-builds');
  builds.hidden = !current.buildsOn;
  if(current.buildsOn) builds.innerHTML = '<b>Builds on</b> · ' + escapeHtml(current.buildsOn);
  /* Rendered, not set as text: these two carry **bold** and `code` that used to show as
     literal asterisks and backticks. renderMarkdown escapes before it introduces any tag, so
     catalogue prose cannot inject markup. */
  document.getElementById('p-useful').innerHTML = renderMarkdown(current.useful);
  /* Walking the story: neighbours in catalogue order, which is the order the narration is
     written for. The ends simply have no link rather than a dead one. */
  const at = patterns.findIndex(p => p.id === id);
  const step = (el, p, arrow) => {
    el.textContent = p ? (arrow === '←' ? '← ' + p.name : p.name + ' →') : '';
    el.href = p ? '#/' + encodeURIComponent(p.id) : '#/';
  };
  step(document.getElementById('p-prev'), patterns[at - 1], '←');
  step(document.getElementById('p-next'), patterns[at + 1], '→');
  /* Closed by default, but if you opened it once you are probably comparing patterns — so it
     behaves like the dock and the rail and stays how you left it. */
  document.getElementById('p-notes').open = !!loadPrefs().notesOpen;
  document.getElementById('p-caveat').innerHTML =
      '<b>⚠ Caveat</b>' + renderMarkdown(current.caveat);
  document.getElementById('input').value = current.defaultInput || '';
  /* Offered only where it is honoured. Unchecked on every navigation on purpose: the toggle
     changes which agent runs, and inheriting it from the pattern you were just looking at is
     the kind of surprise you do not want on a projector. */
  document.getElementById('p-stream-wrap').hidden = !current.streams;
  document.getElementById('p-stream').checked = false;
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
  streamed='';
  document.getElementById('p-time').hidden=true;
  document.querySelectorAll('.node').forEach(n=>n.classList.remove('active','done'));
}

/* ---------- run / SSE ---------- */
/* Times are the cheapest observability there is, and the one number that makes a parallel step
   argue for itself. Rounded the way a reader thinks: milliseconds until it stops being useful. */
function fmtMs(ms){
  if(ms==null) return '';
  if(ms < 1000) return ms + ' ms';
  return (ms/1000).toFixed(ms < 10000 ? 1 : 0) + ' s';
}

/* The whole run, and beside it how long the agents were busy in total. The gap between the two
   IS the parallelism — two 900ms branches inside a 950ms run — so they are shown together or
   the number means very little on its own. */
function showRuntime(totalMs){
  const badge=document.getElementById('p-time');
  if(totalMs==null){ badge.hidden=true; return; }
  const busy = agentMsSum > 0
    ? `<span>agents busy ${fmtMs(agentMsSum)}</span>` : '';
  badge.innerHTML = `<b>${fmtMs(totalMs)}</b>${busy}`;
  badge.title = agentMsSum > 0
    ? `The whole run took ${fmtMs(totalMs)}. The agents inside it were busy for `
      + `${fmtMs(agentMsSum)} altogether — the difference is work that overlapped.`
    : `The whole run took ${fmtMs(totalMs)}.`;
  badge.hidden = false;
}

function log(ev){
  /* Tokens are the Result pane's business, not the event log's: one line per chunk is forty
     lines that say nothing about the shape of the run, and it buries the six that do. */
  if(ev.type==='token') return;
  const c=document.getElementById('console');
  const colors={'run-start':'--c-start','agent-before':'--c-before','agent-after':'--c-after','agent-error':'--c-error','human-ask':'--c-result','human-answer':'--c-after','run-result':'--c-result','run-done':'--c-done'};
  const div=document.createElement('div'); div.className='line';
  const col=`var(${colors[ev.type]||'--c-done'})`;
  const took = ev.millis==null ? '' : `<span class="took">${fmtMs(ev.millis)}</span>`;
  div.innerHTML=`<span class="seq">[${ev.seq}]</span> <span style="color:${col};font-weight:700">${ev.type}</span> <span style="color:var(--c-agent)">${ev.agent||''}</span> — <span style="color:${col}">${escapeHtml(ev.message||'')}</span>${took}`;
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

/* The id the server gave this run, so an answer can be posted back against it: the SSE stream
   is one-way, so the human's reply cannot travel down the pipe the question came from. */
let runId=null;
let agentMsSum=0;
/* What has arrived so far on a streaming run, so each token appends instead of replacing. */
let streamed='';

function showAsk(question){
  const box=document.getElementById('ask');
  document.getElementById('ask-q').textContent=question;
  const text=document.getElementById('ask-text');
  text.value=''; box.hidden=false; text.focus();
}
function hideAsk(){ document.getElementById('ask').hidden=true; }

async function sendAnswer(){
  const text=document.getElementById('ask-text').value.trim();
  if(!text || !runId) return;
  hideAsk();
  try{
    await fetch(`/api/patterns/runs/${encodeURIComponent(runId)}/answer?text=`
      + encodeURIComponent(text), {method:'POST'});
  }catch(_){ /* the run times out on its own; nothing useful to say here */ }
}

function run(){
  if(!current) return;
  reset();
  hideAsk();
  runId=null;
  tabPinned=false;
  const input=encodeURIComponent(document.getElementById('input').value||'');
  const wantsTokens = current.streams && document.getElementById('p-stream').checked;
  streamed=''; document.getElementById('run').disabled=true;
  es=new EventSource(`/api/patterns/${current.id}/run?input=${input}`
      + (wantsTokens ? '&stream=true' : ''));
  es.onmessage=e=>{
    let ev; try{ ev=JSON.parse(e.data); }catch(_){ return; }
    log(ev); updateScope(ev.scope);
    if(ev.type==='run-start'){ runId=ev.data||null; agentMsSum=0; }
    else if(ev.type==='human-ask'){ showAsk(ev.message); markNode(ev.agent,'active'); }
    else if(ev.type==='human-answer'){ hideAsk(); markNode(ev.agent,'done'); }
    else if(ev.type==='agent-before') markNode(ev.agent,'active');
    /* Tokens land as TEXT, not markdown: a half-arrived answer is usually half-way through a
       construct (an unclosed ** or a dangling list item), and re-rendering markdown on every
       chunk makes the pane flicker between two layouts. run-result replaces it with the
       rendered version once the whole thing is there. */
    else if(ev.type==='token'){
      streamed += (ev.data==null ? '' : ev.data);
      const pane=document.getElementById('result');
      pane.innerHTML='<pre class="streaming"></pre>';
      pane.firstChild.textContent=streamed;
      revealResult();
    }
    else if(ev.type==='agent-after'){
      /* Summed, not wall-clock: printed next to the run total, the gap between the two IS the
         parallelism. Two 900ms branches in a 950ms run is the whole lesson in two numbers. */
      if(ev.millis!=null) agentMsSum += ev.millis;
      markNode(ev.agent,'done', ev.millis==null?null:fmtMs(ev.millis));
    }
    else if(ev.type==='run-result'){
      document.getElementById('result').innerHTML = ev.data!=null
        ? '<div class="md">' + renderMarkdown(ev.data) + '</div>'
        : '<span class="empty">The run produced no output.</span>';
      revealResult();
    } else if(ev.type==='run-done'){
      hideAsk();
      showRuntime(ev.millis);
      es.close(); es=null; document.getElementById('run').disabled=false;
    }
  };
  es.onerror=()=>{ if(es){es.close();es=null;} hideAsk();
    document.getElementById('run').disabled=false; };
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
  /* What went to the model and what came back is the half of the log worth projecting; the
     framework's own lines are the scaffolding around it. Same stream, different weight. */
  const chat = l.logger==='chat' ? ' chat' : '';
  return `<div class="line${chat}"><span class="time">${l.time}</span> `
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

/* The diagram is a viewBox scaled to fit its pane, so dragging the dock, collapsing the rail or
   resizing the window all change how far it is scaled down — and with it how big the edge labels
   land on screen. Re-fit them rather than redraw: a redraw would throw away which nodes are
   mid-run, and mid-talk that is the one thing on the screen worth keeping. */
(function keepEdgeLabelsReadable(){
  const wrap=document.querySelector('.graphwrap');
  const svg=document.getElementById('graph');
  if(!wrap || !svg || typeof ResizeObserver !== 'function') return;
  new ResizeObserver(()=>fitEdgeLabels(svg)).observe(wrap);
})();

document.getElementById('run').onclick=run;
document.getElementById('p-notes').addEventListener('toggle', e =>
  savePrefs({notesOpen: e.target.open}));
document.getElementById('ask-send').onclick=sendAnswer;
document.getElementById('ask-text').addEventListener('keydown', e=>{
  if(e.key==='Enter'){ e.preventDefault(); sendAnswer(); }
});
document.getElementById('reset').onclick=()=>{ if(es){es.close();es=null;} hideAsk();
  document.getElementById('run').disabled=false; reset(); };
boot();
connectLogs();
