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
            el.textContent = m + ':' + (ss < 10 ? '0' : '') + ss;
        }, 500);
    }

    /** Stops the live timer if any (called when the end-of-game dialog opens). */
    function stopTimer() {
        if (window._gameTimer) clearInterval(window._gameTimer);
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
                server.handleKeyboardFlip(focus);
                e.preventDefault();
                return;
            }

            if (moved) {
                if (isControl && (tag === 'VAADIN-BUTTON' || tag === 'BUTTON') && ae.blur) ae.blur();
                e.preventDefault();
                server.setKeyboardFocus(focus);
            }
        };
        document.addEventListener('keydown', window._keyListener);
    }

    return { startTimer: startTimer, stopTimer: stopTimer, initKeyboard: initKeyboard };
})();
