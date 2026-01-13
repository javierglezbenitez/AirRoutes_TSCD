
// GUI/src/test/playwright.config.js
// @ts-check
const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
    timeout: 90_000,
    expect: { timeout: 20_000 },
    use: {
        browserName: 'chromium',
        headless: true,
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
        trace: 'retain-on-failure'
    },
    reporter: [['list'], ['html', { outputFolder: 'playwright-report' }]],
    testDir: './specs'
});
