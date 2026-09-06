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

    // Outer "C" arc — a two-cell-thick ring (band 8.0..10.4) with a gap
    // carved in the top-right (angles 285deg..345deg; 0deg = +x, y down).
    // Inner ring around the centre evokes the camera-module glyph, and a
    // detached dash plus a short straight segment complete the layout.
    for (let y = 0; y < N; y++) {
      for (let x = 0; x < N; x++) {
        const dx = x - cx, dy = y - cy;
        const d = Math.sqrt(dx * dx + dy * dy);
        if (d >= 8.0 && d <= 10.4) {
          const a = (Math.atan2(dy, dx) * 180 / Math.PI + 360) % 360;
          if (a > 285 && a < 345) continue; // top-right gap
          grid[y][x] = 1;
        }
        if (d >= 2.4 && d <= 3.8) grid[y][x] = 1; // inner camera ring
      }
    }

    // Short straight segment on the right, inside the arc.
    for (let y = 10; y <= 14; y++) grid[y][19] = 1;

    // Star signature inside the top-right gap — vertical bar, wider
    // horizontal bar, and the four inner diagonal cells filled (value 3).
    const starCells = [];
    const star = (x, y) => {
      grid[y][x] = 3;
      starCells.push([x, y]);
    };
    for (let y = 2; y <= 6; y++) star(19, y);        // vertical
    for (let x = 16; x <= 22; x++) star(x, 4);       // horizontal
    [[18, 3], [20, 3], [18, 5], [20, 5]].forEach(([x, y]) => star(x, y));

    const frag = document.createDocumentFragment();
    const cells = [];
    for (let y = 0; y < N; y++) {
      for (let x = 0; x < N; x++) {
        const c = document.createElement("div");
        c.className = "cell";
        if (grid[y][x] === 1) c.classList.add("on");
        if (grid[y][x] === 3) c.classList.add("sig");
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
            if (!c.classList.contains("on") && !c.classList.contains("sig")) {
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

      // Star blinks red <-> white on a hard timer.
      const sigEls = starCells.map(([x, y]) => cells[y * N + x]);
      let starRed = true;
      setInterval(() => {
        starRed = !starRed;
        sigEls.forEach((c) => {
          c.classList.toggle("sig-red", starRed);
          c.classList.toggle("sig-white", !starRed);
        });
      }, 650);
    }
  }
})();
