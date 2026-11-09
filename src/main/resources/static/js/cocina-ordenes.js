// SAH-2: recibe por Server-Sent Events las prescripciones aprobadas.
// D4: mantiene la tabla de menús aprobados vigentes (asignaciones de menú), sin duplicar filas.
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

    const menus = document.getElementById('menus-aprobados');

    // Una fila por asignación: la carga inicial, la reconsulta y el evento pueden traer la misma.
    function agregarMenu(orden) {
        if (!menus || menus.querySelector('tr[data-asignacion-id="' + orden.asignacionId + '"]')) {
            return;
        }
        const vacio = document.getElementById('sin-menus');
        if (vacio) {
            vacio.remove();
        }
        const fila = document.createElement('tr');
        fila.dataset.asignacionId = orden.asignacionId;
        fila.append(
            celda(orden.paciente + ' (' + orden.codigoPaciente + ')'),
            celda(orden.ubicacion),
            celda(orden.menu),
            celda(orden.regimen),
            celda(orden.fechaInicio + ' – ' + (orden.fechaFin || 'sin fecha de fin')));
        menus.append(fila);
    }

    // Recupera las aprobaciones ocurridas mientras el canal estaba desconectado.
    function reconsultarMenus() {
        if (!menus || !window.fetch) {
            return;
        }
        fetch(menus.dataset.menus, {headers: {'Accept': 'application/json'}})
            .then(function (respuesta) { return respuesta.ok ? respuesta.json() : []; })
            .then(function (ordenes) { ordenes.forEach(agregarMenu); })
            .catch(function () { /* la tabla conserva lo ya mostrado */ });
    }

    const fuente = new EventSource(script.dataset.eventos);
    fuente.addEventListener('conectado', function () {
        mostrarEstado('Conectado. Esperando órdenes aprobadas.', 'alert-success');
        reconsultarMenus();
    });
    fuente.addEventListener('asignacion-menu-aprobada', function (evento) {
        agregarMenu(JSON.parse(evento.data));
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
