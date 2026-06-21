package com.erp.pizzeria.service;

import com.erp.pizzeria.dto.AnuladoReporteDTO;
import com.erp.pizzeria.dto.ProveedorReporteDTO;
import com.erp.pizzeria.dto.ReporteDataDTO;
import com.erp.pizzeria.dto.StatDTO;
import com.erp.pizzeria.dto.TipoMovReporteDTO;
import com.erp.pizzeria.dto.TopProductoDTO;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ReporteExportService {

    private static final Color BRAND = new Color(0xC1, 0x39, 0x2B); // tomate
    private static final Color HEADER_BG = new Color(0xF3, 0xE6, 0xE3);

    private static String money(BigDecimal value) {
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        return "S/ " + v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    // ===================== PDF =====================

    public byte[] buildPdf(ReporteDataDTO data) {
        Document doc = new Document(PageSize.A4, 36, 36, 48, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.DARK_GRAY);

        Paragraph title = new Paragraph("Mamma Tomato - Reporte general", titleFont);
        doc.add(title);
        Paragraph sub = new Paragraph("Generado el " + data.getGeneradoEn(), subFont);
        sub.setSpacingAfter(14);
        doc.add(sub);

        addPdfSection(doc, "Estadisticas de ventas");
        PdfPTable t1 = pdfTable(new float[]{3, 2}, new String[]{"Indicador", "Valor"});
        for (StatDTO s : data.getSalesStats()) {
            pdfCell(t1, s.getLabel(), false);
            pdfCell(t1, s.getValue(), false);
        }
        doc.add(t1);

        addPdfSection(doc, "Productos mas vendidos");
        PdfPTable t2 = pdfTable(new float[]{5, 2}, new String[]{"Producto", "Cantidad"});
        for (TopProductoDTO p : data.getTopProductos()) {
            pdfCell(t2, p.getNombre(), false);
            pdfCell(t2, String.valueOf(p.getCantidad()), false);
        }
        doc.add(t2);

        addPdfSection(doc, "Inventario por tipo");
        PdfPTable t3 = pdfTable(new float[]{4, 2, 2}, new String[]{"Tipo", "Operacion", "Registros"});
        for (TipoMovReporteDTO m : data.getMovimientosPorTipo()) {
            pdfCell(t3, m.getTipo(), false);
            pdfCell(t3, m.getOperacion(), false);
            pdfCell(t3, String.valueOf(m.getRegistros()), false);
        }
        doc.add(t3);

        addPdfSection(doc, "Compras por proveedor");
        PdfPTable t4 = pdfTable(new float[]{4, 2, 2, 3}, new String[]{"Proveedor", "Compras", "Total", "Ultima compra"});
        for (ProveedorReporteDTO c : data.getComprasPorProveedor()) {
            pdfCell(t4, c.getProveedor(), false);
            pdfCell(t4, String.valueOf(c.getCompras()), false);
            pdfCell(t4, money(c.getTotal()), false);
            pdfCell(t4, c.getUltimaCompra(), false);
        }
        doc.add(t4);

        addPdfSection(doc, "Pedidos anulados");
        PdfPTable t5 = pdfTable(new float[]{1, 3, 3, 3, 2, 4},
                new String[]{"N", "Fecha", "Cliente", "Cajero", "Total", "Motivo"});
        for (AnuladoReporteDTO a : data.getAnulados()) {
            pdfCell(t5, String.valueOf(a.getNumero()), false);
            pdfCell(t5, a.getFecha(), false);
            pdfCell(t5, a.getCliente(), false);
            pdfCell(t5, a.getCajero(), false);
            pdfCell(t5, money(a.getTotal()), false);
            pdfCell(t5, a.getMotivo(), false);
        }
        doc.add(t5);

        doc.close();
        return out.toByteArray();
    }

    private void addPdfSection(Document doc, String heading) {
        Font h = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BRAND);
        Paragraph p = new Paragraph(heading, h);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);
    }

    private PdfPTable pdfTable(float[] widths, String[] headers) {
        PdfPTable table = new PdfPTable(widths.length);
        table.setWidthPercentage(100);
        try {
            table.setWidths(widths);
        } catch (Exception ignored) {
            // anchos relativos invalidos: se ignora y POI usa reparto uniforme
        }
        Font hf = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
        for (String head : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(head, hf));
            cell.setBackgroundColor(HEADER_BG);
            cell.setPadding(5);
            cell.setBorderColor(Color.LIGHT_GRAY);
            table.addCell(cell);
        }
        return table;
    }

    private void pdfCell(PdfPTable table, String text, boolean bold) {
        Font f = FontFactory.getFont(bold ? FontFactory.HELVETICA_BOLD : FontFactory.HELVETICA, 9, Color.DARK_GRAY);
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", f));
        cell.setPadding(5);
        cell.setBorderColor(Color.LIGHT_GRAY);
        table.addCell(cell);
    }

    // ===================== Excel =====================

    public byte[] buildExcel(ReporteDataDTO data) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle header = headerStyle(wb);

            Sheet s1 = wb.createSheet("Ventas");
            writeHeader(s1, header, "Indicador", "Valor");
            int r1 = 1;
            for (StatDTO s : data.getSalesStats()) {
                Row row = s1.createRow(r1++);
                row.createCell(0).setCellValue(s.getLabel());
                row.createCell(1).setCellValue(s.getValue());
            }
            autosize(s1, 2);

            Sheet s2 = wb.createSheet("Top productos");
            writeHeader(s2, header, "Producto", "Cantidad");
            int r2 = 1;
            for (TopProductoDTO p : data.getTopProductos()) {
                Row row = s2.createRow(r2++);
                row.createCell(0).setCellValue(p.getNombre());
                row.createCell(1).setCellValue(p.getCantidad());
            }
            autosize(s2, 2);

            Sheet s3 = wb.createSheet("Inventario por tipo");
            writeHeader(s3, header, "Tipo", "Operacion", "Registros");
            int r3 = 1;
            for (TipoMovReporteDTO m : data.getMovimientosPorTipo()) {
                Row row = s3.createRow(r3++);
                row.createCell(0).setCellValue(m.getTipo());
                row.createCell(1).setCellValue(m.getOperacion());
                row.createCell(2).setCellValue(m.getRegistros());
            }
            autosize(s3, 3);

            Sheet s4 = wb.createSheet("Compras por proveedor");
            writeHeader(s4, header, "Proveedor", "Compras", "Total", "Ultima compra");
            int r4 = 1;
            for (ProveedorReporteDTO c : data.getComprasPorProveedor()) {
                Row row = s4.createRow(r4++);
                row.createCell(0).setCellValue(c.getProveedor());
                row.createCell(1).setCellValue(c.getCompras());
                row.createCell(2).setCellValue(c.getTotal() != null ? c.getTotal().doubleValue() : 0d);
                row.createCell(3).setCellValue(c.getUltimaCompra());
            }
            autosize(s4, 4);

            Sheet s5 = wb.createSheet("Pedidos anulados");
            writeHeader(s5, header, "N", "Fecha", "Cliente", "Cajero", "Total", "Motivo");
            int r5 = 1;
            for (AnuladoReporteDTO a : data.getAnulados()) {
                Row row = s5.createRow(r5++);
                row.createCell(0).setCellValue(a.getNumero());
                row.createCell(1).setCellValue(a.getFecha());
                row.createCell(2).setCellValue(a.getCliente());
                row.createCell(3).setCellValue(a.getCajero());
                row.createCell(4).setCellValue(a.getTotal() != null ? a.getTotal().doubleValue() : 0d);
                row.createCell(5).setCellValue(a.getMotivo());
            }
            autosize(s5, 6);

            wb.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el Excel de reportes", e);
        }
    }

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        style.setFont(f);
        return style;
    }

    private void writeHeader(Sheet sheet, CellStyle style, String... titles) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(style);
        }
    }

    private void autosize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
