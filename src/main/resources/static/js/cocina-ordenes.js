// SAH-2: recibe por Server-Sent Events las prescripciones aprobadas.
(function () {
    const script = document.getElementById('script-ordenes');
    const cuerpo = document.getElementById('ordenes');
    const estado = document.getElementById('estado-conexion');
    if (!script || !cuerpo || !estado || !window.EventSource) {
        return;
    }

    function mostrarEstado(texto, clase) {
        estado.textContent = texto;
        estado.className = 'alert ' + clase;
    }

    function celda(texto) {
        const td = document.createElement('td');
        td.textContent = texto; // textContent evita interpretar los datos como HTML.
        return td;
    }

    function agregarOrden(orden) {
        const vacio = document.getElementById('sin-ordenes');
        if (vacio) {
            vacio.remove();
        }
        const fila = document.createElement('tr');
        fila.append(
            celda(window.formatearFechaLocal ? window.formatearFechaLocal(orden.fechaAutorizacion) : orden.fechaAutorizacion),
            celda(orden.paciente + ' (' + orden.codigoPaciente + ')'),
            celda(orden.ubicacion),
            celda(orden.regimen),
            celda(orden.estado));
        cuerpo.prepend(fila);
    }

    const fuente = new EventSource(script.dataset.eventos);
    fuente.addEventListener('conectado', function () {
        mostrarEstado('Conectado. Esperando órdenes aprobadas.', 'alert-success');
    });
    fuente.addEventListener('prescripcion-aprobada', function (evento) {
        agregarOrden(JSON.parse(evento.data));
    });
    fuente.onerror = function () {
        if (fuente.readyState === EventSource.CLOSED) {
            mostrarEstado('La conexión se cerró. Recargue la página o inicie sesión nuevamente.', 'alert-danger');
        } else {
            mostrarEstado('Reconectando…', 'alert-warning');
        }
    };
})();
