// refs:　https://greasyfork.org/en/scripts/533807-youtube-cpu-tamer-hybrid-edition-improved/code

(() => {

  /**
   * Guard – avoid double‑instantiation on SPA navigation
   */
  const FLAG = "__yt_cpu_tamer_hybrid_running__";
  if (window[FLAG]) return;
  window[FLAG] = true;

  /*************************************************************************
   * Helpers
   *************************************************************************/
  const nextAnimationFrame = () => new Promise(r => requestAnimationFrame(r));

  const waitForDocReady = async () => {
    while (!document.documentElement || !document.head) {
      await nextAnimationFrame();
    }
  };

  /**
   * A thin extended‑Promise that exposes resolve()/reject() – convenient for
   * bridging observer + rAF based triggers without extra closures.
   */
  const PromiseExt = (() => {
    let _res, _rej;
    const shim = (r, j) => {
      _res = r;
      _rej = j;
    };
    return class extends Promise {
      constructor(cb = shim) {
        super(cb);
        if (cb === shim) {
          /** @type {(value?: unknown) => void} */
          // @ts-ignore – dynamically injected
          this.resolve = _res;
          /** @type {(reason?: any) => void} */
          // @ts-ignore
          this.reject = _rej;
        }
      }
    };
  })();

  /*************************************************************************
   * Main
   *************************************************************************/
  const setup = async () => {
    await waitForDocReady();

    /***** 1. Create helper iframe that owns lightweight timers *****/
    const FRAME_ID = "yt-cpu-tamer-timer-frame";
    let frame = document.getElementById(FRAME_ID);
    if (!frame) {
      frame = document.createElement("iframe");
      frame.id = FRAME_ID;
      frame.style.display = "none";
      // Allow both same‑origin and script execution so that callbacks routed
      // through the iframe don’t hit the Chrome sandbox error.
      frame.sandbox = "allow-same-origin allow-scripts";
      // Use srcdoc to keep the iframe same‑origin (& blank) in all browsers.
      frame.srcdoc = "<!doctype html><title>yt-cpu-tamer</title>";
      document.documentElement.appendChild(frame);
    }
    // Wait until the inner window is ready.
    while (!frame.contentWindow) {
      await nextAnimationFrame();
    }

    const {
      requestAnimationFrame: frameRAF,
      setTimeout: frameSetTimeout,
      setInterval: frameSetInterval,
      clearTimeout: frameClearTimeout,
      clearInterval: frameClearInterval
    } = /** @type {Window & typeof globalThis} */ (frame.contentWindow);

    /***** 2. Trigger generator – rAF when visible, MutationObserver otherwise *****/
    const dummy = document.createElement("div");
    dummy.style.display = "none";
    document.documentElement.appendChild(dummy);

    /** @returns {(cb: () => void) => Promise<void>} */
    const makeHybridTrigger = () => {
      if (document.visibilityState === "visible") {
        return cb => {
          const p = new PromiseExt();
          requestAnimationFrame(p.resolve);
          return p.then(cb);
        };
      } else {
        return cb => {
          const p = new PromiseExt();
          const MO = new MutationObserver(() => {
            MO.disconnect();
            p.resolve();
          });
          MO.observe(dummy, { attributes: true });
          dummy.setAttribute("data-yt-cpu-tamer", Math.random().toString(36));
          return p.then(cb);
        };
      }
    };

    /** @type {(cb: () => void) => Promise<void>} */
    let currentTrigger = makeHybridTrigger();
    document.addEventListener("visibilitychange", () => {
      currentTrigger = makeHybridTrigger();
    });

    /***** 3. Timer patching *****/
    const activeTimeouts = new Set();
    const activeIntervals = new Set();

    /**
     * Wrap native timer so that:
     *   – scheduling is done with *iframe* timers (very cheap)
     *   – execution is throttled by currentTrigger
     *   – callback runs in the *main* window realm (fn.apply(window,…))
     */
    const makeTimer = (nativeTimer, pool) => {
      return function patchedTimer(fn, delay = 0, ...args) {
        if (typeof fn !== "function") return nativeTimer(fn, delay, ...args);
        const id = nativeTimer(() => {
          currentTrigger(() => fn.apply(window, args));
        }, delay);
        pool.add(id);
        return id;
      };
    };

    const makeClear = (nativeClear, pool) => id => {
      if (pool.has(id)) pool.delete(id);
      nativeClear(id);
    };

    /**
     * Apply / re‑apply the patches (re‑applied on yt‑navigate‑finish).
     */
    const patchTimers = () => {
      window.setTimeout = makeTimer(frameSetTimeout, activeTimeouts);
      window.setInterval = makeTimer(frameSetInterval, activeIntervals);
      window.clearTimeout = makeClear(frameClearTimeout, activeTimeouts);
      window.clearInterval = makeClear(frameClearInterval, activeIntervals);

      // Align Function.prototype.toString() so that devtools show native code
      const mirrorToString = (patched, native) => {
        try {
          patched.toString = native.toString.bind(native);
        } catch {/* ignore */}
      };
      mirrorToString(window.setTimeout, frameSetTimeout);
      mirrorToString(window.setInterval, frameSetInterval);
      mirrorToString(window.clearTimeout, frameClearTimeout);
      mirrorToString(window.clearInterval, frameClearInterval);

      console.log("[YouTube CPU Tamer – Hybrid Edition] Timers patched");
    };

    // Initial patch (DOMContentLoaded OR immediate if already interactive).
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", patchTimers, { once: true });
    } else {
      patchTimers();
    }

    /***** 4. Re‑patch on SPA navigations *****/
    window.addEventListener("yt-navigate-finish", () => {
      console.log("[YouTube CPU Tamer] yt-navigate-finish – re‑applying patch");
      patchTimers();
    });
  };

  setup().catch(err => console.error("[YouTube CPU Tamer] setup failed", err));
})();