/*
 * DHBW Memory — client-side helpers.
 *
 * Loaded once by GameView (Page.addJavaScript("/game.js")) and exposed as
 * window.dhbwMemory.{startTimer, stopTimer, initKeyboard}. The Java side
 * calls these via Element.executeJs() so this file is the single source of
 * truth for the browser-facing logic — no inline JS strings in Java.
 */
window.dhbwMemory = (function () {

    /**
     * Starts a 500ms interval that paints elapsed game time into #game-timer.
     * Idempotent: replaces any previous interval so callers can re-init safely.
     *
     * @param {number} elapsedSeconds  seconds already elapsed when the timer is (re)started
     */
    function startTimer(elapsedSeconds) {
        let el = document.getElementById('game-timer');
        const start = Date.now() - elapsedSeconds * 1000;
        if (window._gameTimer) clearInterval(window._gameTimer);
        window._gameTimer = setInterval(function () {
            if (!el) el = document.getElementById('game-timer');
            if (!el) return;
            const s = Math.floor((Date.now() - start) / 1000);
            const m = Math.floor(s / 60);
            const ss = s % 60;
            const timeStr = m + ':' + (ss < 10 ? '0' : '') + ss;
            el.textContent = timeStr;
            // Also surface the live time in the browser tab so it's glanceable
            // while the user is in another window.
            document.title = 'DHBW Memory · ' + timeStr;
        }, 500);
    }

    /** Stops the live timer if any and restores the plain tab title. */
    function stopTimer() {
        if (window._gameTimer) clearInterval(window._gameTimer);
        document.title = 'DHBW Memory';
    }

    /**
     * Registers a document-level keydown listener for arrow-key board navigation
     * and Space/Enter card flipping.
     *
     * Skips events while a vaadin-dialog-overlay is open OR a focused button/input
     * would otherwise consume Space/Enter (this prevents the help button from
     * re-triggering when the user returns to keyboard nav).
     *
     * On any arrow press while a button is focused, that button is blurred so
     * subsequent Space/Enter routes to the board instead of the button.
     *
     * @param {object} server  Vaadin server-callable proxy (this.$server on the view)
     * @param {number} size    edge length of the grid (4 or 6)
     */
    function initKeyboard(server, size) {
        const total = size * size;
        let focus = 0;
        if (window._keyListener) document.removeEventListener('keydown', window._keyListener);
        if (window._mouseListener) document.removeEventListener('mousedown', window._mouseListener);

        // The focus ring is only drawn while body.keyboard-mode is on.
        // We enter that mode on the first board-relevant key press and leave
        // it the moment the user clicks anything with the mouse.
        window._mouseListener = function () {
            document.body.classList.remove('keyboard-mode');
        };
        document.addEventListener('mousedown', window._mouseListener);

        window._keyListener = function (e) {
            if (document.querySelector('vaadin-dialog-overlay')) return;

            const ae = document.activeElement;
            const tag = ae ? ae.tagName : '';
            const isControl =
                tag === 'INPUT' || tag === 'TEXTAREA' ||
                tag === 'VAADIN-BUTTON' || tag === 'BUTTON' ||
                tag === 'VAADIN-TEXT-FIELD' || tag === 'VAADIN-SELECT';
            const isFlip = e.key === ' ' || e.key === 'Enter';

            if (isFlip && isControl) return;

            let moved = false;
            if      (e.key === 'ArrowRight') { focus = (focus + 1) % total;                moved = true; }
            else if (e.key === 'ArrowLeft')  { focus = (focus - 1 + total) % total;        moved = true; }
            else if (e.key === 'ArrowDown')  { focus = Math.min(focus + size, total - 1);  moved = true; }
            else if (e.key === 'ArrowUp')    { focus = Math.max(focus - size, 0);          moved = true; }
            else if (isFlip) {
                document.body.classList.add('keyboard-mode');
                server.handleKeyboardFlip(focus);
                e.preventDefault();
                return;
            }

            if (moved) {
                if (isControl && (tag === 'VAADIN-BUTTON' || tag === 'BUTTON') && ae.blur) ae.blur();
                e.preventDefault();
                document.body.classList.add('keyboard-mode');
                server.setKeyboardFocus(focus);
            }
        };
        document.addEventListener('keydown', window._keyListener);
    }

    /**
     * Wires arrow-key navigation across every segmented control on the page.
     *
     * Each [role="radiogroup"] gets one keydown listener; arrow keys move
     * focus to the previous/next [role="radio"] sibling AND click it so the
     * Vaadin-side value updates immediately. Idempotent — guarded by the
     * data-seg-init attribute so calling it again on re-render is a no-op.
     */
    function initSegmented() {
        document.querySelectorAll('[role="radiogroup"]:not([data-seg-init])').forEach(function (group) {
            group.setAttribute('data-seg-init', '1');
            group.addEventListener('keydown', function (e) {
                if (e.key !== 'ArrowLeft' && e.key !== 'ArrowRight'
                    && e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return;
                const items = Array.from(group.querySelectorAll('[role="radio"]'));
                const i = items.indexOf(document.activeElement);
                if (i === -1) return;
                e.preventDefault();
                const forward = e.key === 'ArrowRight' || e.key === 'ArrowDown';
                const next = (i + (forward ? 1 : -1) + items.length) % items.length;
                items[next].focus();
                items[next].click();
            });
        });
    }

    // ── Theme switching ──────────────────────────────────────────────────
    //
    // Three user-facing modes: 'light', 'dark', 'system'. The preference is
    // stored in localStorage; the actually-applied theme is mirrored onto
    // <html theme="…"> so both Lumo components and our CSS variables react.

    const THEME_KEY = 'dhbw-memory-theme';

    function resolveTheme(pref) {
        if (pref === 'system') {
            return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
        }
        return pref;
    }

    function applyTheme(pref) {
        document.documentElement.setAttribute('theme', resolveTheme(pref));
    }

    /** Persists the chosen mode and applies it immediately. */
    function setTheme(pref) {
        localStorage.setItem(THEME_KEY, pref);
        applyTheme(pref);
    }

    /** Returns the user's stored preference (defaults to 'system'). */
    function getThemePref() {
        return localStorage.getItem(THEME_KEY) || 'system';
    }

    // Re-resolve when the OS theme flips and the user is on 'system' mode.
    // Idempotent: safe to call initKeyboard again or reload — only the latest
    // listener is active because we register via matchMedia (not a free addEventListener
    // loop that could accumulate).
    if (!window._themeMqListenerRegistered) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
            if (getThemePref() === 'system') applyTheme('system');
        });
        window._themeMqListenerRegistered = true;
    }

    // ── Confetti ─────────────────────────────────────────────────────────
    //
    // Tiny canvas confetti — no dependency. Used on the end-of-game dialog.
    // Particles fall under gravity for ~4 s, then the canvas is removed so
    // it doesn't accumulate in the DOM across replays.

    function showConfetti() {
        const canvas = document.createElement('canvas');
        canvas.style.cssText =
            'position:fixed;top:0;left:0;width:100vw;height:100vh;' +
            'pointer-events:none;z-index:9999';
        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;
        document.body.appendChild(canvas);

        const ctx = canvas.getContext('2d');
        const colors = ['#7BE495', '#3FBF7F', '#A89BFF', '#FFD27A', '#FF8FA0', '#FAFAF7'];
        const particles = [];
        for (let i = 0; i < 160; i++) {
            particles.push({
                x: Math.random() * canvas.width,
                y: -10 - Math.random() * 200,
                vx: (Math.random() - 0.5) * 5,
                vy: 2 + Math.random() * 4,
                size: 5 + Math.random() * 7,
                color: colors[Math.floor(Math.random() * colors.length)],
                rot: Math.random() * Math.PI * 2,
                vrot: (Math.random() - 0.5) * 0.25
            });
        }

        const start = Date.now();
        function frame() {
            ctx.clearRect(0, 0, canvas.width, canvas.height);
            for (const p of particles) {
                p.vy += 0.08; // gravity
                p.x += p.vx;
                p.y += p.vy;
                p.rot += p.vrot;
                ctx.save();
                ctx.translate(p.x, p.y);
                ctx.rotate(p.rot);
                ctx.fillStyle = p.color;
                ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
                ctx.restore();
            }
            if (Date.now() - start < 4000) {
                requestAnimationFrame(frame);
            } else {
                canvas.remove();
            }
        }
        frame();
    }

    return {
        startTimer: startTimer,
        stopTimer: stopTimer,
        initKeyboard: initKeyboard,
        initSegmented: initSegmented,
        setTheme: setTheme,
        getThemePref: getThemePref,
        showConfetti: showConfetti
    };
})();
