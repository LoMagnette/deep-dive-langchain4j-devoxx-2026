/* Application: routing, the pattern catalogue, live runs over SSE, the dock and the
   layout chrome. Rendering primitives live in render.js, which loads first. */

/* The spec's four acts, in its running order, then the two groups outside the acts. The rail
   and the gallery both group by these, so missions 1 and 8 sit together in Act 1 without the
   catalogue (or the package numbers) being reordered. */
const CAT_LABELS = {"classic":"Before the pack","team":"Act 1 · Meet the team","workflow":"Act 2 · Workflows",
                    "planner":"Act 3 · Planners","minds":"Act 4 · Many minds, custom brains",
                    "composite":"The Mega Mutt","production":"Running it for real"};
/* One line per group, in the talk's own words (see the through-line diagram in the root README).
   The gallery separates the categories physically instead of tagging every card, and a heading
   that says what the group MEANS is the reason the separation is worth having — otherwise it is
   just the same cards with more whitespace. */
const CAT_NOTES = {"classic":"A plain AI service: tools and guardrails, no agentic system",
                   "team":"An agent is a pup with a job — and some don't need a brain",
                   "workflow":"You decide the order — Rangers combine into the Mega Mutt",
                   "planner":"The system decides: Zao, a plan, or the pups themselves",
                   "minds":"You can write the rules yourself",
                   "composite":"Several missions wired into one bigger Ranger",
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
  const n = patterns.filter(p => p.category !== 'composite' && p.category !== 'classic').length;
  const composites = patterns.filter(p => p.category === 'composite').length;
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
      /* Read top to bottom: which mission, what the pattern IS (name and gist — enough for
         someone who has not heard the talk), its shape, and then the story beat, quieter, below
         a rule. The story alone used to carry the card, and a grid of dog anecdotes did not say
         which card was a fan-out and which a planner. The mission number is the index in the
         catalogue, which is also the spec's mission number and the package's _NN_. */
      const mission = patterns.indexOf(p);
      a.innerHTML = `<span class="mission">Mission ${mission}</span>`
        + `<h3>${escapeHtml(p.name)}</h3>`
        + (p.gist ? `<p class="gist">${escapeHtml(p.gist)}</p>` : '')
        + `<svg class="thumb" viewBox="0 0 ${W} ${H}" preserveAspectRatio="xMidYMid meet" aria-hidden="true"></svg>`
        + `<p class="story">${escapeHtml(p.story || p.useful)}</p>`;
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
  // Result was just cleared, so each half lands on the tab that will fill as the run goes.
  showPane('scope');
  showPane('console');
  drawGraph(current.topology);
  zoomReset();   // a new diagram starts fitted, and the zoom readout must say so
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
  const colors={'run-start':'--c-start','agent-before':'--c-before','agent-after':'--c-after','agent-error':'--c-error','human-ask':'--c-result','human-answer':'--c-after','tool-call':'--c-tool','tool-result':'--c-tool','guardrail':'--c-tool','run-result':'--c-result','run-done':'--c-done'};
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
  else if(activePane('top')!=='result') document.getElementById('result-dot').hidden=false;
  /* Never switch views for the viewer — on the diagram they are usually pointing at the timings
     a run just left behind. Flag the Data button instead. */
  if(currentView()!=='data') document.getElementById('data-dot').hidden=false;
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
    const res = await fetch(`/api/patterns/runs/${encodeURIComponent(runId)}/answer?text=`
      + encodeURIComponent(text), {method:'POST'});
    /* 409 means the run had stopped waiting — almost always the three-minute timeout expiring
       while the answer was being typed. The run then carries on with "not approved", so an
       answer that silently went nowhere is exactly the thing not to swallow: it makes the demo
       look like it ignored the person, which is the one claim this pattern cannot afford. */
    if(!res.ok) answerWentNowhere();
  }catch(_){ answerWentNowhere(); }
}

/* Reported into the Run events pane rather than back into the ask box: the run is still going
   and will hide that box on run-done, so a note there would flash and vanish. */
function answerWentNowhere(){
  log({seq:'·', type:'human-answer', agent:'you',
       message:'that answer did not reach the run — it had already stopped waiting, so the run '
             + 'treated the question as unanswered'});
}

