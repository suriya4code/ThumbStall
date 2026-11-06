/* ThumbStall site: a doomscrolling feed column that stalls around a 3D film reel,
   plus a working pause-screen simulator. No dependencies. */
(() => {
  "use strict";
  const reduceMotion = matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = matchMedia("(pointer: fine)").matches;
  const params = new URLSearchParams(location.search);
  const LOOP = params.has("loop"); // ?loop cycles scroll → stall, used to record the README banner

  /* ═══════════════ HERO: feed column + reel ═══════════════ */
  const canvas = document.getElementById("feed");
  const ctx = canvas.getContext("2d");
  const toggle = document.getElementById("stallToggle");
  const hud = document.getElementById("breathHud");
  const hudCount = document.getElementById("hudCount");
  const hudWord = document.getElementById("hudWord");

  const COLS = 11, ROWS = 10;
  const CARD_W = 0.47, CARD_H = 0.78, ROW_GAP = 0.9;
  const SPAN = ROWS * ROW_GAP;
  const SPR_W = 180, SPR_H = Math.round((SPR_W * CARD_H) / CARD_W);

  /* Palette pairs for card media */
  const MEDIA = [
    ["#5a6b2e", "#c5cc9e"], ["#c2622d", "#e9b27f"], ["#3f4b1f", "#74883c"], ["#2b3b45", "#7aa0a8"],
    ["#6b4a2e", "#d8b484"], ["#4b2e3f", "#b07a96"], ["#2e4b3b", "#8fc2a2"], ["#45402b", "#d9cf94"],
  ];

  function roundRect(c, x, y, w, h, r) {
    c.beginPath();
    c.moveTo(x + r, y); c.arcTo(x + w, y, x + w, y + h, r); c.arcTo(x + w, y + h, x, y + h, r);
    c.arcTo(x, y + h, x, y, r); c.arcTo(x, y, x + w, y, r); c.closePath();
  }

  function makeSprite(i) {
    const s = document.createElement("canvas");
    s.width = SPR_W; s.height = SPR_H;
    const c = s.getContext("2d");
    const [a, b] = MEDIA[i % MEDIA.length];
    roundRect(c, 0, 0, SPR_W, SPR_H, 16);
    c.fillStyle = "#1d1f17"; c.fill();
    c.strokeStyle = "rgba(197,204,158,.22)"; c.lineWidth = 2; c.stroke();
    // header
    c.fillStyle = b; c.beginPath(); c.arc(22, 22, 10, 0, Math.PI * 2); c.fill();
    c.fillStyle = "rgba(241,242,232,.55)"; roundRect(c, 40, 15, 70 + (i * 13) % 40, 7, 3.5); c.fill();
    c.fillStyle = "rgba(241,242,232,.22)"; roundRect(c, 40, 26, 46, 6, 3); c.fill();
    // media
    const mx = 10, my = 42, mw = SPR_W - 20, mh = SPR_H - 96;
    const g = c.createLinearGradient(mx, my, mx + mw, my + mh);
    g.addColorStop(0, a); g.addColorStop(1, b);
    roundRect(c, mx, my, mw, mh, 10); c.fillStyle = g; c.fill();
    c.save(); c.clip();
    // abstract shapes inside media
    c.globalAlpha = 0.35; c.fillStyle = "#f1f2e8";
    if (i % 3 === 0) { c.beginPath(); c.arc(mx + mw * 0.7, my + mh * 0.3, mw * 0.18, 0, 7); c.fill(); }
    c.beginPath(); c.moveTo(mx, my + mh); c.lineTo(mx + mw * 0.35, my + mh * 0.55); c.lineTo(mx + mw * 0.6, my + mh * 0.8);
    c.lineTo(mx + mw * 0.8, my + mh * 0.6); c.lineTo(mx + mw, my + mh); c.fill();
    c.restore(); c.globalAlpha = 1;
    if (i % 2 === 0) { // reel play triangle
      c.fillStyle = "rgba(19,20,15,.55)"; c.beginPath(); c.arc(mx + mw / 2, my + mh / 2, 18, 0, 7); c.fill();
      c.fillStyle = "#f1f2e8"; c.beginPath(); c.moveTo(mx + mw / 2 - 5, my + mh / 2 - 8); c.lineTo(mx + mw / 2 + 9, my + mh / 2); c.lineTo(mx + mw / 2 - 5, my + mh / 2 + 8); c.fill();
    }
    // footer: heart + lines
    const fy = SPR_H - 40;
    c.fillStyle = i % 4 === 1 ? "#c2622d" : "rgba(241,242,232,.6)";
    c.beginPath(); c.moveTo(20, fy + 13); c.bezierCurveTo(8, fy + 4, 13, fy - 4, 20, fy + 2); c.bezierCurveTo(27, fy - 4, 32, fy + 4, 20, fy + 13); c.fill();
    c.fillStyle = "rgba(241,242,232,.28)"; roundRect(c, 42, fy, 90, 6, 3); c.fill(); roundRect(c, 42, fy + 12, 60, 6, 3); c.fill();
    return s;
  }
  const sprites = Array.from({ length: 8 }, (_, i) => makeSprite(i));
  const backSprite = (() => {
    const s = document.createElement("canvas"); s.width = SPR_W; s.height = SPR_H;
    const c = s.getContext("2d"); roundRect(c, 0, 0, SPR_W, SPR_H, 16);
    c.fillStyle = "#1a1c15"; c.fill(); c.strokeStyle = "rgba(197,204,158,.12)"; c.lineWidth = 2; c.stroke();
    return s;
  })();

  const cards = [];
  for (let col = 0; col < COLS; col++) {
    for (let row = 0; row < ROWS; row++) {
      cards.push({ col, row, sprite: sprites[(col * 3 + row * 5) % sprites.length], phase: (col * 0.37) % 1 });
    }
  }

  /* Logo geometry (108 grid, centred on 54,54) */
  const REEL = new Path2D("M32,54 A22,22 0 1,0 76,54 A22,22 0 1,0 32,54 Z M63.1,54 A3.9,3.9 0 1,0 70.9,54 A3.9,3.9 0 1,0 63.1,54 Z M56.6,65.26 A3.9,3.9 0 1,0 64.4,65.26 A3.9,3.9 0 1,0 56.6,65.26 Z M43.6,65.26 A3.9,3.9 0 1,0 51.4,65.26 A3.9,3.9 0 1,0 43.6,65.26 Z M37.1,54 A3.9,3.9 0 1,0 44.9,54 A3.9,3.9 0 1,0 37.1,54 Z M43.6,42.74 A3.9,3.9 0 1,0 51.4,42.74 A3.9,3.9 0 1,0 43.6,42.74 Z M56.6,42.74 A3.9,3.9 0 1,0 64.4,42.74 A3.9,3.9 0 1,0 56.6,42.74 Z");
  const BARS = new Path2D("M49.4,48.5 h3.4 v11 h-3.4 Z M55.2,48.5 h3.4 v11 h-3.4 Z");
  const DISC = new Path2D("M32,54 A22,22 0 1,0 76,54 A22,22 0 1,0 32,54 Z");

  let W = 0, H = 0, dpr = 1, cx = 0, cy = 0, S = 1, mobile = false;
  let yaw = 0, scroll = 0, speed = 1, spin = 0;
  let stalled = false, stallAmt = 0, stallStart = 0, breath = 0;
  let mx = 0, my = 0, tmx = 0, tmy = 0, pageSlow = 0;
  let running = true, last = performance.now(), t = 0;
  const PAUSE_SECS = 8, D = 3.4;

  const motes = Array.from({ length: 70 }, () => ({ x: Math.random(), y: Math.random(), z: Math.random(), s: Math.random() }));

  function resize() {
    dpr = Math.min(window.devicePixelRatio || 1, 2);
    W = canvas.clientWidth; H = canvas.clientHeight;
    canvas.width = Math.round(W * dpr); canvas.height = Math.round(H * dpr);
    mobile = W < 720;
    cx = mobile ? W * 0.5 : W * 0.72;
    cy = mobile ? 200 : H * 0.5;
    S = mobile ? Math.min(W * 0.36, 150) : Math.min(W * 0.17, H * 0.3);
  }

  function setStall(on) {
    stalled = on;
    toggle.setAttribute("aria-pressed", String(on));
    toggle.querySelector("span").textContent = on ? "Resume the scroll" : "Stall the feed";
    hud.classList.toggle("on", on);
    if (on) stallStart = t;
  }
  toggle.addEventListener("click", () => { userTouched = true; setStall(!stalled); });
  let userTouched = false;

  function frame(now) {
    const dt = Math.min(now - last, 50) / 1000;
    last = now;
    if (!running) { requestAnimationFrame(frame); return; }
    t += dt;

    if (LOOP) { const c = t % 10; if (c < 3.6 && stalled) setStall(false); else if (c >= 3.6 && !stalled) setStall(true); }

    mx += (tmx - mx) * Math.min(1, dt * 3); my += (tmy - my) * Math.min(1, dt * 3);
    stallAmt += ((stalled ? 1 : 0) - stallAmt) * Math.min(1, dt * (stalled ? 1.6 : 3));
    const target = Math.max(0, 1 - Math.max(stallAmt, pageSlow));
    speed += (target - speed) * Math.min(1, dt * 2.2);
    scroll += speed * dt * 2.6;
    yaw += dt * (0.05 + speed * 0.18);
    spin += dt * speed * 7;
    if (stallAmt > 0.6) { const snap = Math.round(spin / (Math.PI / 3)) * (Math.PI / 3); spin += (snap - spin) * Math.min(1, dt * 3); }

    // breathing: 4s in, 4s out once stalled
    const st = stalled ? t - stallStart : 0;
    breath = stalled ? (1 - Math.cos((st / 8) * Math.PI * 2)) / 2 : breath * 0.95;
    if (stalled) {
      const left = Math.max(0, PAUSE_SECS - Math.floor(st));
      hudCount.textContent = String(left);
      hudWord.textContent = left === 0 ? "your call" : (st % 8) < 4 ? "breathe in" : "breathe out";
    }

    draw(st);
    requestAnimationFrame(frame);
  }

  function draw(st) {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    const ry = yaw + mx * 0.35, rx = -0.2 + my * 0.12;
    const cyw = Math.cos(ry), syw = Math.sin(ry), cp = Math.cos(rx), sp = Math.sin(rx);
    const radius = 1 + stallAmt * 0.12;
    const rot = (x, y, z) => {
      const x1 = x * cyw + z * syw, z1 = -x * syw + z * cyw;
      return [x1, y * cp - z1 * sp, y * sp + z1 * cp];
    };
    const proj = (p) => { const k = D / (p[2] + D); return [cx + p[0] * S * k, cy + p[1] * S * k, k]; };

    // motes
    for (const m of motes) {
      const y = ((m.y - scroll * 0.03 * (0.5 + m.z)) % 1 + 1) % 1;
      ctx.globalAlpha = 0.15 + m.z * 0.35;
      ctx.fillStyle = m.s > 0.85 ? "#c2622d" : "#c5cc9e";
      ctx.beginPath(); ctx.arc(m.x * W, y * H, 0.6 + m.z * 1.6, 0, 7); ctx.fill();
    }
    ctx.globalAlpha = 1;

    // project cards
    const list = [];
    for (const c of cards) {
      const th = (c.col / COLS) * Math.PI * 2;
      let y = ((c.row * ROW_GAP + c.phase * ROW_GAP - scroll) % SPAN + SPAN) % SPAN - SPAN / 2;
      const s = Math.sin(th), co = Math.cos(th);
      const ctr = [s * radius, y, -co * radius];
      const tx = co, tz = s; // tangent (maps to +x at the front)
      const hw = CARD_W / 2, hh = CARD_H / 2;
      const TL = rot(ctr[0] - tx * hw, y - hh, ctr[2] - tz * hw);
      const TR = rot(ctr[0] + tx * hw, y - hh, ctr[2] + tz * hw);
      const BL = rot(ctr[0] - tx * hw, y + hh, ctr[2] - tz * hw);
      const n = rot(s, 0, -co);
      const zc = (TL[2] + TR[2] + BL[2]) / 3;
      list.push({ c, TL, TR, BL, z: zc, front: n[2] < 0, y });
    }
    list.sort((a, b) => b.z - a.z);

    const blur = Math.min(1, speed * 1.2);
    for (const it of list) {
      const a = proj(it.TL), b = proj(it.TR), l = proj(it.BL);
      const fog = Math.max(0, Math.min(1, (it.z + 1.4) / 2.6));
      const edge = Math.max(0, 1 - Math.abs(it.y) / (SPAN / 2)); // fade at column ends
      let alpha = (it.front ? 1 - fog * 0.75 : 0.28 - fog * 0.18) * Math.min(1, edge * 2.2);
      alpha *= 1 - stallAmt * 0.45;
      if (alpha <= 0.01) continue;
      const spr = it.front ? it.c.sprite : backSprite;
      const ax = (b[0] - a[0]) / SPR_W, ay = (b[1] - a[1]) / SPR_W;
      const bx = (l[0] - a[0]) / SPR_H, by = (l[1] - a[1]) / SPR_H;
      if (it.front && blur > 0.15) { // motion streaks
        for (let g = 2; g >= 1; g--) {
          const off = g * blur * 16;
          ctx.globalAlpha = alpha * 0.16;
          ctx.setTransform(dpr * ax, dpr * ay, dpr * bx, dpr * by, dpr * a[0], dpr * (a[1] + off));
          ctx.drawImage(spr, 0, 0);
        }
      }
      ctx.globalAlpha = alpha;
      ctx.setTransform(dpr * ax, dpr * ay, dpr * bx, dpr * by, dpr * a[0], dpr * a[1]);
      ctx.drawImage(spr, 0, 0);
    }
    ctx.globalAlpha = 1;

    drawReel(st, proj, rot);
  }

  function drawReel(st, proj, rot) {
    const zR = -radius() - 0.25;
    const c = proj([mx * 0.12, -my * 0.06, zR]);
    const R = (mobile ? 0.32 : 0.3) * (1 + breath * 0.1);
    const u = (R * S * c[2]) / 22; // px per logo unit
    const phi = (1 - stallAmt) * (0.55 * Math.sin(t * 0.9)) + mx * 0.25;
    const fx = Math.cos(phi);
    const psi = spin;

    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    // halo
    const glowR = 31 * u * (1.9 + breath * 0.5);
    const g = ctx.createRadialGradient(c[0], c[1], 0, c[0], c[1], glowR);
    g.addColorStop(0, `rgba(197,204,158,${0.18 + stallAmt * 0.22})`);
    g.addColorStop(0.45, `rgba(90,107,46,${0.16 + stallAmt * 0.14})`);
    g.addColorStop(1, "rgba(90,107,46,0)");
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(c[0], c[1], glowR, 0, 7); ctx.fill();

    const M = (ox) => ctx.setTransform(dpr * u * fx, 0, 0, dpr * u, dpr * (c[0] + ox), dpr * c[1]);

    // timer ring (tilts with the reel)
    M(0);
    ctx.lineCap = "round";
    ctx.lineWidth = 2.2;
    ctx.strokeStyle = "rgba(241,242,232,.25)";
    ctx.beginPath(); ctx.arc(0, 0, 31, 0, Math.PI * 2); ctx.stroke();
    let a0, a1;
    if (stallAmt < 0.5 || !stalled) { a0 = -Math.PI / 2 + t * 2.4 * speed + t * 0.3; a1 = a0 + Math.PI * 1.5; }
    else { const p = Math.min(1, st / PAUSE_SECS); a0 = -Math.PI / 2; a1 = a0 + Math.max(0.001, p) * Math.PI * 2; }
    ctx.strokeStyle = "#f1f2e8";
    ctx.shadowColor = "rgba(197,204,158,.9)"; ctx.shadowBlur = 12 * dpr;
    ctx.beginPath(); ctx.arc(0, 0, 31, a0, a1); ctx.stroke();
    ctx.shadowBlur = 0;
    ctx.fillStyle = "#c2622d";
    ctx.beginPath(); ctx.arc(Math.cos(a1) * 31, Math.sin(a1) * 31, 2.6, 0, 7); ctx.fill();

    // light seen through the cut-outs
    M(0);
    const lg = ctx.createRadialGradient(0, 0, 0, 0, 0, 22);
    lg.addColorStop(0, `rgba(241,242,232,${0.25 + stallAmt * 0.6})`);
    lg.addColorStop(1, `rgba(197,204,158,${0.05 + stallAmt * 0.35})`);
    ctx.fillStyle = lg; ctx.beginPath(); ctx.arc(0, 0, 21.5, 0, 7); ctx.fill();

    // extruded edge
    const depth = Math.sin(phi) * 7 * u;
    const L = 9;
    for (let i = L; i >= 1; i--) {
      M(-depth * (i / L));
      ctx.rotate(psi); ctx.translate(-54, -54);
      ctx.fillStyle = i === L ? "#2c3416" : `rgb(${60 + i * 3},${72 + i * 3},${32 + i * 2})`;
      ctx.fill(REEL, "evenodd");
    }
    // face
    M(0);
    ctx.rotate(psi); ctx.translate(-54, -54);
    const fg = ctx.createLinearGradient(32, 32, 76, 76);
    fg.addColorStop(0, "#ffffff"); fg.addColorStop(0.5, "#f1f2e8"); fg.addColorStop(1, "#c5cc9e");
    ctx.fillStyle = fg;
    ctx.fill(REEL, "evenodd");
    // pause hub: bars are cut out, so paint them as night with a glow when stalled
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    M(0); ctx.translate(-54, -54);
    ctx.fillStyle = stallAmt > 0.05 ? `rgba(90,107,46,${0.6 + stallAmt * 0.4})` : "#2c3416";
    ctx.fill(BARS);
    // rim highlight
    M(0); ctx.translate(-54, -54);
    ctx.strokeStyle = "rgba(255,255,255,.35)"; ctx.lineWidth = 0.6; ctx.stroke(DISC);
    ctx.setTransform(1, 0, 0, 1, 0, 0);
  }
  function radius() { return 1 + stallAmt * 0.12; }

  resize();
  addEventListener("resize", resize);
  if (finePointer) addEventListener("pointermove", (e) => { tmx = (e.clientX / innerWidth - 0.5) * 2; tmy = (e.clientY / innerHeight - 0.5) * 2; });
  addEventListener("scroll", () => { pageSlow = Math.min(1, scrollY / (innerHeight * 0.9)); }, { passive: true });
  new IntersectionObserver(([e]) => { running = e.isIntersecting; }).observe(canvas);

  if (reduceMotion) {
    stallAmt = 1; speed = 0; setStall(true);
    t = 0; draw(PAUSE_SECS); running = false;
  } else {
    requestAnimationFrame(frame);
    if (!LOOP) setTimeout(() => { if (!userTouched && !stalled) setStall(true); }, 3400);
  }

  /* ═══════════════ Reveal on scroll ═══════════════ */
  document.querySelectorAll(".feature-grid .card").forEach((el, i) => el.style.setProperty("--i", i % 3));
  const io = new IntersectionObserver((entries) => {
    for (const e of entries) if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); }
  }, { threshold: 0.15 });
  document.querySelectorAll(".reveal").forEach((el) => io.observe(el));

  /* ═══════════════ Tilt ═══════════════ */
  if (finePointer && !reduceMotion) {
    document.querySelectorAll("[data-tilt]").forEach((el) => {
      const max = el.classList.contains("phone") ? 10 : 7;
      el.addEventListener("pointermove", (e) => {
        const r = el.getBoundingClientRect();
        const px = (e.clientX - r.left) / r.width, py = (e.clientY - r.top) / r.height;
        el.style.transform = `rotateY(${(px - 0.5) * max}deg) rotateX(${(0.5 - py) * max}deg)`;
        el.style.setProperty("--mx", `${px * 100}%`); el.style.setProperty("--my", `${py * 100}%`);
      });
      el.addEventListener("pointerleave", () => { el.style.transform = ""; });
    });
  }

  /* ═══════════════ Pause simulator ═══════════════ */
  const $ = (id) => document.getElementById(id);
  const sim = { base: 8, app: "Instagram", repeats: 0, closed: 0, left: 0, timer: 0, breathTimer: 0, started: false };
  const STEP = 5, CAP = 30;
  const pauseFor = (r) => Math.min(sim.base + STEP * r, CAP);
  const ordinal = (n) => { const s = ["th", "st", "nd", "rd"], v = n % 100; return n + (s[(v - 20) % 10] || s[v] || s[0]); };
  const chips = [...document.querySelectorAll(".ps-chips button")];
  const breathEl = $("psBreath");

  function buildLadder() {
    const lad = $("ladder");
    lad.innerHTML = "";
    for (let i = 0; i < 6; i++) {
      const v = pauseFor(i);
      const r = document.createElement("div");
      r.className = "rung" + (v === CAP && sim.base + STEP * i >= CAP ? " cap" : "");
      r.innerHTML = `<b>${v}s</b><i style="height:${(v / CAP) * 100}%"></i><small>${i === 5 ? "6th+" : ordinal(i + 1)}</small>`;
      lad.appendChild(r);
    }
    paintLadder();
  }
  function paintLadder() {
    const idx = Math.min(sim.repeats, 5);
    [...$("ladder").children].forEach((r, i) => { r.classList.toggle("now", i === idx); r.classList.toggle("past", i < idx); });
    $("statClosed").textContent = sim.closed;
    $("statOpens").textContent = sim.repeats + 1;
    $("statWait").textContent = pauseFor(sim.repeats + 1) + "s";
  }

  function toast(html, ms) {
    const el = $("psToast");
    el.innerHTML = `<div>${html}</div>`; el.classList.add("on");
    clearTimeout(toast.t);
    if (ms) toast.t = setTimeout(() => el.classList.remove("on"), ms);
  }

  function startPause() {
    clearInterval(sim.timer); clearInterval(sim.breathTimer);
    $("psToast").classList.remove("on");
    sim.left = pauseFor(sim.repeats);
    $("psName").textContent = sim.app;
    $("psIco").textContent = sim.app[0];
    $("psNth").textContent = `${ordinal(sim.repeats + 1)} open this hour`;
    $("psClose").textContent = `Close ${sim.app}`;
    $("psFoot").textContent = `You closed it instead ${sim.closed} time${sim.closed === 1 ? "" : "s"} this week`;
    chips.forEach((c) => (c.disabled = true));
    breathEl.classList.remove("done");
    let inhale = true;
    breathEl.classList.add("in");
    $("psWord").textContent = "breathe in";
    const tick = () => {
      $("psCount").textContent = sim.left;
      $("psAvail").textContent = sim.left > 0 ? `Open for · available in ${sim.left}s` : "Open for · your call";
      if (sim.left <= 0) {
        clearInterval(sim.timer);
        chips.forEach((c) => (c.disabled = false));
        breathEl.classList.add("done");
      }
      sim.left--;
    };
    tick();
    sim.timer = setInterval(tick, 1000);
    sim.breathTimer = setInterval(() => {
      inhale = !inhale;
      breathEl.classList.toggle("in", inhale);
      $("psWord").textContent = inhale ? "breathe in" : "breathe out";
    }, 4000);
    paintLadder();
  }

  $("psClose").addEventListener("click", () => {
    clearInterval(sim.timer); clearInterval(sim.breathTimer);
    sim.closed++;
    paintLadder();
    $("psFoot").textContent = `You closed it instead ${sim.closed} time${sim.closed === 1 ? "" : "s"} this week`;
    toast(`Closed. That's time back.<small>Counted as time reclaimed on your Today screen. Tap “Open it again” to see the next pause.</small>`);
  });
  chips.forEach((c) => c.addEventListener("click", () => {
    clearInterval(sim.breathTimer);
    toast(`${sim.app} open for ${c.dataset.min} min.<small>It opens without a pause until this session budget runs out.</small>`);
  }));
  $("openAgain").addEventListener("click", () => { sim.repeats++; startPause(); });
  $("resetDemo").addEventListener("click", () => { sim.repeats = 0; startPause(); });

  function seg(id, onPick) {
    const g = $(id);
    g.addEventListener("click", (e) => {
      const b = e.target.closest("button"); if (!b) return;
      g.querySelectorAll("button").forEach((x) => { x.classList.toggle("on", x === b); x.setAttribute("aria-checked", String(x === b)); });
      onPick(b);
    });
  }
  seg("intensity", (b) => { sim.base = +b.dataset.base; buildLadder(); startPause(); });
  seg("appPick", (b) => { sim.app = b.dataset.app; sim.repeats = 0; startPause(); });

  buildLadder();
  $("psCount").textContent = pauseFor(0);
  new IntersectionObserver(([e], obs) => {
    if (e.isIntersecting && !sim.started) { sim.started = true; startPause(); obs.disconnect(); }
  }, { threshold: 0.35 }).observe($("phone"));

  /* ═══════════════ Flow progress ═══════════════ */
  const flow = $("flow");
  const steps = [...flow.children];
  function flowProgress() {
    const r = flow.getBoundingClientRect();
    const vertical = matchMedia("(max-width: 980px)").matches;
    let p = vertical ? (innerHeight * 0.65 - r.top) / r.height : (innerHeight * 0.8 - r.top) / (innerHeight * 0.5);
    p = Math.max(0, Math.min(1, p));
    flow.style.setProperty("--p", p);
    steps.forEach((s, i) => s.classList.toggle("lit", p >= i / (steps.length - 1) - 0.02 && p > 0));
  }
  addEventListener("scroll", flowProgress, { passive: true });
  flowProgress();

  /* ═══════════════ Counters ═══════════════ */
  const co = new IntersectionObserver((entries) => {
    for (const e of entries) {
      if (!e.isIntersecting) continue;
      co.unobserve(e.target);
      const el = e.target, end = +el.dataset.count, suf = el.dataset.suffix || "";
      if (reduceMotion || end === 0) { el.textContent = end.toLocaleString() + suf; continue; }
      const t0 = performance.now(), dur = 1400;
      const step = (now) => {
        const k = Math.min(1, (now - t0) / dur), v = Math.round(end * (1 - Math.pow(1 - k, 3)));
        el.textContent = v.toLocaleString() + suf;
        if (k < 1) requestAnimationFrame(step);
      };
      requestAnimationFrame(step);
    }
  }, { threshold: 0.6 });
  document.querySelectorAll("[data-count]").forEach((el) => co.observe(el));

  /* ═══════════════ 3D screens carousel ═══════════════ */
  const ring = $("ring"), carousel = $("carousel");
  const figs = [...ring.children];
  let angle = 0, vel = reduceMotion ? 0 : 0.12, drag = false, lastX = 0, ringR = 0;
  function layoutRing() {
    const w = ring.offsetWidth;
    ringR = Math.round((w / 2) / Math.tan(Math.PI / figs.length) * 1.12);
    figs.forEach((f, i) => { f.dataset.a = (360 / figs.length) * i; });
  }
  function spinRing() {
    if (!drag) { vel += ((reduceMotion ? 0 : 0.12) - vel) * 0.02; angle += vel; }
    ring.style.transform = `translateZ(${-ringR}px) rotateY(${angle}deg)`;
    figs.forEach((f) => {
      const a = (+f.dataset.a + angle) % 360;
      const rad = (a * Math.PI) / 180;
      f.style.transform = `rotateY(${f.dataset.a}deg) translateZ(${ringR}px)`;
      f.style.opacity = String(0.25 + 0.75 * ((Math.cos(rad) + 1) / 2));
    });
    requestAnimationFrame(spinRing);
  }
  carousel.addEventListener("pointerdown", (e) => { drag = true; lastX = e.clientX; carousel.setPointerCapture(e.pointerId); });
  carousel.addEventListener("pointermove", (e) => { if (!drag) return; const dx = e.clientX - lastX; lastX = e.clientX; vel = dx * 0.25; angle += vel; });
  const endDrag = () => { drag = false; };
  carousel.addEventListener("pointerup", endDrag); carousel.addEventListener("pointercancel", endDrag);
  layoutRing(); addEventListener("resize", layoutRing);
  requestAnimationFrame(spinRing);

  /* ═══════════════ Copy ═══════════════ */
  document.querySelectorAll(".copy").forEach((b) => b.addEventListener("click", async () => {
    try { await navigator.clipboard.writeText(b.dataset.copy); b.textContent = "Copied"; }
    catch { b.textContent = "Select & copy"; }
    setTimeout(() => (b.textContent = "Copy"), 1600);
  }));
})();
