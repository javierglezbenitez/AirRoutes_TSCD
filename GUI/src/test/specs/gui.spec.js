
const { test, expect } = require('@playwright/test');

const GUI_LOCAL = process.env.GUI_LOCAL ?? 'http://localhost:3000/index.html';
const API_BASE  = process.env.API_BASE  ?? 'http://127.0.0.1:8080';

test('Health muestra estado cuando la API responde', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url);

    const healthMsg = page.locator('#healthMsg');
    await expect(healthMsg).toBeVisible();
    await expect(healthMsg).toHaveText(/API conectada \(OK\)|API respondió:|API no disponible/i, { timeout: 10000 });
});

test('Consulta "Todos" rellena la tabla y muestra KPIs', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url);

    await page.getByPlaceholder('Origen (ej. MUR)').fill('MUR');
    await page.getByPlaceholder('Destino (ej. SDR)').fill('SDR');
    await page.getByRole('button', { name: 'Todos' }).click();

    const searchMsg = page.locator('#searchMsg');
    await expect(searchMsg).toBeVisible();

    const rows = page.locator('#tablaVuelos tbody tr');
    await expect(await rows.count()).toBeGreaterThan(0);

    await expect(page.locator('#kpisVuelos')).toBeVisible();
    await expect(page.locator('#kpiPrecioMedio')).not.toHaveText('—');
});

test('Descargar ticket produce un PDF', async ({ page }) => {
    const url = `${GUI_LOCAL}?api=${encodeURIComponent(API_BASE)}`;
    await page.goto(url);

    await page.getByRole('button', { name: 'Todos' }).click();

    const rows = page.locator('#tablaVuelos tbody tr');
    await expect(await rows.count()).toBeGreaterThan(0);

    const downloadPromise = page.waitForEvent('download');
    await page.locator('.btn-descargar').first().click();
    const download = await downloadPromise;
    const suggested = await download.suggestedFilename();
    expect(suggested).toMatch(/ticket-.*\.pdf$/i);
});
