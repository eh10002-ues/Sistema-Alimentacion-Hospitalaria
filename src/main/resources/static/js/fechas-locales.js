// Las fechas se envían en UTC (ISO-8601); se muestran en la hora local del navegador.
window.formatearFechaLocal = function (iso) {
    const fecha = new Date(iso);
    return Number.isNaN(fecha.getTime()) ? iso
        : fecha.toLocaleString('es', { dateStyle: 'short', timeStyle: 'short' });
};

document.querySelectorAll('time[data-fecha-local]').forEach(function (elemento) {
    elemento.textContent = window.formatearFechaLocal(elemento.getAttribute('datetime'));
});
