export async function copyText(text) {
    try {
        if (navigator.clipboard) {
            await navigator.clipboard.writeText(text);
            return;
        }
    } catch { /* Fall back for browsers that deny clipboard access. */
    }
    const previous = document.activeElement;
    const area = document.createElement('textarea');
    area.value = text;
    area.style.position = 'fixed';
    area.style.opacity = '0';
    document.body.append(area);
    try {
        area.select();
        if (!document.execCommand('copy')) throw new Error('Clipboard access was denied.');
    } finally {
        area.remove();
        previous?.focus();
    }
}
