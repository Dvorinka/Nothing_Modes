// Nothing Modes — landing interactivity. Vanilla, no deps.

(function () {
  "use strict";

  const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // ---- Glyph Matrix: 25x25 dot grid rendering a stylised glyph ring ----
  // Evokes the Nothing Phone (2) glyph: a large "C" arc with a top-right gap,
  // a detached diagonal dash, and a central "!" exclamation motif.
  const matrix = document.getElementById("matrix");
  if (matrix) {
    const N = 25;
    const grid = [];
    for (let y = 0; y < N; y++) grid.push(new Array(N).fill(0));

    const cx = 12, cy = 12;

    // Main ring: cells whose distance from centre sits in the band [8.2, 9.8],
    // with a gap carved in the top-right arc (angles 285deg..345deg, where
    // 0deg = +x axis and y points down).
    const ringCells = [];
    for (let y = 0; y < N; y++) {
      for (let x = 0; x < N; x++) {
        const dx = x - cx, dy = y - cy;
        const d = Math.sqrt(dx * dx + dy * dy);
        if (d >= 8.2 && d <= 9.8) {
          const a = (Math.atan2(dy, dx) * 180 / Math.PI + 360) % 360;
          if (a > 285 && a < 345) continue; // top-right gap
          grid[y][x] = 1;
          if (a >= 150 && a <= 250) ringCells.push([x, y]); // lower-left arc
        }
      }
    }

    // Detached diagonal dash of 3 dots near the top-right gap.
    grid[4][17] = 1;
    grid[3][18] = 1;
    grid[2][19] = 1;

    // Centre "!" motif: a single lit dot with a 3-dot vertical bar below it.
    grid[12][12] = 1;
    grid[14][12] = 1;
    grid[15][12] = 1;
    grid[16][12] = 1;

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
        const cxp = Math.floor((e.clientX - rect.left) / cellW);
        const cyp = Math.floor((e.clientY - rect.top) / cellW);
        for (let y = 0; y < N; y++) {
          for (let x = 0; x < N; x++) {
            const c = cells[y * N + x];
            if (!c.classList.contains("on") && !c.classList.contains("red")) {
              const dx = x - cxp, dy = y - cyp;
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

      // Stepped mechanical "breathe": six dots on the ring's lower-left arc
      // pulse red on a hard timer.
      const redCells = [];
      if (ringCells.length) {
        const step = Math.max(1, Math.floor(ringCells.length / 6));
        for (let i = 0; i < 6 && i * step < ringCells.length; i++) {
          const [rx, ry] = ringCells[i * step];
          redCells.push(cells[ry * N + rx]);
        }
      }
      let phase = 0;
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
