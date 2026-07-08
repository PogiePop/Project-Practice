import * as XLSX from 'xlsx'
import { ElMessage } from 'element-plus'

/**
 * 读取并解析 Excel 文件，返回 JSON 数组
 * @param {File} file - 用户选择的文件对象
 * @returns {Promise<{header: string[], data: object[]}>}
 */
export function readExcel(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = (e) => {
      try {
        const wb = XLSX.read(e.target.result, { type: 'binary' })
        const sheetName = wb.SheetNames[0]
        const ws = wb.Sheets[sheetName]
        const json = XLSX.utils.sheet_to_json(ws, { defval: '' })
        if (json.length === 0) {
          reject(new Error('Excel 文件中没有数据'))
          return
        }
        const header = Object.keys(json[0])
        resolve({ header, data: json })
      } catch (err) {
        reject(new Error('文件解析失败，请确认文件格式正确'))
      }
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsBinaryString(file)
  })
}

/**
 * 导出 JSON 数据为 Excel 文件并触发下载
 * @param {object[]} data - 数据数组
 * @param {string} fileName - 文件名（不含后缀）
 * @param {object[]} columns - 列定义 [{prop, label}]
 */
export function exportExcel(data, fileName, columns) {
  if (!data || data.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  // 按 columns 顺序提取数据
  const exportData = data.map(row => {
    const obj = {}
    columns.forEach(col => {
      obj[col.label] = row[col.prop] ?? ''
    })
    return obj
  })

  const ws = XLSX.utils.json_to_sheet(exportData)
  // 设置列宽
  const colWidths = columns.map(col => ({ wch: Math.max(col.label.length * 2, 15) }))
  ws['!cols'] = colWidths

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  XLSX.writeFile(wb, `${fileName}.xlsx`)
  ElMessage.success('导出成功')
}

/**
 * 根据模板定义生成导入模板 Excel 并触发下载
 * @param {string} fileName - 文件名
 * @param {object[]} templateFields - 模板字段 [{label,示例值}]
 */
export function downloadImportTemplate(fileName, templateFields) {
  const templateData = [{}]
  const row = {}
  templateFields.forEach(f => {
    row[f.label] = f.example || ''
  })
  // 加一行示例
  const wsData = [row]
  templateFields.forEach(f => {
    if (f.required) {
      row[f.label] = f.example || `【必填】`
    }
  })

  const ws = XLSX.utils.json_to_sheet(wsData)
  const colWidths = templateFields.map(f => ({ wch: Math.max(f.label.length * 2, 20) }))
  ws['!cols'] = colWidths

  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1')
  XLSX.writeFile(wb, `${fileName}.xlsx`)
  ElMessage.success('模板下载成功')
}
