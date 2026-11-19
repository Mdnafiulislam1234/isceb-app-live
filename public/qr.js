// Lightweight, self-contained ISO/IEC 18004 Byte-mode QR Code SVG renderer (Versions 1..10, ECC-L/M)
// Zero external network dependency so QR codes always render offline and inside any hosting page.

const GF_EXP = new Uint8Array(512);
const GF_LOG = new Uint8Array(256);
(() => {
  let x = 1;
  for (let i = 0; i < 255; i++) {
    GF_EXP[i] = x;
    GF_LOG[x] = i;
    x <<= 1;
    if (x & 0x100) x ^= 0x11d;
  }
  for (let i = 255; i < 512; i++) GF_EXP[i] = GF_EXP[i - 255];
})();

function gfMul(a, b) {
  if (a === 0 || b === 0) return 0;
  return GF_EXP[GF_LOG[a] + GF_LOG[b]];
}

function rsGeneratorPoly(degree) {
  let poly = [1];
  for (let i = 0; i < degree; i++) {
    const next = new Array(poly.length + 1).fill(0);
    const factor = GF_EXP[i];
    for (let j = 0; j < poly.length; j++) {
      next[j] ^= poly[j];
      next[j + 1] ^= gfMul(poly[j], factor);
    }
    poly = next;
  }
  return poly;
}

function rsRemainder(data, ecCount) {
  const gen = rsGeneratorPoly(ecCount);
  const res = new Uint8Array(ecCount);
  for (let i = 0; i < data.length; i++) {
    const factor = data[i] ^ res[0];
    res.copyWithin(0, 1);
    res[ecCount - 1] = 0;
    if (factor !== 0) {
      for (let j = 0; j < ecCount; j++) {
        res[j] ^= gfMul(gen[j + 1], factor);
      }
    }
  }
  return res;
}

// Versions 1..10 ECC Level M single-block / multi-block spec:
// [version, totalDataCodewords, ecPerBlock, blocksGroup1Count, blocksGroup1Data, blocksGroup2Count, blocksGroup2Data, alignCenters]
const QR_SPECS_M = [
  [1, 16, 10, 1, 16, 0, 0, []],
  [2, 28, 16, 1, 28, 0, 0, [6, 18]],
  [3, 44, 26, 1, 44, 0, 0, [6, 22]],
  [4, 64, 18, 2, 32, 0, 0, [6, 26]],
  [5, 86, 24, 2, 43, 0, 0, [6, 30]],
  [6, 108, 16, 4, 27, 0, 0, [6, 34]],
  [7, 124, 18, 4, 31, 0, 0, [6, 22, 38]],
  [8, 154, 22, 2, 38, 2, 39, [6, 24, 42]],
  [9, 182, 22, 3, 36, 2, 37, [6, 26, 46]],
  [10, 216, 26, 4, 43, 1, 44, [6, 28, 50]]
];

const VERSION_INFO_BCH = {
  7: 0x07c94,
  8: 0x085bc,
  9: 0x09a99,
  10: 0x0a4d3
};

function chooseSpec(byteLen) {
  for (const spec of QR_SPECS_M) {
    const ver = spec[0];
    const headerBits = 4 + (ver >= 10 ? 16 : 8);
    const neededCodewords = Math.ceil((headerBits + byteLen * 8 + 4) / 8);
    if (neededCodewords <= spec[1]) return spec;
  }
  throw new Error("URL exceeds QR version 10 M capacity");
}

function encodeDataCodewords(bytes, spec) {
  const [ver, totalData] = spec;
  const bits = [];
  const pushBits = (val, len) => {
    for (let i = len - 1; i >= 0; i--) bits.push((val >> i) & 1);
  };
  pushBits(0b0100, 4); // Byte mode
  pushBits(bytes.length, ver >= 10 ? 16 : 8);
  for (const b of bytes) pushBits(b, 8);
  const maxBits = totalData * 8;
  pushBits(0, Math.min(4, maxBits - bits.length));
  while (bits.length % 8 !== 0) bits.push(0);
  const pads = [0xec, 0x11];
  let pi = 0;
  while (bits.length < maxBits) {
    pushBits(pads[pi % 2], 8);
    pi++;
  }
  const out = new Uint8Array(totalData);
  for (let i = 0; i < totalData; i++) {
    let val = 0;
    for (let b = 0; b < 8; b++) val = (val << 1) | bits[i * 8 + b];
    out[i] = val;
  }
  return out;
}

function buildFinalBytes(dataCodewords, spec) {
  const [, , ecPerBlock, g1Count, g1Data, g2Count, g2Data] = spec;
  const dataBlocks = [];
  const ecBlocks = [];
  let offset = 0;
  for (let i = 0; i < g1Count; i++) {
    const blk = dataCodewords.slice(offset, offset + g1Data);
    offset += g1Data;
    dataBlocks.push(blk);
    ecBlocks.push(rsRemainder(blk, ecPerBlock));
  }
  for (let i = 0; i < g2Count; i++) {
    const blk = dataCodewords.slice(offset, offset + g2Data);
    offset += g2Data;
    dataBlocks.push(blk);
    ecBlocks.push(rsRemainder(blk, ecPerBlock));
  }
  const result = [];
  const maxD = Math.max(g1Data, g2Data);
  for (let i = 0; i < maxD; i++) {
    for (const blk of dataBlocks) if (i < blk.length) result.push(blk[i]);
  }
  for (let i = 0; i < ecPerBlock; i++) {
    for (const blk of ecBlocks) result.push(blk[i]);
  }
  return result;
}

