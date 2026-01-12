
// @ts-check
const { defineConfig } = require('@playwright/test');

module.exports = defineConfig({
    timeout: 60_000,
    use: {
        browserName: 'chromium',
        headless: true,
        screenshot: 'only-on-failure',
        video: 'retain-on-failure'
    },
    reporter: [['list'], ['html', { outputFolder: 'playwright-report' }]],
    testDir: './specs'
});
