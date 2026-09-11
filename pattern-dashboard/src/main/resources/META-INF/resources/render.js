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
  svg.innerHTML='<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-end"><path d="M0 0L10 5L0 10z" fill="var(--edge-line)"/></marker></defs>';
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

  /* Labels are collected, not drawn, in this pass: they are placed once every node position is
     known and appended AFTER the nodes. See placeEdgeLabels. */
  const pending=[];

  topo.edges.forEach(e=>{
    const a=idx[e.from], b=idx[e.to]; if(!a||!b) return;
    const back = order[e.to] < order[e.from];
    const p=document.createElementNS('http://www.w3.org/2000/svg','path');
    /* at(t) walks the curve this edge is actually drawn as, so a label can slide ALONG its own
       arrow to find a clear spot instead of being pinned to the midpoint of a straight line it
       is not drawn on. */
    let at;
    if(back && (topo.layout==='chain'||topo.layout==='loop'||topo.layout==='dag')){
      const arc=Math.min(a.y,b.y)-70;
      const p0={x:a.x,y:a.y-NH/2}, c1={x:a.x,y:arc}, c2={x:b.x,y:arc}, p1={x:b.x,y:b.y-NH/2};
      p.setAttribute('d',`M${p0.x} ${p0.y} C ${c1.x} ${c1.y}, ${c2.x} ${c2.y}, ${p1.x} ${p1.y}`);
      p.setAttribute('class','edge back');
      at = t => { const u=1-t, k0=u*u*u, k1=3*u*u*t, k2=3*u*t*t, k3=t*t*t;
        return {x:k0*p0.x+k1*c1.x+k2*c2.x+k3*p1.x, y:k0*p0.y+k1*c1.y+k2*c2.y+k3*p1.y}; };
    } else if(isMutual(e)){
      // Perpendicular offset, signed consistently so the two halves bow apart, not together.
      const dx=b.x-a.x, dy=b.y-a.y, len=Math.hypot(dx,dy)||1;
      const dir=(e.from<e.to)?1:-1, bow=26*dir;
      const mx=(a.x+b.x)/2 - dy/len*bow, my=(a.y+b.y)/2 + dx/len*bow;
      p.setAttribute('d',`M${a.x} ${a.y} Q ${mx} ${my}, ${b.x} ${b.y}`);
      p.setAttribute('class','edge'+(back?' back':''));
      at = t => { const u=1-t;
        return {x:u*u*a.x+2*u*t*mx+t*t*b.x, y:u*u*a.y+2*u*t*my+t*t*b.y}; };
    } else {
      p.setAttribute('d',`M${a.x} ${a.y} L ${b.x} ${b.y}`);
      p.setAttribute('class','edge');
      at = t => ({x:a.x+(b.x-a.x)*t, y:a.y+(b.y-a.y)*t});
    }
    svg.appendChild(p);
    if(e.label) pending.push({text:e.label, at, a, b});
  });
  // nodes
  nodes.forEach(n=>{
    const g=document.createElementNS('http://www.w3.org/2000/svg','g');
    g.setAttribute('class','node '+(n.role==='board'||n.role==='join'||n.role==='human'?n.role:'')); g.dataset.id=n.id; g.dataset.tok=leadTok(n.label);
    const r=document.createElementNS('http://www.w3.org/2000/svg','rect');
    r.setAttribute('x',n.x-NW/2); r.setAttribute('y',n.y-NH/2); r.setAttribute('width',NW); r.setAttribute('height',NH); r.setAttribute('rx',14);
    g.appendChild(r);
    const t=document.createElementNS('http://www.w3.org/2000/svg','text');
    t.setAttribute('x',n.x); t.setAttribute('y',n.y);
    const lbl=n.label.length>20?n.label.slice(0,19)+'…':n.label;
    t.textContent=lbl; g.appendChild(t);
    svg.appendChild(g);
  });
  placeEdgeLabels(svg, pending, nodes);
  fitEdgeLabels(svg);
}

/* Edge labels are the most fragile thing in the diagram: they are small, they float free of any
   box, and they are the only thing that says WHAT travels along an arrow — "score < 0.8" is the
   loop's exit condition, and a diagram that loses it has lost the pattern. Three separate things
   were making them unreadable, and none of them was the font size:

   1. They were appended BEFORE the nodes, so any label whose anchor landed on a node box was
      painted over by it. Opaque node fills made this total, not partial — the commonest case,
      and it looked like the label was missing rather than covered.
   2. Their anchor was the midpoint between node CENTRES. In a fan-out, a star or a branch that
      point is regularly inside a third node, or on top of a sibling edge's label.
   3. They had no backing, so glyphs were crossed by their own arrow.

   (3) is fixed in CSS with a halo (paint-order). This function fixes (1) by being called after
   the nodes are appended, and (2) by sliding each label along its own curve — and perpendicular
   to it — until it sits clear of every node box and every label already placed. */
