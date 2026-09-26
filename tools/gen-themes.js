// Generates Material 3 color schemes for OpenVisum themes.
// Usage: npm i @material/material-color-utilities && node gen-themes.js > GeneratedSchemes.kt
const { Hct, SchemeContent, MaterialDynamicColors } = require('@material/material-color-utilities');

const themes = [
  ['brand', '0xFF4F6BED'],
  ['teal', '0xFF00838F'],
  ['green', '0xFF2E7D32'],
  ['amber', '0xFFB26A00'],
  ['rose', '0xFFC2185B'],
  ['violet', '0xFF6A3DE8'],
  ['crimson', '0xFFC62828'],
  ['slate', '0xFF455A64'],
];

const roles = [
  'primary', 'onPrimary', 'primaryContainer', 'onPrimaryContainer',
  'secondary', 'onSecondary', 'secondaryContainer', 'onSecondaryContainer',
  'tertiary', 'onTertiary', 'tertiaryContainer', 'onTertiaryContainer',
  'error', 'onError', 'errorContainer', 'onErrorContainer',
  'background', 'onBackground', 'surface', 'onSurface',
  'surfaceVariant', 'onSurfaceVariant',
  'surfaceContainerLowest', 'surfaceContainerLow', 'surfaceContainer',
  'surfaceContainerHigh', 'surfaceContainerHighest',
  'surfaceBright', 'surfaceDim',
  'outline', 'outlineVariant',
  'inverseSurface', 'inverseOnSurface', 'inversePrimary', 'scrim',
];

function argbOf(name, scheme) {
  const dc = MaterialDynamicColors[name];
  const resolved = typeof dc === 'function' ? dc() : dc;
  return resolved.getArgb(scheme) >>> 0;
}

let out = '';
out += '// Generated from https://github.com/material-foundation/material-color-utilities\n';
out += '// Do not edit manually. Regenerate with tools/gen-themes.js.\n';
out += 'package verlintas.openvisum.ui.theme\n\n';
out += 'import androidx.compose.material3.ColorScheme\n';
out += 'import androidx.compose.material3.darkColorScheme\n';
out += 'import androidx.compose.material3.lightColorScheme\n';
out += 'import androidx.compose.ui.graphics.Color\n\n';

for (const [id, seedHex] of themes) {
  const seed = parseInt(seedHex, 16) >>> 0;
  const hct = Hct.fromInt(seed);
  for (const [suffix, isDark, builder] of [['Light', false, 'lightColorScheme'], ['Dark', true, 'darkColorScheme']]) {
    const scheme = new SchemeContent(hct, isDark, 0.0);
    out += `internal fun ${id}${suffix}Scheme(): ColorScheme = ${builder}(\n`;
    for (const role of roles) {
      const argb = argbOf(role, scheme);
      out += `    ${role} = Color(0x${argb.toString(16).toUpperCase().padStart(8, '0')}),\n`;
    }
    out += ')\n\n';
  }
}

console.log(out);
