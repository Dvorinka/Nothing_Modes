// Nothing Modes — landing interactivity. Vanilla, no deps.

(function () {
  "use strict";

  const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // ---- Glyph Matrix: 25x25 dot grid spelling "NM" pattern + hover + breathe ----
  const matrix = document.getElementById("matrix");
  if (matrix) {
    const N = 25;
    // 25x25 bitmap for "NM" — block letters, 7 cols each, separated.
    // Build via simple per-pixel rule: draw N and M side by side.
    const grid = [];
    for (let y = 0; y < N; y++) grid.push(new Array(N).fill(0));

    // Letter cell bounds
    const set = (x0, y0, w, h, val) => {
      for (let y = 0; y < h; y++) {
        for (let x = 0; x < w; x++) {
          const px = x0 + x, py = y0 + y;
          if (px >= 0 && px < N && py >= 0 && py < N) grid[py][px] = val;
        }
      }
    };

    // "N" — left col, right col, diagonal
    const nX = 4, nY = 5, nH = 15, nW = 7;
    set(nX, nY, 1, nH, 1);            // left
    set(nX + nW - 1, nY, 1, nH, 1);   // right
    for (let i = 0; i < nH; i++) {
      const t = i / (nH - 1);
      const cx = Math.round(nX + t * (nW - 1));
      set(cx, nY + i, 1, 1, 1);
    }

    // "M" — two verticals + middle V
    const mX = 14, mY = 5, mH = 15, mW = 7;
    set(mX, mY, 1, mH, 1);
    set(mX + mW - 1, mY, 1, mH, 1);
    for (let i = 0; i < Math.floor(mH / 2); i++) {
      set(mX + 1 + i, mY + i, 1, 1, 1);
      set(mX + mW - 2 - i, mY + i, 1, 1, 1);
    }

    const frag = document.createDocumentFragment();
    const cells = [];
    for (let y = 0; y < N; y++) {
      for (let x = 0; x < N; x++) {
        const c = document.createElement("div");
        c.className = "cell";
        if (grid[y][x]) c.classList.add("on");
        frag.appendChild(c);
        cells.push(c);
      }
    }
    matrix.appendChild(frag);

    // Hover lights up neighbouring dots mechanically
    if (!prefersReduced) {
      matrix.addEventListener("mousemove", (e) => {
        const rect = matrix.getBoundingClientRect();
        const cellW = rect.width / N;
        const cx = Math.floor((e.clientX - rect.left) / cellW);
        const cy = Math.floor((e.clientY - rect.top) / cellW);
        for (let y = 0; y < N; y++) {
          for (let x = 0; x < N; x++) {
            const c = cells[y * N + x];
            if (!c.classList.contains("on") && !c.classList.contains("red")) {
              const dx = x - cx, dy = y - cy;
              const d = Math.sqrt(dx * dx + dy * dy);
              if (d < 2.2) c.classList.add("hover");
              else c.classList.remove("hover");
            }
          }
        }
      });
      matrix.addEventListener("mouseleave", () => {
        cells.forEach((c) => c.classList.remove("hover"));
      });

      // Stepped mechanical "breathe": a few cells pulse red on a hard timer
      let phase = 0;
      const redCells = [];
      // pick 6 cells from the N stroke to pulse
      [3, 7, 11, 13, 17, 21].forEach((i) => {
        const c = cells[nY * N + nX + i * N];
        if (c) redCells.push(c);
      });
      const tick = () => {
        redCells.forEach((c, i) => {
          const on = ((phase + i) % 4) < 2;
          c.classList.toggle("red", on);
          c.classList.toggle("on", !on && !c.classList.contains("red"));
        });
        phase++;
      };
      tick();
      setInterval(tick, 600);
    }
  }
})();
