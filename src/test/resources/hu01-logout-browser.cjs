// Prueba opcional sin capturas, vídeos, HAR ni almacenamiento del estado del navegador.
const fs = require('node:fs');
let etapa = 'inicio';
let browser;
(async () => {
  const lineas = fs.readFileSync(0, 'utf8').trim().split('\n');
  const base = lineas.shift();
  if (!/^http:\/\/127\.0\.0\.1:\d+$/.test(base)) throw new Error('Entorno de prueba inválido.');
  const { chromium } = require(process.argv[2]);
  browser = await chromium.launch({ channel: 'chrome', headless: true });
  for (const linea of lineas) {
    const [rol, nombre, clave, etiqueta] = linea.split('\t');
    etapa = 'login y dashboard: ' + rol;
    const context = await browser.newContext();
    const page = await context.newPage();
    page.setDefaultTimeout(10000);
    await page.goto(base + '/login');
    await page.locator('#username').fill(nombre);
    await page.locator('#password').fill(clave);
    await Promise.all([page.waitForURL('**/dashboard'), page.getByRole('button', { name: 'Ingresar', exact: true }).click()]);
    if (await page.locator('.rol-panel').innerText() !== etiqueta) throw new Error('Panel incorrecto.');
    const enlacesAdmin = await page.locator('a[href="/admin/usuarios"]').count();
    if ((rol === 'ROLE_ADMIN') !== (enlacesAdmin > 0)) throw new Error('Navegación incorrecta.');
    if (rol !== 'ROLE_ADMIN') {
      const respuesta = await page.goto(base + '/admin/usuarios');
      if (respuesta.status() !== 403) throw new Error('Acceso incorrecto.');
      await page.getByRole('heading', { name: 'Acceso denegado' }).waitFor();
    }
    await page.goto(base + '/dashboard');
    const anterior = (await context.cookies()).find(cookie => cookie.name === 'JSESSIONID')?.value;
    if (!anterior) throw new Error('Sesión ausente.');
    etapa = 'logout: ' + rol;
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await page.waitForURL('**/login?logout');
    if ((await context.cookies()).some(cookie => cookie.name === 'JSESSIONID' && cookie.value === anterior)) {
      throw new Error('Cookie anterior conservada.');
    }
    etapa = 'historial: ' + rol;
    await page.goBack({ waitUntil: 'domcontentloaded' });
    if (await page.locator('#funciones-titulo').isVisible()) throw new Error('Panel recuperado del historial.');
    etapa = 'reutilización de sesión: ' + rol;
    const replay = await context.request.get(base + '/dashboard', { headers: { Cookie: 'JSESSIONID=' + anterior }, maxRedirects: 0 });
    if (replay.status() !== 302 || !replay.headers().location.endsWith('/login')) throw new Error('Sesión aceptada.');
    await context.close();
  }
  console.log('Correcto: cinco roles, navegación, acceso denegado, logout, historial y sesión anterior. Sin capturas.');
})().catch(() => {
  // Las excepciones originales de Playwright pueden contener valores de formularios.
  console.error('Falló la prueba de navegador sin capturas. Etapa: ' + etapa);
  process.exitCode = 1;
}).finally(async () => { if (browser) await browser.close(); });
