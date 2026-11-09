(() => {
    const resultado = document.getElementById("resultado-raciones");
    const tablaTotales = document.getElementById("tabla-totales-piso");
    const tablaRaciones = document.getElementById("tabla-raciones");
    const errorExportacion = document.getElementById("error-exportacion-raciones");

    if (!resultado || !tablaTotales || !tablaRaciones || !errorExportacion) {
        return;
    }

    const leerFilas = (tabla, columnasNumericas = []) => Array.from(tabla.querySelectorAll("tbody tr"))
        .filter((fila) => fila.cells.length > 1)
        .map((fila) => Array.from(fila.cells, (celda, indice) => {
            const valor = celda.innerText.trim();
            return columnasNumericas.includes(indice) ? Number(valor) : valor;
        }));
    const nombreBase = `raciones-servidas-${resultado.dataset.periodo}`;
    const mostrarError = (error) => {
        errorExportacion.hidden = false;
        errorExportacion.textContent = `No se pudo generar el archivo: ${error.message}`;
        errorExportacion.focus();
    };
    const descargar = (contenido, mime, nombre) => {
        const url = URL.createObjectURL(new Blob([contenido], { type: mime }));
        const enlace = document.createElement("a");
        enlace.href = url;
        enlace.download = nombre;
        enlace.click();
        URL.revokeObjectURL(url);
    };

    document.getElementById("exportar-pdf-raciones")?.addEventListener("click", () => {
        try {
            const jsPDF = window.jspdf?.jsPDF;
            if (!jsPDF) {
                throw new Error("La librería PDF no está disponible.");
            }
            const documento = new jsPDF({ orientation: "landscape" });
            documento.text(`Raciones servidas por piso — ${resultado.dataset.periodo}`, 14, 15);
            documento.text(`Acumulado al ${resultado.dataset.fechaCorte}`, 14, 22);
            documento.autoTable({
                startY: 28,
                head: [["Piso", "Raciones"]],
                body: leerFilas(tablaTotales, [1])
            });
            documento.autoTable({
                startY: documento.lastAutoTable.finalY + 8,
                head: [["Piso", "Tiempo de comida", "Plato", "Menú", "Raciones"]],
                body: leerFilas(tablaRaciones, [4])
            });
            documento.save(`${nombreBase}.pdf`);
        } catch (error) {
            mostrarError(error);
        }
    });

    document.getElementById("exportar-excel-raciones")?.addEventListener("click", async () => {
        try {
            if (!window.ExcelJS?.Workbook) {
                throw new Error("La librería Excel no está disponible.");
            }
            const libro = new window.ExcelJS.Workbook();
            const resumen = libro.addWorksheet("Totales por piso");
            resumen.addRow(["Piso", "Raciones"]);
            leerFilas(tablaTotales, [1]).forEach((fila) => resumen.addRow(fila));
            const detalle = libro.addWorksheet("Detalle");
            detalle.addRow(["Piso", "Tiempo de comida", "Plato", "Menú", "Raciones"]);
            leerFilas(tablaRaciones, [4]).forEach((fila) => detalle.addRow(fila));
            resumen.getRow(1).font = { bold: true };
            detalle.getRow(1).font = { bold: true };
            resumen.columns = [{ width: 28 }, { width: 14 }];
            detalle.columns = [{ width: 28 }, { width: 24 }, { width: 32 }, { width: 32 }, { width: 14 }];
            const contenido = await libro.xlsx.writeBuffer();
            descargar(contenido, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                `${nombreBase}.xlsx`);
        } catch (error) {
            mostrarError(error);
        }
    });
})();