function formatBitsForMask(maskId) {
  // ECC Level M = 00
  const data = (0b00 << 3) | maskId;
  let rem = data << 10;
  const gen = 0x537;
  for (let i = 14; i >= 10; i--) {
    if ((rem >> i) & 1) rem ^= gen << (i - 10);
  }
  return ((data << 10) | rem) ^ 0x5412;
}

export function generateQrMatrix(text) {
  const bytes = new TextEncoder().encode(String(text));
  const spec = chooseSpec(bytes.length);
  const [ver, , , , , , , alignCenters] = spec;
  const size = ver * 4 + 17;
  const modules = Array.from({ length: size }, () => new Int8Array(size)); // 0 empty, 1 light, 2 dark
  const reserved = Array.from({ length: size }, () => new Uint8Array(size));

  const setFixed = (r, c, dark) => {
    modules[r][c] = dark ? 2 : 1;
    reserved[r][c] = 1;
  };

  const drawFinder = (top, left) => {
    for (let r = -1; r <= 7; r++) {
      for (let c = -1; c <= 7; c++) {
        const rr = top + r;
        const cc = left + c;
        if (rr < 0 || rr >= size || cc < 0 || cc >= size) continue;
        const inOuter = r >= 0 && r <= 6 && c >= 0 && c <= 6;
        const onBorder = r === 0 || r === 6 || c === 0 || c === 6;
        const inCenter = r >= 2 && r <= 4 && c >= 2 && c <= 4;
        setFixed(rr, cc, inOuter && (onBorder || inCenter));
      }
    }
  };

  drawFinder(0, 0);
  drawFinder(0, size - 7);
  drawFinder(size - 7, 0);

  for (let i = 8; i < size - 8; i++) {
    setFixed(6, i, i % 2 === 0);
    setFixed(i, 6, i % 2 === 0);
  }
  setFixed(size - 8, 8, true);

  for (const rCenter of alignCenters) {
    for (const cCenter of alignCenters) {
      if (reserved[rCenter][cCenter]) continue;
      for (let dr = -2; dr <= 2; dr++) {
        for (let dc = -2; dc <= 2; dc++) {
          const dark = Math.max(Math.abs(dr), Math.abs(dc)) !== 1;
          setFixed(rCenter + dr, cCenter + dc, dark);
        }
      }
    }
  }

  for (let i = 0; i < 9; i++) {
    if (!reserved[8][i]) reserved[8][i] = 1;
    if (!reserved[i][8]) reserved[i][8] = 1;
  }
  for (let i = 0; i < 8; i++) {
    if (!reserved[8][size - 1 - i]) reserved[8][size - 1 - i] = 1;
    if (i < 7 && !reserved[size - 1 - i][8]) reserved[size - 1 - i][8] = 1;
  }

  if (ver >= 7) {
    const vBits = VERSION_INFO_BCH[ver];
    for (let i = 0; i < 18; i++) {
      const bit = ((vBits >> i) & 1) === 1;
      const r = Math.floor(i / 3);
      const c = size - 11 + (i % 3);
      setFixed(r, c, bit);
      setFixed(c, r, bit);
    }
  }

  const finalBytes = buildFinalBytes(encodeDataCodewords(bytes, spec), spec);
  const bitStream = [];
  for (const byte of finalBytes) {
    for (let b = 7; b >= 0; b--) bitStream.push((byte >> b) & 1);
  }

  // Mask 0: (r + c) % 2 === 0 (always supported cleanly)
  const maskId = 0;
  let bitIdx = 0;
  let dirUp = true;
  for (let right = size - 1; right >= 1; right -= 2) {
    if (right === 6) right = 5;
    for (let i = 0; i < size; i++) {
      const r = dirUp ? size - 1 - i : i;
      for (let colOffset = 0; colOffset < 2; colOffset++) {
        const c = right - colOffset;
        if (reserved[r][c]) continue;
        const dataBit = bitIdx < bitStream.length ? bitStream[bitIdx++] : 0;
        const maskBit = (r + c) % 2 === 0 ? 1 : 0;
        modules[r][c] = (dataBit ^ maskBit) === 1 ? 2 : 1;
      }
    }
    dirUp = !dirUp;
  }

  const fmt = formatBitsForMask(maskId);
  for (let i = 0; i < 15; i++) {
    const dark = ((fmt >> i) & 1) === 1;
    if (i < 6) modules[i][8] = dark ? 2 : 1;
    else if (i < 8) modules[i + 1][8] = dark ? 2 : 1;
    else modules[size - 15 + i][8] = dark ? 2 : 1;

    if (i < 8) modules[8][size - 1 - i] = dark ? 2 : 1;
    else if (i < 9) modules[8][15 - i] = dark ? 2 : 1;
    else modules[8][14 - i] = dark ? 2 : 1;
  }

  return modules.map((row) => Array.from(row, (v) => v === 2));
}

export function renderQrSvg(text, pxSize = 180) {
  const matrix = generateQrMatrix(text);
  const count = matrix.length;
  const quiet = 4;
  const total = count + quiet * 2;
  const rects = [];
  for (let r = 0; r < count; r++) {
    for (let c = 0; c < count; c++) {
      if (matrix[r][c]) {
        rects.push(`<rect x="${c + quiet}" y="${r + quiet}" width="1" height="1"/>`);
      }
    }
  }
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${total} ${total}" width="${pxSize}" height="${pxSize}" shape-rendering="crispEdges" role="img" aria-label="QR code to open this download page"><rect width="${total}" height="${total}" fill="#ffffff"/><g fill="#071A33">${rects.join("")}</g></svg>`;
}
