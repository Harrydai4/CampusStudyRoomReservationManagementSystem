/**
 * 标准 QR 码生成：内容为纯学号文本，供管理员「拍照扫码」识别。
 */
import QRCode from 'qrcode'

/**
 * @param {string} text 学号等纯文本
 * @returns {Promise<string>} SVG 字符串
 */
export async function createQrSvg(text) {
  return QRCode.toString(String(text), {
    type: 'svg',
    margin: 2,
    width: 240,
    errorCorrectionLevel: 'M',
    color: { dark: '#000000', light: '#ffffff' }
  })
}
