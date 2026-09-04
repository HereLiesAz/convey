// Headless Chrome refuses to start its own sandbox inside most CI/container environments
// (no user namespaces, no D-Bus session) -- the standard fix is a custom Karma launcher that
// adds --no-sandbox (and the two flags that go with it in a container: --disable-gpu since
// there's no GPU device, --disable-dev-shm-usage since /dev/shm is often too small in a
// container). This file is picked up automatically by the Kotlin/JS Gradle plugin's
// karma.config.d convention -- every *.js file here is merged into the generated karma.conf.js
// for wasmJsBrowserTest/wasmJsTest, no build.gradle.kts wiring needed.
config.set({
  browsers: config.browsers.includes('ChromeHeadless') ? ['ChromeHeadlessNoSandbox'] : config.browsers,
  customLaunchers: {
    ChromeHeadlessNoSandbox: {
      base: 'ChromeHeadless',
      flags: ['--no-sandbox', '--disable-gpu', '--disable-dev-shm-usage'],
    },
  },
})
