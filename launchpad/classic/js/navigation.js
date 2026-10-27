// Keep shared product request parameters when switching between layouts.
document.querySelectorAll('[data-modern-version]').forEach(link => {
    link.search = window.location.search;
});
