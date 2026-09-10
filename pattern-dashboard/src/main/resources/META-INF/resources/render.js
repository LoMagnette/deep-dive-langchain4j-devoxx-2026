/* Pure rendering helpers: HTML escaping, the markdown subset, and the topology
   drawing. No application state and no network — everything here is a function of its
   arguments, which is what makes it safe to reuse for both the live diagram and the
   gallery thumbnails. Loaded before app.js. */

function escapeHtml(s){return String(s).replace(/[&<>]/g,m=>({'&':'&amp;','<':'&lt;','>':'&gt;'}[m]));}

/* ---------- tiny markdown renderer ----------
   Models answer in markdown — ### headings, **bold**, bullet lists, fenced code — and a wall of
   raw asterisks is unreadable from the back of a room. A library would have to come off a CDN,
   which is the one thing guaranteed to fail on conference wifi, so this covers the subset an LLM
   actually emits and nothing else.

   Order matters for safety: the text is HTML-escaped BEFORE any tag is introduced, so nothing the
   model returns can inject markup. Only the tags generated below ever reach the DOM. */
const MD_CODE = '\u0000CODE';

function renderMarkdown(src){
  const codeBlocks = [];
  // Fenced code comes out first, so the inline rules below can't rewrite its contents.
  let s = String(src ?? '').replace(/```[^\n]*\n?([\s\S]*?)```/g, (_, body) => {
    codeBlocks.push('<pre><code>' + escapeHtml(body.replace(/\n$/, '')) + '</code></pre>');
    return MD_CODE + (codeBlocks.length - 1) + '\u0000';
  });
  s = escapeHtml(s);

  const out = [];
  let para = [], quote = [], list = null;
  const flushPara = () => { if(para.length){ out.push('<p>' + para.join('<br>') + '</p>'); para = []; } };
  const flushQuote = () => { if(quote.length){ out.push('<blockquote>' + quote.join('<br>') + '</blockquote>'); quote = []; } };
  const flushList = () => { if(list){ out.push('</' + list + '>'); list = null; } };
  const flush = () => { flushPara(); flushQuote(); flushList(); };
  const openList = (kind) => { if(list !== kind){ flushList(); out.push('<' + kind + '>'); list = kind; } };

  for(const line of s.split('\n')){
    const t = line.trim();
    let m;
    if(!t){ flush(); continue; }
    if(t.includes(MD_CODE)){ flush(); out.push(t); continue; }
    if(m = t.match(/^(#{1,6})\s+(.*)$/)){
      flush(); out.push(`<h${m[1].length}>${mdInline(m[2])}</h${m[1].length}>`); continue;
    }
    if(/^([-*_])\1{2,}$/.test(t)){ flush(); out.push('<hr>'); continue; }
    // Escaping already ran, so a quote marker reaches this loop as &gt;, never as >.
    // Consecutive quoted lines accumulate into one blockquote instead of a stack of boxes.
    if(m = t.match(/^&gt;\s?(.*)$/)){ flushPara(); flushList(); quote.push(mdInline(m[1])); continue; }
    if(m = t.match(/^[-*+]\s+(.*)$/)){ flushPara(); flushQuote(); openList('ul'); out.push('<li>' + mdInline(m[1]) + '</li>'); continue; }
    if(m = t.match(/^\d+[.)]\s+(.*)$/)){ flushPara(); flushQuote(); openList('ol'); out.push('<li>' + mdInline(m[1]) + '</li>'); continue; }
    flushQuote(); flushList();
    para.push(mdInline(t));
  }
  flush();

  return out.join('').replace(/\u0000CODE(\d+)\u0000/g, (_, i) => codeBlocks[+i]);
}

function mdInline(t){
  return t
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/__([^_]+)__/g, '<strong>$1</strong>')
    .replace(/(^|[^*])\*([^*\n]+)\*/g, '$1<em>$2</em>')
    .replace(/\[([^\]]+)\]\(([^)\s]+)\)/g, (whole, label, href) => {
      /* Only schemes that can't execute, and quotes encoded so a URL can't escape the attribute
         (escapeHtml deliberately leaves quotes alone, which is fine in text but not in href). */
      if(!/^(https?:|mailto:)/i.test(href)) return whole;
      const safe = href.replace(/"/g, '%22').replace(/'/g, '%27');
      return `<a href="${safe}" target="_blank" rel="noopener noreferrer">${label}</a>`;
    });
}

