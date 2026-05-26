/**
 * 标准 QR 码生成（qrcode 库），确保手机相机 / jsQR / BarcodeDetector 均可识别。
 * 原先自研编码器与标准不完全兼容，会导致「拍照扫码」识别失败。
 */
import QRCode from 'qrcode'

/**
 * @param {string} text 签到 token 等文本
 * @returns {Promise<string>} SVG 字符串，可直接 v-html 渲染
 */
export async function createQrSvg(text) {
  return QRCode.toString(String(text), {
    type: 'svg',
    margin: 2,
    errorCorrectionLevel: 'M',
    color: { dark: '#000000', light: '#ffffff' }
  })
}
