// Nothing Modes — landing interactivity. Vanilla, no deps.

(function () {
  "use strict";

  const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

  // ---- Theme (dark/light) and style (nothing/normal) toggles ----
  const root = document.documentElement;
  const metaTheme = document.querySelector('meta[name="theme-color"]');

  const applyTheme = (t) => {
    root.setAttribute("data-theme", t);
    if (metaTheme) metaTheme.setAttribute("content", t === "light" ? "#F4F4F2" : "#000000");
    const btn = document.getElementById("theme-toggle");
    if (btn) btn.setAttribute("aria-pressed", String(t === "light"));
  };
  const applyStyle = (s) => {
    root.setAttribute("data-style", s);
    const btn = document.getElementById("style-toggle");
    if (btn) {
      btn.setAttribute("aria-pressed", String(s === "normal"));
      btn.textContent = s === "normal" ? "PLAIN" : "DOTS";
    }
    // Swap screenshot sources between Nothing and classic captures.
    document.querySelectorAll("img[data-normal-src]").forEach((img) => {
      const nothing = img.getAttribute("data-nothing-src") || img.src;
      if (!img.hasAttribute("data-nothing-src")) img.setAttribute("data-nothing-src", nothing);
      img.src = s === "normal" ? img.getAttribute("data-normal-src") : img.getAttribute("data-nothing-src");
    });
  };

  applyTheme(
    localStorage.getItem("nm-theme") ||
      (window.matchMedia("(prefers-color-scheme: light)").matches ? "light" : "dark")
  );
  applyStyle(localStorage.getItem("nm-style") || "nothing");

  const themeBtn = document.getElementById("theme-toggle");
  if (themeBtn)
    themeBtn.addEventListener("click", () => {
      const next = root.getAttribute("data-theme") === "light" ? "dark" : "light";
      localStorage.setItem("nm-theme", next);
      applyTheme(next);
    });
  const styleBtn = document.getElementById("style-toggle");
  if (styleBtn)
    styleBtn.addEventListener("click", () => {
      const next = root.getAttribute("data-style") === "normal" ? "nothing" : "normal";
      localStorage.setItem("nm-style", next);
      applyStyle(next);
    });

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
    for (let x = 17; x <= 21; x++) star(x, 4);       // horizontal
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

  // ---- Library preview on home page ----
  const libraryPreview = document.getElementById("library-preview");
  if (libraryPreview) {
    fetch("/api/library?type=template&limit=3&preview=1")
      .then((r) => r.json())
      .then((data) => {
        const items = (data.items || []).slice(0, 3);
        if (!items.length) {
          libraryPreview.innerHTML = '<article class="library-placeholder"><p class="mono-label">NO ITEMS YET</p></article>';
          return;
        }
        libraryPreview.innerHTML = items
          .map(
            (it) => `
            <article class="library-card">
              <p class="library-type mono-label">${esc(it.type || "template")}</p>
              <h3 class="library-title">${esc(it.title || "Untitled")}</h3>
              <p class="library-desc">${esc((it.description || it.summary || "").slice(0, 120))}</p>
              <p class="library-meta mono-label">by ${esc(it.handle || "anonymous")}</p>
            </article>
          `
          )
          .join("");
      })
      .catch(() => {
        libraryPreview.innerHTML = '<article class="library-placeholder"><p class="mono-label">LIBRARY UNAVAILABLE</p></article>';
      });
  }

  function esc(s) {
    return String(s ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }
})();
