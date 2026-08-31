# hotswap

A from-scratch, free, on-device hot-swap tool for iterating on `convey`'s kinetic typography
composables on a real Android device -- the counterpart to `:dev-app:hotRunJvm`'s desktop Compose
Hot Reload, which is explicitly desktop-only upstream (JetBrains: "While we explore adding support
for other targets, you can already use the desktop app as your sandbox").

## Why build this instead of using a paid tool or Android Studio's Apply Changes

Android Studio's own **Apply Changes** already does the equivalent of this, for free, from the
IDE -- it drives ART's JDWP `RedefineClasses` command by hand when you click the button. This
tool exists because that's not scriptable into a save-triggered dev loop the way desktop Hot
Reload is; it's this repo's own implementation of the same underlying mechanism, so it can be
driven automatically. HotSwan (`hotswan.dev`) does something similar but is paid/proprietary.

## How it works

1. You edit a composable and your build recompiles the changed class (via Android Studio, or
   `./gradlew :android-dev-app:compileDebugKotlin` -- this tool doesn't watch files itself, see
   below).
2. `ClassRedefiner` runs the Android SDK's `d8` over the single changed `.class` file, producing a
   small DEX container -- ART's JDWP `RedefineClasses` implementation expects Dalvik-executable
   bytecode, not plain JVM classfile bytes, since ART runs DEX rather than a JVM class loader.
3. `JdwpClient` connects to the already-running debug process over `adb forward tcp:<port>
   jdwp:<pid>`, looks up the class's `referenceTypeID` via the standard `VirtualMachine.
   ClassesBySignature` JDWP command, and sends the standard `VirtualMachine.RedefineClasses`
   command with the new DEX bytes.
4. `main()` then does `adb shell am broadcast -a compose.conveyance.devapp.RELOAD`, which
   `android-dev-app`'s `MainActivity` is listening for; it bumps a `key(generation)` wrapper
   around the whole composable tree, forcing a full re-invocation -- redefined method bodies only
   take effect for *new* stack frames, so anything already composed with the old code needs a
   fresh call to actually run the new one.

Usage:

```
ANDROID_HOME=/path/to/sdk ./gradlew :hotswap:run --args="compose.conveyance.devapp compose.conveyance.devapp.MainActivity android-dev-app/build/tmp/kotlin-classes/debug/compose/conveyance/devapp/MainActivity.class"
```

This redefines **one class per invocation** and does not watch files itself -- pipe changed
`.class` paths into it from a file watcher of your choice, e.g. with `entr`:

```
find android-dev-app/src convey/src -name '*.kt' | entr -s './gradlew :android-dev-app:compileDebugKotlin && ./gradlew :hotswap:run --args="..."'
```

## What's actually verified here, and what isn't

The JDWP wire protocol (handshake, `IDSizes`, `ClassesBySignature`, `RedefineClasses` packet
encoding/decoding) is unit-tested in `JdwpClientTest` against a scripted fake server -- that part
is genuinely verified: this tool speaks the documented protocol correctly.

**What is not verified, because this sandbox has no Android device or emulator:** whether ART
actually accepts a `RedefineClasses` request built this way, whether the `d8`-produced single-class
DEX is in a form ART's redefinition path will take, and whether the whole pipeline (adb forward,
process attach, broadcast, recomposition) works end-to-end on a real device. This needs to be
tried on real hardware before trusting it -- expect to debug it there. In particular:

- The exact byte-level expectations of ART's `RedefineClasses` handler for the "classfile" payload
  aren't publicly speced the way the rest of JDWP is; Android Studio's Apply Changes implementation
  (closed source) is the only known-working reference, and this is a best-effort, unverified
  reimplementation of what it's understood to do.
- Same restriction as every JVMTI/ART class-redefinition tool (including HotSwan and Apply
  Changes): only method bodies can change. Adding/removing fields or methods, changing a class's
  hierarchy, or adding new classes will fail or be silently ignored -- those still need a real
  reinstall.
- `adb shell pidof <package>` assumes exactly one running process for the package; a
  multi-process app needs `--user-namespace`-aware pid selection this doesn't have.