function run(){
  if(!current) return;
  reset();
  hideAsk();
  runId=null;
  tabPinned=false;
  // The result was just cleared; the scope is what fills while the run is going.
  showPane('scope');
  document.getElementById('data-dot').hidden=true;
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
    /* A Ranger reaching for his gear: the tool's box is labelled "sniff(place)", whose leading
       token is the tool name, so the message's name lights it — the model's choice, made visible. */
    else if(ev.type==='tool-call') markNode(String(ev.message||'').split('(')[0],'active');
    else if(ev.type==='tool-result') markNode(String(ev.message||'').split(' ')[0],'done', ev.millis);
    /* Mission 0's guardrails report under their class name, which is their box's label. */
    else if(ev.type==='guardrail') markNode(ev.agent,'done', ev.millis);
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

/* ---------- the data view: result / scope over run events / server log ---------- */
const LEVEL_RANK={TRACE:0,DEBUG:1,INFO:2,WARN:3,ERROR:4};
let logLines=[];

/* Two halves, each with its own tabs: what the run produced (top) and how it got there
   (bottom). Showing a pane only switches the half it belongs to. */
const GROUPS={top:['result','scope'], bottom:['console','log']};
function groupOf(name){ return GROUPS.top.includes(name) ? 'top' : 'bottom'; }
function showPane(name){
  const group=groupOf(name);
  document.querySelectorAll(`.tab[data-group="${group}"]`).forEach(t=>
    t.classList.toggle('active', t.dataset.pane===name));
  GROUPS[group].forEach(p=>document.getElementById(p).hidden = p!==name);
  /* Per-tab controls: a level filter only means something for the log, and "clear" would be
     meaningless on panes that mirror the current run. */
  if(group==='bottom'){
    document.getElementById('log-level').style.display = name==='log' ? '' : 'none';
    document.getElementById('pane-clear').style.display = '';
  }
  if(name==='log') document.getElementById('log-dot').hidden = true;
  if(name==='result') document.getElementById('result-dot').hidden = true;
}
function activePane(group){
  return document.querySelector(`.tab.active[data-group="${group}"]`).dataset.pane;
}
/* Whether the viewer can actually see a pane right now: its tab, AND the data view. */
function paneVisible(name){ return currentView()==='data' && activePane(groupOf(name))===name; }

/* ---------- the stage: diagram or data, one at a time ---------- */
function currentView(){ return document.getElementById('view-data').hidden ? 'diagram' : 'data'; }
function setView(view, persist){
  document.getElementById('view-diagram').hidden = view!=='diagram';
  document.getElementById('view-data').hidden = view!=='data';
  document.querySelectorAll('.view').forEach(b=>{
    const on = b.dataset.view===view;
    b.classList.toggle('active', on);
    b.setAttribute('aria-selected', String(on));
  });
  if(view==='data') document.getElementById('data-dot').hidden = true;
  if(persist) savePrefs({view});
}
document.querySelectorAll('.view').forEach(b=>b.onclick=()=>setView(b.dataset.view, true));
/* V flips the stage — handy with a clicker in one hand. Ignored while typing, so an input
   containing a "v" stays typeable. */
document.addEventListener('keydown', e=>{
  if(e.key!=='v' && e.key!=='V') return;
  if(e.ctrlKey || e.metaKey || e.altKey) return;
  if(e.target.closest && e.target.closest('input,textarea,select,[contenteditable]')) return;
  if(document.body.classList.contains('on-grid')) return;
  setView(currentView()==='data' ? 'diagram' : 'data', true);
});
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
  if((l.level==='WARN'||l.level==='ERROR') && !paneVisible('log')){
    if(activePane('bottom')!=='log') document.getElementById('log-dot').hidden=false;
    if(currentView()!=='data') document.getElementById('data-dot').hidden=false;
  }
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

/* Only a choice in the TOP half pins it: picking the server log says nothing about whether you
   want the result shown when the run ends. */
document.querySelectorAll('.tab').forEach(t=>t.onclick=()=>{
  if(t.dataset.group==='top') tabPinned=true;
  showPane(t.dataset.pane);
});
document.getElementById('log-level').onchange=renderLog;
document.getElementById('pane-clear').onclick=()=>{
  if(activePane('bottom')==='log'){ logLines=[]; renderLog(); }
  else document.getElementById('console').innerHTML='';
};

/* ---------- layout chrome: collapsible rail, theme, view, data split ----------
   All remembered in localStorage: the speaker arranges the room's view once, and a reload
   (or a dev-mode restart mid-talk) doesn't undo it. */
const PREFS='dashboard.layout', SPLIT_MIN=0.15, SPLIT_MAX=0.85, SPLIT_DEFAULT=0.45;

function loadPrefs(){ try{ return JSON.parse(localStorage.getItem(PREFS)) || {}; }catch(_){ return {}; } }
function savePrefs(patch){ try{ localStorage.setItem(PREFS, JSON.stringify({...loadPrefs(), ...patch})); }catch(_){} }

/* The bottom half's share of the data view, as a fraction. A fraction rather than pixels, so a
   window resize or a browser zoom keeps the proportion instead of squeezing one half. */
let split=SPLIT_DEFAULT;
function setSplit(f, persist){
  split = Math.min(SPLIT_MAX, Math.max(SPLIT_MIN, f));
  document.getElementById('dock-bottom').style.flexBasis = (split*100).toFixed(1) + '%';
  if(persist) savePrefs({split});
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

(function splitResizing(){
  const grip=document.getElementById('grip');
  const view=document.getElementById('view-data');
  let active=false;
  grip.addEventListener('pointerdown', e=>{
    active=true;
    grip.setPointerCapture?.(e.pointerId);
    document.body.classList.add('resizing');
    e.preventDefault();
  });
  grip.addEventListener('pointermove', e=>{
    if(!active) return;
    const box=view.getBoundingClientRect();
    if(box.height) setSplit((box.bottom - e.clientY) / box.height, false);   // drag up = taller
  });
  const stop = e => {
    if(!active) return;
    active=false;
    document.body.classList.remove('resizing');
    try{ grip.releasePointerCapture?.(e.pointerId); }catch(_){}
    savePrefs({split});
  };
  grip.addEventListener('pointerup', stop);
  grip.addEventListener('pointercancel', stop);
  grip.addEventListener('dblclick', ()=> setSplit(SPLIT_DEFAULT, true));
  grip.addEventListener('keydown', e=>{
    const step = e.shiftKey ? 0.1 : 0.03;
    if(e.key==='ArrowUp') setSplit(split+step, true);
    else if(e.key==='ArrowDown') setSplit(split-step, true);
    else return;
    e.preventDefault();
  });
})();

(function applySavedLayout(){
  const p=loadPrefs();
  applyTheme(currentTheme());
  setSplit(typeof p.split === 'number' ? p.split : SPLIT_DEFAULT, false);
  setView(p.view === 'data' ? 'data' : 'diagram', false);
  setRailHidden(!!p.railHidden, false);
})();

/* ---------- zooming the diagram ----------
   Done on the viewBox, so the drawing stays vector-sharp at any size: the live viewBox is a
   window onto the fitted one (svg.dataset.base, set by drawGraph), ZOOM_MAX times smaller at
   most. A new diagram resets it, because drawGraph rewrites both. Running does not redraw, so a
   zoom set up before Run survives the run — zoom into the part of the topology you are about to
   talk about, then press Run. */
const ZOOM_MAX = 6, ZOOM_STEP = 1.25;
const graphSvg = document.getElementById('graph');
function baseBox(){
  const b = (graphSvg.dataset.base || '').split(' ').map(Number);
  return b.length === 4 && b[2] > 0 ? {x:b[0], y:b[1], w:b[2], h:b[3]} : null;
}
function viewBox(){ const v = graphSvg.viewBox.baseVal; return {x:v.x, y:v.y, w:v.width, h:v.height}; }
function zoomLevel(){ const b = baseBox(); return b ? b.w / viewBox().w : 1; }
/* Keep at least half the drawing on the canvas, so a pan can never lose it entirely. */
function clampView(v, b){
  const x = Math.min(b.x + b.w - v.w/2, Math.max(b.x - v.w/2, v.x));
  const y = Math.min(b.y + b.h - v.h/2, Math.max(b.y - v.h/2, v.y));
  return {x, y, w:v.w, h:v.h};
}
function setViewBox(v){
  const b = baseBox(); if(!b) return;
  const z = b.w / v.w;
  if(z <= 1.0001) v = b; else v = clampView(v, b);
  graphSvg.setAttribute('viewBox', `${v.x} ${v.y} ${v.w} ${v.h}`);
  const zoomed = b.w / v.w > 1.0001;
  document.getElementById('view-diagram').classList.toggle('zoomed', zoomed);
  document.getElementById('zoom-reset').textContent = Math.round(100 * b.w / v.w) + '%';
}
/* The point under the pointer, in drawing units; the view centre when there is no pointer. */
function svgPoint(clientX, clientY){
  const v = viewBox();
  const ctm = graphSvg.getScreenCTM();
  if(clientX == null || !ctm) return {x: v.x + v.w/2, y: v.y + v.h/2};
  const p = new DOMPoint(clientX, clientY).matrixTransform(ctm.inverse());
  return {x:p.x, y:p.y};
}
/* Zoom by a factor, keeping the point under the pointer exactly where it is on screen. */
function zoomBy(factor, clientX, clientY){
  const b = baseBox(); if(!b) return;
  const v = viewBox();
  const z = Math.min(ZOOM_MAX, Math.max(1, (b.w / v.w) * factor));
  const w = b.w / z, h = b.h / z, r = w / v.w;
  const p = svgPoint(clientX, clientY);
  setViewBox({x: p.x - (p.x - v.x) * r, y: p.y - (p.y - v.y) * r, w, h});
}
function zoomReset(){ const b = baseBox(); if(b) setViewBox(b); }

graphSvg.addEventListener('wheel', e=>{
  e.preventDefault();
  /* A trackpad pinch arrives as a wheel event with ctrlKey and small deltas; a mouse wheel as
     ±100-ish. Scaling by the delta makes both feel proportionate. */
  const delta = Math.max(-100, Math.min(100, e.deltaY * (e.deltaMode === 1 ? 33 : 1)));
  zoomBy(Math.exp(-delta * (e.ctrlKey ? 0.01 : 0.0025)), e.clientX, e.clientY);
}, {passive:false});
(function panning(){
  let from = null;
  graphSvg.addEventListener('pointerdown', e=>{
    if(e.button !== 0 || zoomLevel() <= 1.0001) return;
    from = {x:e.clientX, y:e.clientY, v:viewBox()};
    graphSvg.setPointerCapture?.(e.pointerId);
    document.getElementById('view-diagram').classList.add('panning');
  });
  graphSvg.addEventListener('pointermove', e=>{
    if(!from) return;
    const ctm = graphSvg.getScreenCTM(); if(!ctm) return;
    setViewBox({x: from.v.x - (e.clientX - from.x) / ctm.a,
                y: from.v.y - (e.clientY - from.y) / ctm.d, w: from.v.w, h: from.v.h});
  });
  const stop = e=>{
    if(!from) return;
    from = null;
    try{ graphSvg.releasePointerCapture?.(e.pointerId); }catch(_){}
    document.getElementById('view-diagram').classList.remove('panning');
  };
  graphSvg.addEventListener('pointerup', stop);
  graphSvg.addEventListener('pointercancel', stop);
})();
graphSvg.addEventListener('dblclick', zoomReset);
document.getElementById('zoom-in').onclick = ()=>zoomBy(ZOOM_STEP);
document.getElementById('zoom-out').onclick = ()=>zoomBy(1/ZOOM_STEP);
document.getElementById('zoom-reset').onclick = zoomReset;
document.addEventListener('keydown', e=>{
  if(e.ctrlKey || e.metaKey || e.altKey) return;
  if(e.target.closest && e.target.closest('input,textarea,select,[contenteditable]')) return;
  if(document.body.classList.contains('on-grid') || currentView() !== 'diagram') return;
  if(e.key === '+' || e.key === '=') zoomBy(ZOOM_STEP);
  else if(e.key === '-' || e.key === '_') zoomBy(1/ZOOM_STEP);
  else if(e.key === '0') zoomReset();
  else return;
  e.preventDefault();
});

/* The diagram is a viewBox scaled to fit its pane, so switching views, collapsing the rail or
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