/* ---------- graph layout & drawing ---------- */
const W=820,H=440,NW=150,NH=46;
function leadTok(s){ return (s||'').split(/[\s(]/)[0]; }

function layout(topo){
  const nodes = topo.nodes.map(n=>({...n}));
  const idx = {}; nodes.forEach((n,i)=>idx[n.id]=n);
  const lay = topo.layout;
  let cw=W, ch=H;                     // the canvas this graph needs, reported back to the caller
  const cx=W/2, cy=H/2;
  const agents = nodes.filter(n=>n.role!=='board'&&n.role!=='supervisor'&&n.role!=='input'&&n.role!=='router');

  if(lay==='chain'||lay==='dag'||lay==='loop'){
    const n=nodes.length, gap=W/(n+1);
    nodes.forEach((nd,i)=>{ nd.x=gap*(i+1); nd.y=cy; });
  } else if(lay==='branch'){
    /* Three columns, because a router that sits in the same column as its branches doesn't
       look like routing at all: input -> router -> one of N. */
    const src = nodes.find(n=>n.role==='input');
    const router = nodes.find(n=>n.role==='router');
    const branches = nodes.filter(n=>n!==src&&n!==router);
    if(src){ src.x=105; src.y=cy; }
    if(router){ router.x=cx-20; router.y=cy; }
    const step=H/(branches.length+1);
    branches.forEach((b,i)=>{ b.x=W-125; b.y=step*(i+1); });
  } else if(lay==='fanout'){
    /* Fan out on the left, join on the right. The join is what makes this a parallel workflow
       rather than three unrelated calls, so it anchors the layout when present. */
    const src = nodes.find(n=>n.role==='input')||nodes[0];
    const sink = nodes.find(n=>n.role==='join')||nodes.find(n=>n.role==='board');
    const mids = nodes.filter(n=>n!==src&&n!==sink);
    src.x = sink ? 110 : W*0.3; src.y = cy;
    if(sink){ sink.x=W-110; sink.y=cy; }
    const midX = sink ? cx : W*0.7;
    const step=H/(mids.length+1);
    mids.forEach((m,i)=>{ m.x=midX; m.y=step*(i+1); });
  } else if(lay==='star'){
    const center = nodes.find(n=>n.role==='supervisor')||nodes.find(n=>n.role==='board')||nodes[0];
    const others = nodes.filter(n=>n!==center);
    center.x=cx; center.y=cy;
    const R=150;
    others.forEach((o,i)=>{ const a=-Math.PI/2 + i*2*Math.PI/others.length; o.x=cx+R*Math.cos(a); o.y=cy+R*Math.sin(a); });
  } else if(lay==='stages'){
    /* Hand-placed columns. A composite has a real order of steps, and no automatic layout
       recovers it — the stage number on each node is the diagram's script. */
    const byStage = new Map();
    nodes.forEach(n => {
      const st = n.stage == null ? 0 : n.stage;
      if(!byStage.has(st)) byStage.set(st, []);
      byStage.get(st).push(n);
    });
    const stages = [...byStage.keys()].sort((a,b)=>a-b);
    cw = Math.max(W, stages.length * 225);   // room for the edge labels between columns
    const colGap = cw/(stages.length+1);
    stages.forEach((st,i)=>{
      const col = byStage.get(st);
      const rowGap = ch/(col.length+1);
      col.forEach((n,j)=>{ n.x = colGap*(i+1); n.y = rowGap*(j+1); });
    });
  } else { // mesh
    const R=150;
    nodes.forEach((nd,i)=>{ const a=-Math.PI/2 + i*2*Math.PI/nodes.length; nd.x=cx+R*Math.cos(a); nd.y=cy+R*Math.sin(a); });
  }
  return {nodes, idx, cw, ch};
}

function drawGraph(topo){
  const svg=document.getElementById('graph');
  svg.innerHTML='<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-end"><path d="M0 0L10 5L0 10z" fill="var(--node-line)"/></marker></defs>';
  const {nodes,idx,cw,ch}=layout(topo);
  svg.setAttribute('viewBox',`0 0 ${cw} ${ch}`);
  const order={}; topo.nodes.forEach((n,i)=>order[n.id]=i);
  /* A->B and B->A drawn as straight lines land exactly on top of each other, so a mutual
     relationship (debate rebuttals, supervisor invoke/result, blackboard read/write) rendered
     as one arrow. Bow each side of such a pair the opposite way. */
  /* Chain-like layouts already separate a mutual pair by arcing the return edge overhead
     (that is how the loop's exit condition reads), so bowing the forward edge too would just
     make a straight pipeline wobble. */
  const arcLayout = topo.layout==='chain'||topo.layout==='loop'||topo.layout==='dag';
  const pairs=new Set(topo.edges.map(e=>e.from+'->'+e.to));
  const isMutual = e => !arcLayout && pairs.has(e.to+'->'+e.from);

  topo.edges.forEach(e=>{
    const a=idx[e.from], b=idx[e.to]; if(!a||!b) return;
    const back = order[e.to] < order[e.from];
    const p=document.createElementNS('http://www.w3.org/2000/svg','path');
    let lx=(a.x+b.x)/2, ly=(a.y+b.y)/2 - 8;
    if(back && (topo.layout==='chain'||topo.layout==='loop'||topo.layout==='dag')){
      const arc=Math.min(a.y,b.y)-70;
      p.setAttribute('d',`M${a.x} ${a.y-NH/2} C ${a.x} ${arc}, ${b.x} ${arc}, ${b.x} ${b.y-NH/2}`);
      p.setAttribute('class','edge back');
      ly=(a.y+b.y)/2 - 60;
    } else if(isMutual(e)){
      // Perpendicular offset, signed consistently so the two halves bow apart, not together.
      const dx=b.x-a.x, dy=b.y-a.y, len=Math.hypot(dx,dy)||1;
      const dir=(e.from<e.to)?1:-1, bow=26*dir;
      const mx=(a.x+b.x)/2 - dy/len*bow, my=(a.y+b.y)/2 + dx/len*bow;
      p.setAttribute('d',`M${a.x} ${a.y} Q ${mx} ${my}, ${b.x} ${b.y}`);
      p.setAttribute('class','edge'+(back?' back':''));
      lx=(a.x+b.x)/2 - dy/len*bow*0.75; ly=(a.y+b.y)/2 + dx/len*bow*0.75;
    } else {
      p.setAttribute('d',`M${a.x} ${a.y} L ${b.x} ${b.y}`);
      p.setAttribute('class','edge');
    }
    svg.appendChild(p);
    if(e.label){
      const t=document.createElementNS('http://www.w3.org/2000/svg','text');
      t.setAttribute('x',lx); t.setAttribute('y',ly);
      t.setAttribute('class','edgelabel'); t.textContent=e.label; svg.appendChild(t);
    }
  });
  // nodes
  nodes.forEach(n=>{
    const g=document.createElementNS('http://www.w3.org/2000/svg','g');
    g.setAttribute('class','node '+(n.role==='board'||n.role==='join'?n.role:'')); g.dataset.id=n.id; g.dataset.tok=leadTok(n.label);
    const r=document.createElementNS('http://www.w3.org/2000/svg','rect');
    r.setAttribute('x',n.x-NW/2); r.setAttribute('y',n.y-NH/2); r.setAttribute('width',NW); r.setAttribute('height',NH); r.setAttribute('rx',14);
    g.appendChild(r);
    const t=document.createElementNS('http://www.w3.org/2000/svg','text');
    t.setAttribute('x',n.x); t.setAttribute('y',n.y);
    const lbl=n.label.length>20?n.label.slice(0,19)+'…':n.label;
    t.textContent=lbl; g.appendChild(t);
    svg.appendChild(g);
  });
}

function markNode(agent, cls){
  if(!agent) return;
  /* Fan-out agents report indexed names (RunInspector_0, CaseScout$1); the topology
     node is labelled with the bare type, so strip the index before matching. */
  const base=String(agent).replace(/[_$]\d+$/,'');
  document.querySelectorAll('.node').forEach(g=>{
    if(g.dataset.tok && (g.dataset.tok===agent || g.dataset.tok===base)){
      if(cls==='active'){ g.classList.add('active'); }
      else { g.classList.remove('active'); g.classList.add('done'); }
    }
  });
}

function drawThumb(svg, topo){
  const {nodes, idx, cw, ch} = layout(topo);
  svg.setAttribute('viewBox', `0 0 ${cw} ${ch}`);
  const parts = [];
  topo.edges.forEach(e => {
    const a = idx[e.from], b = idx[e.to];
    if(a && b) parts.push(`<path class="tedge" d="M${a.x} ${a.y} L ${b.x} ${b.y}"/>`);
  });
  nodes.forEach(n => parts.push(`<rect class="tnode ${n.role}" x="${n.x-NW/2}" y="${n.y-NH/2}"`
    + ` width="${NW}" height="${NH}" rx="16"/>`));
  svg.innerHTML = parts.join('');
}
