// SAH-9: exporta a PDF el reporte visible con jsPDF y AutoTable.
// Lee el encabezado de los atributos data-reporte-* y cada tabla marcada con data-reporte-tabla.
(function () {
    const datos = document.getElementById("reporte-datos");
    const boton = document.getElementById("exportar-pdf");
    const error = document.getElementById("error-exportacion");
    if (!datos || !boton) {
        return;
    }

    const MARGEN = 14;
    const COLOR_ENCABEZADO = [37, 99, 93];
    const COLOR_PIE = [232, 240, 238];
    const COLOR_TEXTO_PIE = [23, 58, 53];

    const formatearFecha = (iso) => {
        const fecha = new Date(iso);
        return Number.isNaN(fecha.getTime()) ? iso
            : fecha.toLocaleString("es", { dateStyle: "short", timeStyle: "short" });
    };

    const mostrarError = (causa) => {
        if (!error) {
            return;
        }
        error.textContent = `No se pudo generar el PDF. ${causa.message}`;
        error.hidden = false;
        error.focus();
    };

    const escribirLineas = (documento, texto, y, ancho) => {
        const lineas = documento.splitTextToSize(texto, ancho);
        documento.text(lineas, MARGEN, y);
        return y + lineas.length * 4.5;
    };

    boton.addEventListener("click", () => {
        if (error) {
            error.hidden = true;
        }
        try {
            const { jsPDF } = window.jspdf || {};
            if (!jsPDF) {
                throw new Error("La biblioteca PDF no está disponible.");
            }
            const documento = new jsPDF({ orientation: "landscape" });
            if (typeof documento.autoTable !== "function") {
                throw new Error("El complemento de tablas PDF no está disponible.");
            }
            const meta = datos.dataset;
            const ancho = documento.internal.pageSize.getWidth() - MARGEN * 2;
            const alto = documento.internal.pageSize.getHeight();

            documento.setFont("helvetica", "bold");
            documento.setFontSize(15);
            let y = escribirLineas(documento, `${meta.reporteCodigo} · ${meta.reporteTitulo}`, 16, ancho) + 2;
            documento.setFont("helvetica", "normal");
            documento.setFontSize(9);
            if (meta.reporteFiltros) {
                y = escribirLineas(documento, meta.reporteFiltros, y, ancho);
            }
            y = escribirLineas(documento,
                `Generado por ${meta.reporteGeneradoPor} el ${formatearFecha(meta.reporteGeneradoEn)}`, y, ancho);
            if (meta.reporteNota) {
                documento.setFont("helvetica", "italic");
                y = escribirLineas(documento, meta.reporteNota, y, ancho);
                documento.setFont("helvetica", "normal");
            }
            document.querySelectorAll("[data-reporte-resumen]").forEach((resumen) => {
                documento.setFont("helvetica", "bold");
                y = escribirLineas(documento, resumen.textContent.trim().replace(/\s+/g, " "), y + 1, ancho);
                documento.setFont("helvetica", "normal");
            });
            y += 3;

            const tablas = document.querySelectorAll("table[data-reporte-tabla]");
            if (tablas.length === 0) {
                escribirLineas(documento, "Sin resultados con los filtros seleccionados.", y + 2, ancho);
            }
            tablas.forEach((tabla) => {
                const titulo = tabla.caption ? tabla.caption.textContent.trim() : "";
                if (y > alto - 30) {
                    documento.addPage();
                    y = 16;
                }
                if (titulo) {
                    documento.setFont("helvetica", "bold");
                    documento.setFontSize(11);
                    documento.text(titulo, MARGEN, y + 3);
                    documento.setFont("helvetica", "normal");
                    documento.setFontSize(9);
                    y += 6;
                }
                let tablaPdf = tabla;
                if (tabla.dataset.reporteFirma === "true") {
                    tablaPdf = tabla.cloneNode(true);
                    const encabezadoFirma = tablaPdf.querySelector("thead tr");
                    if (encabezadoFirma) {
                        encabezadoFirma.appendChild(document.createElement("th")).textContent = "firma";
                    }
                    tablaPdf.querySelectorAll("tbody tr").forEach((fila) => {
                        fila.appendChild(document.createElement("td"));
                    });
                }
                documento.autoTable({
                    html: tablaPdf,
                    startY: y,
                    margin: { left: MARGEN, right: MARGEN },
                    styles: { font: "helvetica", fontSize: 8 },
                    headStyles: { fillColor: COLOR_ENCABEZADO },
                    footStyles: { fillColor: COLOR_PIE, textColor: COLOR_TEXTO_PIE, fontStyle: "bold" },
                    showFoot: "lastPage"
                });
                y = documento.lastAutoTable.finalY + 8;
            });

            const paginas = documento.internal.getNumberOfPages();
            for (let pagina = 1; pagina <= paginas; pagina += 1) {
                documento.setPage(pagina);
                documento.setFontSize(8);
                documento.text(`${meta.reporteCodigo} · Página ${pagina} de ${paginas}`,
                    documento.internal.pageSize.getWidth() - MARGEN, alto - 8, { align: "right" });
            }
            documento.save(`${meta.reporteArchivo}.pdf`);
        } catch (causa) {
            mostrarError(causa);
        }
    });
})();
