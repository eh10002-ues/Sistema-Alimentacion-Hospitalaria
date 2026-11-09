const fs = require("node:fs");
const path = require("node:path");

const raiz = path.resolve(__dirname, "..");
const destino = path.join(raiz, "src", "main", "resources", "static", "vendor", "exportacion");
const archivos = [
    ["exceljs/dist/exceljs.min.js", "exceljs.min.js"],
    ["exceljs/LICENSE", "LICENSE.exceljs"],
    ["jspdf/dist/jspdf.umd.min.js", "jspdf.umd.min.js"],
    ["jspdf/LICENSE", "LICENSE.jspdf"],
    ["jspdf-autotable/dist/jspdf.plugin.autotable.min.js", "jspdf.plugin.autotable.min.js"],
    ["jspdf-autotable/LICENSE.txt", "LICENSE.jspdf-autotable"]
];

fs.mkdirSync(destino, { recursive: true });
for (const [origenRelativo, nombreDestino] of archivos) {
    const origen = path.join(raiz, "node_modules", origenRelativo);
    if (!fs.existsSync(origen)) {
        throw new Error(`No se encontró el archivo requerido: ${origenRelativo}`);
    }
    fs.copyFileSync(origen, path.join(destino, nombreDestino));
}
