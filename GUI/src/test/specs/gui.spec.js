
// GUI/src/test/specs/gui.spec.js
const { test, expect } = require('@playwright/test');

const GUI_LOCAL = process.env.GUI_LOCAL ?? 'http://localhost:3000/index.html';
const API_BASE  = process.env.API_BASE  ?? 'http://54.158.15.130:8080';
const USE_MOCK  = (process.env.USE_MOCK ?? '1') === '1'; // ← por defecto enciende mock para que pase

function extractResultsCount(text) {
    const m = String(text).match(/(\d+)/);
    return m ? parseInt(m[1], 10) : 0;
}

test.beforeEach(async ({ page }) => {
    // Logs de consola del navegador: ayuda a ver CORS u otros errores en el reporte
    page.on('console', msg => {
        if (['error', 'warning'].includes(msg.type())) {
            console.log(`[${msg.type().toUpperCase()}]`, msg.text());
        }
    });

    if (USE_MOCK) {
        // Mock de /api/health
        await page.route(`${API_BASE}/api/health`, async route => {
            await route.fulfill({ status: 200, contentType: 'text/plain', body: 'OK' });
        });

        // Mock de cualquier endpoint de tickets
        const base = API_BASE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        await page.route(new RegExp(`${base}/api/graph/.*`), async route => {
            const body = JSON.stringify([
                {
                    vuelo: 'FL-TEST123456',
                    aerolinea: 'DemoAir',
                    precio: 45.82,
                    duracionMin: 147,   // 2h 27m
                    escala: 'PMI',
                    embarque: 4
                }
            ]);
            await route.fulfill({ status: 200, contentType: 'application/json', body });
        });
    }
});

test('Health muestra estado cuando la API responde', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    const healthMsg = page.locator('#healthMsg');
    await expect(healthMsg).toBeVisible();
    await expect(healthMsg).toHaveText(/API conectada \(OK\)|API respondió:|API no disponible/i, { timeout: 15000 });
});

test('Consulta "Todos" rellena la tabla y muestra KPIs (TFN → SDR)', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    // Espera a los inputs por ID (más robusto que placeholder)
    await page.locator('#origen').waitFor({ state: 'visible', timeout: 15000 });
    await page.locator('#destino').waitFor({ state: 'visible', timeout: 15000 });

    // Rellena usando IDs
    await page.fill('#origen', 'TFN');
    await page.fill('#destino', 'SDR');

    // Lanza búsqueda
    await page.getByRole('button', { name: 'Todos' }).click();

    // Espera a que aparezca el mensaje y a que haya filas (>0)
    const searchMsg = page.locator('#searchMsg');
    await expect(searchMsg).toBeVisible({ timeout: 15000 });

    const rows = page.locator('#tablaVuelos tbody tr');
    await expect
        .poll(async () => rows.count(), { timeout: 20000, intervals: [200, 400, 800] })
        .toBeGreaterThan(0);

    // KPIs visibles y precio calculado
    await expect(page.locator('#kpisVuelos')).toBeVisible();
    await expect(page.locator('#kpiPrecioMedio')).not.toHaveText('—');

    // Ruta y resultados
    await expect(page.locator('#pillRuta')).toContainText('TFN → SDR');
    const pillText = await page.locator('#pillResultados').textContent();
    expect(extractResultsCount(pillText)).toBeGreaterThan(0);
});

test('Descargar ticket produce un PDF (TFN → SDR)', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    await page.locator('#origen').waitFor({ state: 'visible', timeout: 15000 });
    await page.locator('#destino').waitFor({ state: 'visible', timeout: 15000 });

    await page.fill('#origen', 'TFN');
    await page.fill('#destino', 'SDR');

    await page.getByRole('button', { name: 'Todos' }).click();

    const rows = page.locator('#tablaVuelos tbody tr');
    await expect
        .poll(async () => rows.count(), { timeout: 20000, intervals: [200, 400, 800] })
        .toBeGreaterThan(0);

    // Descarga PDF
    const downloadPromise = page.waitForEvent('download');
    await page.locator('.btn-descargar').first().click();
    const download = await downloadPromise;

    const suggested = await download.suggestedFilename();
    expect(suggested).toMatch(/ticket-.*\.pdf$/i);

    // Guarda para inspección
    await download.saveAs(`./downloads/${suggested}`);
});
