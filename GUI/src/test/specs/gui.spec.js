
// GUI/src/test/specs/gui.spec.js
const { test, expect } = require('@playwright/test');

const GUI_LOCAL = process.env.GUI_LOCAL ?? 'http://localhost:3000/index.html';
const API_BASE  = process.env.API_BASE  ?? 'http://54.158.15.130:8080';
const USE_MOCK  = (process.env.USE_MOCK ?? '1') === '1'; // mock ON por defecto

function extractResultsCount(text) {
    const m = String(text).match(/(\d+)/);
    return m ? parseInt(m[1], 10) : 0;
}

test.beforeEach(async ({ page }) => {
    page.on('console', msg => {
        if (['error', 'warning'].includes(msg.type())) {
            console.log(`[${msg.type().toUpperCase()}]`, msg.text());
        }
    });
    page.on('pageerror', err => {
        console.log('[PAGEERROR]', err.message);
    });

    if (USE_MOCK) {
        // Mock /api/health
        await page.route(`${API_BASE}/api/health`, async route => {
            await route.fulfill({ status: 200, contentType: 'text/plain', body: 'OK' });
        });

        // Mock /api/graph/**
        const base = API_BASE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
        await page.route(new RegExp(`${base}/api/graph/.*`), async route => {
            const body = JSON.stringify([
                {
                    vuelo: 'FL-TEST123456',
                    aerolinea: 'DemoAir',
                    precio: 45.82,
                    duracionMin: 147,
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
    await expect(healthMsg).toBeVisible({ timeout: 15000 });
    await expect(healthMsg).toHaveText(/API conectada \(OK\)|API respondió:|API no disponible/i, { timeout: 15000 });
});

async function setRutaTFN_SDR(page) {
    // Espera a que estén en DOM
    await page.waitForSelector('#origen',  { state: 'attached', timeout: 15000 });
    await page.waitForSelector('#destino', { state: 'attached', timeout: 15000 });

    // Diagnóstico del formulario y captura
    const info = await page.evaluate(() => {
        const ids = Array.from(document.querySelectorAll('input')).map(i => i.id || '(sin id)');
        return { inputs: ids, count: ids.length };
    });
    console.log('[FORM] inputs:', info.inputs, 'count:', info.count);
    await page.screenshot({ path: 'debug-form.png', fullPage: true });

    // Rellenar por script (no depende de "visible")
    await page.evaluate(() => {
        const set = (id, val) => {
            const el = document.getElementById(id);
            if (el) {
                el.value = val;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
            }
        };
        set('origen', 'TFN');
        set('destino', 'SDR');
    });
}

test('Consulta "Todos" rellena la tabla y muestra KPIs (TFN → SDR)', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    await setRutaTFN_SDR(page);
    await page.getByRole('button', { name: 'Todos' }).click();

    // Espera a que haya filas (>0)
    const rows = page.locator('#tablaVuelos tbody tr');
    await expect
        .poll(() => rows.count(), { timeout: 20000, intervals: [200, 400, 800] })
        .toBeGreaterThan(0);

    // KPIs visibles
    await expect(page.locator('#kpisVuelos')).toBeVisible({ timeout: 10000 });
    await expect(page.locator('#kpiPrecioMedio')).not.toHaveText('—', { timeout: 10000 });

    // Ruta y resultados
    await expect(page.locator('#pillRuta')).toContainText('TFN → SDR');
    const pillText = await page.locator('#pillResultados').textContent();
    expect(extractResultsCount(pillText)).toBeGreaterThan(0);
});

test('Descargar ticket produce un PDF (TFN → SDR)', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url, { waitUntil: 'domcontentloaded' });

    await setRutaTFN_SDR(page);
    await page.getByRole('button', { name: 'Todos' }).click();

    const rows = page.locator('#tablaVuelos tbody tr');
    await expect
        .poll(() => rows.count(), { timeout: 20000, intervals: [200, 400, 800] })
        .toBeGreaterThan(0);

    const downloadPromise = page.waitForEvent('download');
    await page.locator('.btn-descargar').first().click();
    const download = await downloadPromise;

    const suggested = await download.suggestedFilename();
    expect(suggested).toMatch(/ticket-.*\.pdf$/i);
    await download.saveAs(`./downloads/${suggested}`);
});