function placeEdgeLabels(svg, pending, nodes){
  const boxes = nodes.map(n=>({x1:n.x-NW/2-5, y1:n.y-NH/2-5, x2:n.x+NW/2+5, y2:n.y+NH/2+5}));
  const hits = (r,o) => r.x1 < o.x2 && r.x2 > o.x1 && r.y1 < o.y2 && r.y2 > o.y1;
  /* Tried nearest the middle of the arrow first, then further along it, then pushed further off
     to one side: a label that has to move should move as little as the picture allows.
     The offsets have to reach past a node box (NH/2 = 23) plus half a label, because in a chain
     the boxes are 150 wide and only ~15 apart — no multi-word label will EVER fit in that gap,
     so the honest place for it is above or below the row rather than spilling across two nodes.
     Negative first: above an arrow is where a reader looks for its label. */
  const TS = [0.5, 0.44, 0.56, 0.38, 0.62, 0.32, 0.68, 0.26, 0.74];
  const OFFS = [0, -16, 16, -27, 27, -36, 36, -45, 45, -56, 56];

  pending.forEach(p=>{
    /* No getBBox before the element is in the DOM, so estimate. Deliberately estimated at the
       LARGEST size a label can be drawn at (EDGE_FS_MAX, see fitEdgeLabels) rather than at 12px:
       the font grows when the diagram is scaled down, and space reserved for 12px text would be
       overrun by the same label at 18px. Reserving the worst case makes the placement valid at
       every pane size, and errs towards generous spacing, which is what legibility wants. */
    const w = p.text.length*(EDGE_FS_MAX*0.54) + 8, h = EDGE_FS_MAX*1.25;
    const dx=p.b.x-p.a.x, dy=p.b.y-p.a.y, len=Math.hypot(dx,dy)||1;
    const nx=-dy/len, ny=dx/len;              // unit normal to the chord
    let best=null, bestHits=Infinity;
    outer:
    for(const off of OFFS){
      for(const t of TS){
        const c=p.at(t);
        const x=c.x+nx*off, y=c.y+ny*off - (off===0?9:0);   // clear the line when centred
        const r={x1:x-w/2, y1:y-h/2, x2:x+w/2, y2:y+h/2};
        const n=boxes.reduce((k,o)=>k+(hits(r,o)?1:0),0);
        if(n===0){ best={x,y,r}; break outer; }
        // Nothing is clear yet — remember the least bad, so a cramped diagram degrades to one
        // slightly crowded label rather than to whatever the first guess happened to be.
        if(n<bestHits){ bestHits=n; best={x,y,r}; }
      }
    }
    /* Reserved with a margin, not flush: two labels that merely fail to overlap still read as
       one run of text. "needs been out" and "needs fed" landed 0.4px apart on the BDI diagram
       and the collision check was perfectly happy with it. */
    boxes.push({x1:best.r.x1-8, y1:best.r.y1-5, x2:best.r.x2+8, y2:best.r.y2+5});
    const t=document.createElementNS('http://www.w3.org/2000/svg','text');
    t.setAttribute('x',best.x); t.setAttribute('y',best.y);
    t.setAttribute('class','edgelabel'); t.textContent=p.text;
    svg.appendChild(t);
  });
}

/* The graph is a viewBox scaled to fit its pane, so a wide composite in a short pane is drawn at
   well under half size — and 12px labels stop being readable long before the diagram stops being
   useful. Hold them at a constant SIZE ON SCREEN instead by growing them in user units as the
   diagram shrinks. EDGE_FS_MAX is the ceiling, and it is not cosmetic: placeEdgeLabels reserves
   space at that size, so the labels it spaced apart stay spaced apart however the pane is
   dragged. Node labels are left to scale — they sit on an opaque plate that shrinks with them,
   and enlarging them would push their text out of its box. */
const EDGE_FS = 12, EDGE_FS_MAX = 18;
function fitEdgeLabels(svg){
  const box = svg.getBoundingClientRect();
  const vb = svg.viewBox.baseVal;
  if(!box.width || !vb || !vb.width) return;
  const scale = Math.min(box.width/vb.width, box.height/vb.height);
  if(!isFinite(scale) || scale <= 0) return;
  const px = Math.max(EDGE_FS/2, Math.min(EDGE_FS/scale, EDGE_FS_MAX));
  svg.style.setProperty('--edge-fs', px.toFixed(2)+'px');
}

function markNode(agent, cls){
  if(!agent) return;
  /* Fan-out agents report indexed names (FoodSafetyCheck_0, AngleScout$1); the topology
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
