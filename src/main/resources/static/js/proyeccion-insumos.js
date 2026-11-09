(() => {
    const formatoCantidad = new Intl.NumberFormat("es-SV", {
        maximumFractionDigits: 2
    });
    const formatoMoneda = new Intl.NumberFormat("es-SV", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const formatoPrecioPresentacion = new Intl.NumberFormat("es-SV", {
        style: "currency",
        currency: "USD",
        minimumFractionDigits: 2,
        maximumFractionDigits: 4
    });

    const formatearCantidad = (valor, unidadOriginal = "") => {
        const cantidadBase = Number(valor);
        const unidadNormalizada = unidadOriginal.toLocaleLowerCase("es-SV");
        if (Math.abs(cantidadBase) >= 1000 && unidadNormalizada === "g") {
            return `${formatoCantidad.format(cantidadBase / 1000)} kg`;
        }
        if (Math.abs(cantidadBase) >= 1000
            && (unidadNormalizada === "ml" || unidadNormalizada === "millilitro")) {
            return `${formatoCantidad.format(cantidadBase / 1000)} L`;
        }
        return `${formatoCantidad.format(cantidadBase)}${unidadOriginal ? ` ${unidadOriginal}` : ""}`;
    };

    document.querySelectorAll("[data-number]").forEach((elemento) => {
        const valor = Number(elemento.dataset.raw);
        const unidadOriginal = elemento.dataset.unidad || "";
        elemento.textContent = formatearCantidad(valor, unidadOriginal);
    });

    document.querySelectorAll("[data-currency]").forEach((elemento) => {
        elemento.textContent = formatoMoneda.format(Number(elemento.dataset.raw));
    });
    document.querySelectorAll("[data-package-currency]").forEach((elemento) => {
        elemento.textContent = formatoPrecioPresentacion.format(Number(elemento.dataset.raw));
    });

    if (window.bootstrap?.Tooltip) {
        document.querySelectorAll("[data-bs-toggle='tooltip']").forEach((elemento) => {
            new window.bootstrap.Tooltip(elemento);
        });
    }

    const resultado = document.getElementById("resultado-proyeccion");
    const tabla = document.getElementById("tabla-proyeccion");
    const errorExportacion = document.getElementById("error-exportacion");
    const obtenerProyeccion = () => {
        if (!resultado || !tabla) {
            throw new Error("No hay una proyección disponible para exportar.");
        }
        const insumos = Array.from(tabla.querySelectorAll("tbody tr")).map((fila) => {
            const celdas = fila.querySelectorAll("td");
            const unidad = celdas[0].querySelector("[data-unidad]")?.dataset.unidad || "";
            const cantidadCompra = celdas[4].querySelector("[data-export-value]");
            const costo = celdas[5].querySelector("[data-currency]");
            return {
                nombre: fila.dataset.insumo,
                neto: celdas[0].querySelector("[data-raw]")?.dataset.raw || "",
                bruto: celdas[1].querySelector("[data-raw]")?.dataset.raw || "",
                totalConMargen: celdas[2].querySelector("[data-raw]")?.dataset.raw || "",
                unidad,
                presentacion: celdas[3].innerText.trim(),
                cantidadComprar: cantidadCompra?.dataset.raw || "",
                costo: costo?.dataset.raw || ""
            };
        });
        const demanda = Array.from(document.querySelectorAll("#tabla-resumen-demanda tbody tr"))
            .map((fila) => {
                const cantidad = fila.querySelector("[data-raw]");
                return {
                    categoria: fila.querySelector("th").innerText.trim(),
                    cantidad: cantidad?.dataset.raw || "",
                    unidad: cantidad?.dataset.unidad || ""
                };
            });
        const compras = Array.from(document.querySelectorAll("#tabla-resumen-compras tbody tr"))
            .map((fila) => {
                const celdas = fila.querySelectorAll("th, td");
                const cantidad = celdas[2].querySelector("[data-raw]");
                const precio = celdas[3].querySelector("[data-raw]");
                const subtotal = celdas[4].querySelector("[data-raw]");
                return {
                    insumo: celdas[0].innerText.trim(),
                    presentacion: celdas[1].innerText.trim(),
                    cantidad: cantidad?.dataset.raw || "",
                    precio: precio?.dataset.raw || "",
                    subtotal: subtotal?.dataset.raw || ""
                };
            });
        return {
            periodo: resultado.dataset.periodo,
            fechaInicio: resultado.dataset.fechaInicio,
            fechaFin: resultado.dataset.fechaFin,
            margen: resultado.dataset.margen,
            costoTotal: resultado.dataset.costoTotal === "null" ? "" : resultado.dataset.costoTotal,
            presupuestoTotal: resultado.dataset.presupuestoTotal === "null" ? "" : resultado.dataset.presupuestoTotal,
            pacientes: resultado.dataset.pacientes,
            racionesPorDia: resultado.dataset.racionesDia,
            advertencias: Array.from(resultado.querySelectorAll(".alert-warning li"))
                .map((elemento) => elemento.innerText.trim()),
            demanda,
            compras,
            insumos
        };
    };

    const avisarErrorExportacion = (error) => {
        errorExportacion.hidden = false;
        errorExportacion.textContent = `No se pudo generar el archivo: ${error.message}`;
        errorExportacion.focus();
    };

    const prepararNombreArchivo = (tipo, proyeccion) =>
        `proyeccion-insumos-${tipo}-${proyeccion.fechaInicio}.`;
    const formatearFecha = (fechaIso) => {
        const [anio, mes, dia] = fechaIso.split("-");
        return `${dia}/${mes}/${anio}`;
    };

    const descargar = (contenido, tipoMime, nombre) => {
        const archivo = new Blob([contenido], { type: tipoMime });
        const enlace = document.createElement("a");
        const url = URL.createObjectURL(archivo);
        enlace.href = url;
        enlace.download = nombre;
        document.body.append(enlace);
        enlace.click();
        enlace.remove();
        window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    };

    const botonPdf = document.getElementById("exportar-pdf");
    if (botonPdf && errorExportacion) {
        botonPdf.addEventListener("click", () => {
            try {
                const proyeccion = obtenerProyeccion();
                const { jsPDF } = window.jspdf || {};
                if (!jsPDF) {
                    throw new Error("La biblioteca PDF no está disponible.");
                }
                const documento = new jsPDF({ orientation: "landscape" });
                documento.setFontSize(16);
                documento.text("Proyección de insumos", 14, 16);
                documento.setFontSize(10);
                documento.text(
                    `${proyeccion.periodo} · ${formatearFecha(proyeccion.fechaInicio)} al ${formatearFecha(proyeccion.fechaFin)} · Margen ${formatoCantidad.format(Number(proyeccion.margen))}%`,
                    14,
                    23
                );
                documento.text(
                    `Pacientes: ${proyeccion.pacientes} · Raciones por día (promedio): ${formatoCantidad.format(Number(proyeccion.racionesPorDia))}`,
                    14,
                    29
                );
                documento.text(
                    `Costo estimado de consumo: ${proyeccion.costoTotal !== "" ? formatoMoneda.format(Number(proyeccion.costoTotal)) : "No disponible"} · Presupuesto estimado de compra: ${proyeccion.presupuestoTotal !== "" ? formatoMoneda.format(Number(proyeccion.presupuestoTotal)) : "No disponible"}`,
                    14,
                    35
                );
                documento.text("Demanda consolidada", 14, 43);
                documento.autoTable({
                    startY: 45,
                    head: [["Grupo", "Demanda"]],
                    body: proyeccion.demanda.map((total) => [
                        total.categoria,
                        `${formatoCantidad.format(Number(total.cantidad))} ${total.unidad}`
                    ]),
                    styles: { font: "helvetica", fontSize: 8 },
                    headStyles: { fillColor: [37, 99, 93] },
                    tableWidth: 120
                });
                let inicioCompra = documento.lastAutoTable.finalY + 9;
                if (inicioCompra > 190) {
                    documento.addPage();
                    inicioCompra = 15;
                }
                documento.text("Detalle de compra por presentación", 14, inicioCompra);
                documento.autoTable({
                    startY: inicioCompra + 2,
                    head: [["Insumo", "Presentación", "Paquetes", "Precio por paquete", "Subtotal"]],
                    body: proyeccion.compras.map((compra) => [
                        compra.insumo,
                        compra.presentacion,
                        compra.cantidad !== "" ? formatoCantidad.format(Number(compra.cantidad)) : "No calculable",
                        compra.precio !== "" ? formatoPrecioPresentacion.format(Number(compra.precio)) : "—",
                        compra.subtotal !== "" ? formatoMoneda.format(Number(compra.subtotal)) : "—"
                    ]),
                    styles: { font: "helvetica", fontSize: 8 },
                    headStyles: { fillColor: [37, 99, 93] }
                });
                let inicioDetalle = documento.lastAutoTable.finalY + 9;
                if (inicioDetalle > 190) {
                    documento.addPage();
                    inicioDetalle = 15;
                }
                documento.text("Detalle de demanda y costo de consumo", 14, inicioDetalle);
                documento.autoTable({
                    startY: inicioDetalle + 2,
                    head: [["Insumo", "Neto", "Bruto", "Total con margen", "Presentación", "A comprar", "Costo"]],
                    body: proyeccion.insumos.map((insumo) => [
                        insumo.nombre,
                        formatearCantidad(insumo.neto, insumo.unidad),
                        formatearCantidad(insumo.bruto, insumo.unidad),
                        formatearCantidad(insumo.totalConMargen, insumo.unidad),
                        insumo.presentacion,
                        insumo.cantidadComprar !== "" ? `${formatoCantidad.format(Number(insumo.cantidadComprar))} ${insumo.presentacion}` : "No calculable",
                        insumo.costo !== "" ? formatoMoneda.format(Number(insumo.costo)) : "—"
                    ]),
                    styles: { font: "helvetica", fontSize: 8 },
                    headStyles: { fillColor: [37, 99, 93] },
                    columnStyles: { 5: { fontStyle: "bold", textColor: [23, 78, 66] } }
                });
                if (proyeccion.advertencias.length > 0) {
                    let posicionAdvertencias = documento.lastAutoTable.finalY + 8;
                    if (posicionAdvertencias > 190) {
                        documento.addPage();
                        posicionAdvertencias = 15;
                    }
                    documento.text(
                        `Advertencias: ${proyeccion.advertencias.join(" ")}`,
                        14,
                        posicionAdvertencias,
                        { maxWidth: 265 }
                    );
                }
                const tipoPeriodo = proyeccion.periodo.includes("30") ? "mensual" : "semanal";
                documento.save(`${prepararNombreArchivo(tipoPeriodo, proyeccion)}pdf`);
            } catch (error) {
                avisarErrorExportacion(error);
            }
        });
    }

    const botonExcel = document.getElementById("exportar-excel");
    if (botonExcel && errorExportacion) {
        botonExcel.addEventListener("click", async () => {
            botonExcel.disabled = true;
            try {
                const proyeccion = obtenerProyeccion();
                if (!window.ExcelJS?.Workbook) {
                    throw new Error("La biblioteca Excel no está disponible.");
                }
                const libro = new window.ExcelJS.Workbook();
                libro.creator = "Sistema de Alimentación Hospitalaria";
                const hoja = libro.addWorksheet("Resumen de compra");
                hoja.mergeCells("A1:E1");
                hoja.getCell("A1").value = "Proyección de insumos";
                hoja.getCell("A1").font = { bold: true, size: 16, color: { argb: "FF173A35" } };
                hoja.addRow(["Período", proyeccion.periodo]);
                hoja.addRow(["Fechas", `${proyeccion.fechaInicio} al ${proyeccion.fechaFin}`]);
                hoja.addRow(["Margen (%)", Number(proyeccion.margen) / 100]);
                hoja.addRow(["Pacientes incluidos", Number(proyeccion.pacientes)]);
                hoja.addRow(["Raciones por día (promedio)", Number(proyeccion.racionesPorDia)]);
                hoja.addRow(["Costo estimado de consumo (USD)", proyeccion.costoTotal !== "" ? Number(proyeccion.costoTotal) : null]);
                hoja.addRow(["Presupuesto estimado de compra (USD)", proyeccion.presupuestoTotal !== "" ? Number(proyeccion.presupuestoTotal) : null]);
                hoja.addRow([]);
                hoja.addRow(["Demanda consolidada"]);
                const encabezadoDemanda = hoja.addRow(["Grupo", "Demanda", "Unidad"]);
                encabezadoDemanda.font = { bold: true, color: { argb: "FFFFFFFF" } };
                encabezadoDemanda.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF25635D" } };
                for (const total of proyeccion.demanda) {
                    hoja.addRow([total.categoria, Number(total.cantidad), total.unidad]);
                }
                hoja.addRow([]);
                hoja.addRow(["Detalle de compra por presentación"]);
                const encabezadoCompra = hoja.addRow([
                    "Insumo", "Presentación", "Paquetes", "Precio por paquete (USD)", "Subtotal (USD)"
                ]);
                encabezadoCompra.font = { bold: true, color: { argb: "FFFFFFFF" } };
                encabezadoCompra.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF25635D" } };
                for (const compra of proyeccion.compras) {
                    hoja.addRow([
                        compra.insumo,
                        compra.presentacion,
                        compra.cantidad !== "" ? Number(compra.cantidad) : null,
                        compra.precio !== "" ? Number(compra.precio) : null,
                        compra.subtotal !== "" ? Number(compra.subtotal) : null
                    ]);
                }
                hoja.columns = [
                    { width: 32 }, { width: 24 }, { width: 16 }, { width: 25 }, { width: 18 }
                ];
                hoja.getCell("B4").numFmt = "0.00%";
                hoja.getCell("B7").numFmt = "$#,##0.00";
                hoja.getCell("B8").numFmt = "$#,##0.00";
                for (let fila = 12; fila < 12 + proyeccion.demanda.length; fila += 1) {
                    hoja.getCell(`B${fila}`).numFmt = "#,##0.##";
                }
                const inicioCompras = 15 + proyeccion.demanda.length;
                for (let fila = inicioCompras; fila < inicioCompras + proyeccion.compras.length; fila += 1) {
                    hoja.getCell(`C${fila}`).numFmt = "#,##0";
                    hoja.getCell(`D${fila}`).numFmt = "$#,##0.0000";
                    hoja.getCell(`E${fila}`).numFmt = "$#,##0.00";
                    hoja.getCell(`C${fila}`).font = { bold: true, color: { argb: "FF174E42" } };
                }
                if (proyeccion.advertencias.length > 0) {
                    hoja.addRow([]);
                    hoja.addRow(["Advertencias"]);
                    proyeccion.advertencias.forEach((advertencia) => hoja.addRow([advertencia]));
                }
                const detalle = libro.addWorksheet("Detalle por insumo");
                const encabezado = detalle.addRow([
                    "Insumo", "Neto", "Unidad", "Bruto", "Total con margen",
                    "Presentación", "A comprar", "Costo (USD)"
                ]);
                encabezado.font = { bold: true, color: { argb: "FFFFFFFF" } };
                encabezado.fill = { type: "pattern", pattern: "solid", fgColor: { argb: "FF25635D" } };
                for (const insumo of proyeccion.insumos) {
                    detalle.addRow([
                        insumo.nombre,
                        Number(insumo.neto),
                        insumo.unidad,
                        Number(insumo.bruto),
                        Number(insumo.totalConMargen),
                        insumo.presentacion,
                        insumo.cantidadComprar !== "" ? Number(insumo.cantidadComprar) : null,
                        insumo.costo !== "" ? Number(insumo.costo) : null
                    ]);
                }
                detalle.columns = [
                    { width: 28 }, { width: 16 }, { width: 12 }, { width: 16 },
                    { width: 20 }, { width: 28 }, { width: 18 }, { width: 16 }
                ];
                for (const fila of detalle.getRows(1, proyeccion.insumos.length + 1) || []) {
                    for (const indice of [2, 4, 5]) {
                        fila.getCell(indice).numFmt = "#,##0.##";
                    }
                    fila.getCell(8).numFmt = "$#,##0.00";
                    fila.getCell(7).font = { bold: true, color: { argb: "FF174E42" } };
                }
                const contenido = await libro.xlsx.writeBuffer();
                descargar(
                    contenido,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    `${prepararNombreArchivo(proyeccion.periodo.includes("30") ? "mensual" : "semanal", proyeccion)}xlsx`
                );
            } catch (error) {
                avisarErrorExportacion(error);
            } finally {
                botonExcel.disabled = false;
            }
        });
    }

    const formulario = document.getElementById("formulario-proyeccion");
    const margenPorcentaje = document.getElementById("margenPorcentaje");
    const margenFraccion = document.getElementById("margenFraccion");
    const botonCalcular = document.getElementById("boton-calcular");
    const estadoCarga = document.getElementById("estado-carga");

    if (formulario && margenPorcentaje && margenFraccion) {
        const actualizarMargen = () => {
            const porcentaje = Number(margenPorcentaje.value);
            const valido = margenPorcentaje.value !== "" && porcentaje >= 0 && porcentaje <= 100;
            margenPorcentaje.setCustomValidity(valido ? "" : "Ingrese un margen entre 0 y 100.");
            margenPorcentaje.setAttribute("aria-invalid", String(!valido));
            if (valido) {
                margenFraccion.value = (porcentaje / 100).toFixed(4);
            }
            return valido;
        };

        margenPorcentaje.addEventListener("input", actualizarMargen);
        formulario.addEventListener("submit", (evento) => {
            if (!actualizarMargen()) {
                evento.preventDefault();
                margenPorcentaje.reportValidity();
                return;
            }
            botonCalcular.disabled = true;
            botonCalcular.textContent = "Calculando…";
            estadoCarga.textContent = "Calculando la proyección de insumos.";
        });
    }
})();
