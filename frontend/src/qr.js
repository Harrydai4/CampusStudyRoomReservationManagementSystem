const VERSION = 5
const SIZE = 21 + VERSION * 4
const DATA_CODEWORDS = 108
const ECC_CODEWORDS = 26
const FORMAT_BITS_L_MASK_0 = 0x77c4

export function createQrSvg(text, scale = 5, border = 4) {
  const bytes = new TextEncoder().encode(text)
  if (bytes.length > 106) {
    throw new Error('二维码内容过长')
  }

  const modules = Array.from({ length: SIZE }, () => Array(SIZE).fill(false))
  const reserved = Array.from({ length: SIZE }, () => Array(SIZE).fill(false))

  const set = (x, y, dark, isReserved = true) => {
    if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) return
    modules[y][x] = !!dark
    if (isReserved) reserved[y][x] = true
  }

  drawFinder(set, 0, 0)
  drawFinder(set, SIZE - 7, 0)
  drawFinder(set, 0, SIZE - 7)
  drawAlignment(set, 30, 30)
  drawTiming(set)
  reserveFormat(set)
  set(8, VERSION * 4 + 9, true)

  const data = makeDataCodewords(bytes)
  const ecc = reedSolomonRemainder(data, reedSolomonDivisor(ECC_CODEWORDS))
  drawCodewords(modules, reserved, [...data, ...ecc])
  drawFormat(set)

  const pixels = SIZE + border * 2
  const cells = []
  for (let y = 0; y < SIZE; y += 1) {
    for (let x = 0; x < SIZE; x += 1) {
      if (modules[y][x]) {
        cells.push(`<rect x="${x + border}" y="${y + border}" width="1" height="1"/>`)
      }
    }
  }

  return `<svg class="qr-svg" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${pixels} ${pixels}" width="${pixels * scale}" height="${pixels * scale}" role="img" aria-label="签到二维码"><rect width="100%" height="100%" fill="#fff"/>${cells.join('')}</svg>`
}

function drawFinder(set, x, y) {
  for (let dy = -1; dy <= 7; dy += 1) {
    for (let dx = -1; dx <= 7; dx += 1) {
      const xx = x + dx
      const yy = y + dy
      const inPattern = dx >= 0 && dx <= 6 && dy >= 0 && dy <= 6
      const dark = inPattern && (dx === 0 || dx === 6 || dy === 0 || dy === 6 || (dx >= 2 && dx <= 4 && dy >= 2 && dy <= 4))
      set(xx, yy, dark)
    }
  }
}

function drawAlignment(set, cx, cy) {
  for (let dy = -2; dy <= 2; dy += 1) {
    for (let dx = -2; dx <= 2; dx += 1) {
      const distance = Math.max(Math.abs(dx), Math.abs(dy))
      set(cx + dx, cy + dy, distance === 2 || distance === 0)
    }
  }
}

function drawTiming(set) {
  for (let i = 8; i < SIZE - 8; i += 1) {
    set(6, i, i % 2 === 0)
    set(i, 6, i % 2 === 0)
  }
}

function reserveFormat(set) {
  for (let i = 0; i <= 8; i += 1) {
    if (i !== 6) {
      set(8, i, false)
      set(i, 8, false)
    }
  }
  for (let i = 0; i < 8; i += 1) {
    set(SIZE - 1 - i, 8, false)
  }
  for (let i = 8; i < 15; i += 1) {
    set(8, SIZE - 15 + i, false)
  }
}

function drawFormat(set) {
  for (let i = 0; i <= 5; i += 1) set(8, i, getBit(FORMAT_BITS_L_MASK_0, i))
  set(8, 7, getBit(FORMAT_BITS_L_MASK_0, 6))
  set(8, 8, getBit(FORMAT_BITS_L_MASK_0, 7))
  set(7, 8, getBit(FORMAT_BITS_L_MASK_0, 8))
  for (let i = 9; i < 15; i += 1) set(14 - i, 8, getBit(FORMAT_BITS_L_MASK_0, i))
  for (let i = 0; i < 8; i += 1) set(SIZE - 1 - i, 8, getBit(FORMAT_BITS_L_MASK_0, i))
  for (let i = 8; i < 15; i += 1) set(8, SIZE - 15 + i, getBit(FORMAT_BITS_L_MASK_0, i))
  set(8, SIZE - 8, true)
}

function makeDataCodewords(bytes) {
  const bits = []
  appendBits(bits, 0x4, 4)
  appendBits(bits, bytes.length, 8)
  bytes.forEach(byte => appendBits(bits, byte, 8))
  appendBits(bits, 0, Math.min(4, DATA_CODEWORDS * 8 - bits.length))
  while (bits.length % 8 !== 0) bits.push(false)

  const data = []
  for (let i = 0; i < bits.length; i += 8) {
    let value = 0
    for (let j = 0; j < 8; j += 1) value = (value << 1) | (bits[i + j] ? 1 : 0)
    data.push(value)
  }
  for (let pad = 0xec; data.length < DATA_CODEWORDS; pad ^= 0xfd) data.push(pad)
  return data
}

function drawCodewords(modules, reserved, codewords) {
  const bits = []
  codewords.forEach(byte => appendBits(bits, byte, 8))

  let bitIndex = 0
  for (let right = SIZE - 1; right >= 1; right -= 2) {
    if (right === 6) right = 5
    for (let vert = 0; vert < SIZE; vert += 1) {
      for (let j = 0; j < 2; j += 1) {
        const x = right - j
        const upward = ((right + 1) & 2) === 0
        const y = upward ? SIZE - 1 - vert : vert
        if (reserved[y][x]) continue
        const raw = bitIndex < bits.length ? bits[bitIndex] : false
        modules[y][x] = (x + y) % 2 === 0 ? !raw : raw
        bitIndex += 1
      }
    }
  }
}

function appendBits(bits, value, length) {
  for (let i = length - 1; i >= 0; i -= 1) bits.push(((value >>> i) & 1) !== 0)
}

function getBit(value, index) {
  return ((value >>> index) & 1) !== 0
}

function reedSolomonDivisor(degree) {
  const result = Array(degree).fill(0)
  result[degree - 1] = 1
  let root = 1
  for (let i = 0; i < degree; i += 1) {
    for (let j = 0; j < degree; j += 1) {
      result[j] = gfMultiply(result[j], root)
      if (j + 1 < degree) result[j] ^= result[j + 1]
    }
    root = gfMultiply(root, 0x02)
  }
  return result
}

function reedSolomonRemainder(data, divisor) {
  const result = Array(divisor.length).fill(0)
  data.forEach(byte => {
    const factor = byte ^ result.shift()
    result.push(0)
    divisor.forEach((coef, index) => {
      result[index] ^= gfMultiply(coef, factor)
    })
  })
  return result
}

function gfMultiply(x, y) {
  let z = 0
  for (let i = 7; i >= 0; i -= 1) {
    z = (z << 1) ^ ((z >>> 7) * 0x11d)
    z ^= ((y >>> i) & 1) * x
  }
  return z
}
