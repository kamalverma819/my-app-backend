package com.example.New.Lotus.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.apache.poi.ss.util.CellRangeAddress;
import java.time.format.DateTimeFormatter;
import com.example.New.Lotus.Model.Item;
import com.example.New.Lotus.Model.PurchaseItem;
import com.example.New.Lotus.Repository.ItemRepository;
import com.example.New.Lotus.Repository.PurchaseRepository;
import com.example.New.Lotus.entity.PurchaseInvoice;
import java.lang.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    @Autowired
    private PurchaseRepository purchaseRepo;

    @Autowired
    private ItemRepository itemRepo;

    @PostMapping
    public ResponseEntity<?> createPurchase(@RequestBody PurchaseInvoice invoice) {
        List<PurchaseItem> updatedItems = new ArrayList<>();

        String vendorStateCode = invoice.getGstin() != null && invoice.getGstin().length() >= 2
            ? invoice.getGstin().substring(0, 2)
            : "";
        String companyStateCode = "23"; // Madhya Pradesh

        double subtotal = 0;
        double cgst = 0, sgst = 0, igst = 0;

        for (PurchaseItem pItem : invoice.getItems()) {
            Optional<Item> existingOpt = itemRepo.findByNameAndHsnCode(pItem.getName(), pItem.getHsnCode());

            Item item;
            if (existingOpt.isPresent()) {
                item = existingOpt.get();
                item.setStock(item.getStock() + pItem.getQuantity());
                item.setBuyingPrice(pItem.getPrice());
            } else {
                item = new Item();
                item.setName(pItem.getName());
                item.setHsnCode(pItem.getHsnCode());
                item.setGstRate(pItem.getGstRate());
                item.setStock(pItem.getQuantity());
                item.setBuyingPrice(pItem.getPrice());
                item.setSellingPrice(0);
            }

            Item savedItem = itemRepo.save(item);
            pItem.setItemId(savedItem.getId());

            // 💰 Calculate total and GST
            double itemTotal = pItem.getPrice() * pItem.getQuantity();
            double gstAmount = itemTotal * pItem.getGstRate() / 100;
            pItem.setTotal(itemTotal);
            pItem.setGstAmount(gstAmount);

            subtotal += itemTotal;

            if (vendorStateCode.equals(companyStateCode)) {
                cgst += gstAmount / 2;
                sgst += gstAmount / 2;
            } else {
                igst += gstAmount;
            }

            updatedItems.add(pItem);
        }

        invoice.setItems(updatedItems);
        invoice.setSubtotal(subtotal);
        invoice.setCgst(cgst);
        invoice.setSgst(sgst);
        invoice.setIgst(igst);
        invoice.setTotal(subtotal + cgst + sgst + igst + invoice.getFreight());

        PurchaseInvoice savedInvoice = purchaseRepo.save(invoice);
        return ResponseEntity.ok(savedInvoice);
    }



    @GetMapping
    public ResponseEntity<List<PurchaseInvoice>> getAllPurchases() {
        return ResponseEntity.ok(purchaseRepo.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseInvoice> getPurchaseById(@PathVariable String id) {
        return purchaseRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
  


    @GetMapping("/download-report")
    public void downloadPurchaseReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response
    ) throws IOException {

        List<PurchaseInvoice> purchases;
        double totalTaxableAmount = 0.0;
        double totalCgstAmt = 0.0;
        double totalSgstAmt = 0.0;
        double totalIgstAmt = 0.0;
        double totalGstAmt = 0.0;
        double totalInvoiceAmount = 0.0;
        double totalInvoiceTotal = 0.0;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        if (from != null && to != null) {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to).plusDays(1); // inclusive
            purchases = purchaseRepo.findInvoicesBetweenDates(fromDate, toDate);
        } else {
            purchases = purchaseRepo.findAll();
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Purchase Report");

        String[] headers = {
            "S.N.", "Bill No.", "Bill Date", "Supplier Name", "GST No.",
            "HSN/SAC", "UNIT RATE", "QTY.", "Taxable Amount",
            "CGST %", "CGST Amt", "SGST %", "SGST Amt", "IGST %", "IGST Amt",
            "TOTAL GST", "Invoice Amount", "Invoice Total"
        };

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }

        int rowIdx = 1;
        int serialNo = 1;

        for (PurchaseInvoice invoice : purchases) {
            String billNo = invoice.getInvoiceNo();
            String billDate = invoice.getInvoiceDate().format(formatter);
            String supplier = invoice.getVendorName();
            String gstNo = invoice.getGstin();
            double freight = invoice.getFreight();

            String vendorStateCode = gstNo != null && gstNo.length() >= 2 ? gstNo.substring(0, 2) : "";
            String companyStateCode = "23"; // MP

            double invoiceTotal = 0.0;
            double invoiceTaxableAmount = 0.0;

            int invoiceStartRow = rowIdx;

            for (PurchaseItem item : invoice.getItems()) {
                double taxable = item.getPrice() * item.getQuantity();
                double gstRate = item.getGstRate();
                double gstAmt = taxable * gstRate / 100;

                double cgst = 0, sgst = 0, igst = 0;
                if (vendorStateCode.equals(companyStateCode)) {
                    cgst = gstAmt / 2;
                    sgst = gstAmt / 2;
                } else {
                    igst = gstAmt;
                }

                double totalGst = cgst + sgst + igst;
                double totalAmount = taxable + totalGst;
                totalGstAmt+=totalGst;
                invoiceTaxableAmount += taxable;
                invoiceTotal += totalAmount;

                Row row = sheet.createRow(rowIdx++);
                int col = 0;
                row.createCell(col++).setCellValue(serialNo++);
                row.createCell(col++).setCellValue(billNo);
                row.createCell(col++).setCellValue(billDate);
                row.createCell(col++).setCellValue(supplier);
                row.createCell(col++).setCellValue(gstNo);
                row.createCell(col++).setCellValue(item.getHsnCode());
                row.createCell(col++).setCellValue(item.getPrice());
                row.createCell(col++).setCellValue(item.getQuantity());
                row.createCell(col++).setCellValue(taxable);
                row.createCell(col++).setCellValue(gstRate / 2);
                row.createCell(col++).setCellValue(cgst);
                row.createCell(col++).setCellValue(gstRate / 2);
                row.createCell(col++).setCellValue(sgst);
                row.createCell(col++).setCellValue(gstRate);
                row.createCell(col++).setCellValue(igst);
                row.createCell(col++).setCellValue(totalGst);
                row.createCell(col++).setCellValue(totalAmount);
                // Invoice Total will be merged after freight
            }

            // Add freight as a separate row if applicable
            if (freight > 0) {
                Row freightRow = sheet.createRow(rowIdx++);
                double gstRate = 18;
                double gstAmt = freight * gstRate / 100;
                double cgst = 0, sgst = 0, igst = 0;

                if (vendorStateCode.equals(companyStateCode)) {
                    cgst = gstAmt / 2;
                    sgst = gstAmt / 2;
                } else {
                    igst = gstAmt;
                }

                double totalGst = cgst + sgst + igst;
                double total = freight + totalGst;

                invoiceTaxableAmount += freight;
                invoiceTotal += total;
                totalCgstAmt +=cgst;
                totalSgstAmt+=sgst;
                totalIgstAmt+=igst;
                totalGstAmt+=totalGst;
                
                int col = 0;
                freightRow.createCell(col++).setCellValue(serialNo++);
                freightRow.createCell(col++).setCellValue(billNo);
                freightRow.createCell(col++).setCellValue(billDate);
                freightRow.createCell(col++).setCellValue(supplier);
                freightRow.createCell(col++).setCellValue(gstNo);
                freightRow.createCell(col++).setCellValue("FREIGHT");
                freightRow.createCell(col++).setCellValue(freight);
                freightRow.createCell(col++).setCellValue(1);
                freightRow.createCell(col++).setCellValue(freight);
                freightRow.createCell(col++).setCellValue(gstRate / 2);
                freightRow.createCell(col++).setCellValue(cgst);
                freightRow.createCell(col++).setCellValue(gstRate / 2);
                freightRow.createCell(col++).setCellValue(sgst);
                freightRow.createCell(col++).setCellValue(gstRate);
                freightRow.createCell(col++).setCellValue(igst);
                freightRow.createCell(col++).setCellValue(totalGst);
                freightRow.createCell(col++).setCellValue(total);
                // Invoice Total will be merged next
            }

            // Merge Invoice Total column (R / index 17)
            int invoiceEndRow = rowIdx - 1;
            if (invoiceStartRow <= invoiceEndRow) {
                sheet.addMergedRegion(new CellRangeAddress(invoiceStartRow, invoiceEndRow, 17, 17));
                Row mergedRow = sheet.getRow(invoiceStartRow);
                if (mergedRow == null) mergedRow = sheet.createRow(invoiceStartRow);
                Cell totalCell = mergedRow.createCell(17);
                totalCell.setCellValue(invoiceTotal);
                totalInvoiceTotal +=invoiceTotal ;
            }

            totalCgstAmt += invoice.getItems().stream().mapToDouble(i -> {
                double gstAmt = i.getPrice() * i.getQuantity() * i.getGstRate() / 100.0;
                return vendorStateCode.equals(companyStateCode) ? gstAmt / 2 : 0.0;
            }).sum();

            totalSgstAmt += invoice.getItems().stream().mapToDouble(i -> {
                double gstAmt = i.getPrice() * i.getQuantity() * i.getGstRate() / 100.0;
                return vendorStateCode.equals(companyStateCode) ? gstAmt / 2 : 0.0;
            }).sum();

            totalIgstAmt += invoice.getItems().stream().mapToDouble(i -> {
                double gstAmt = i.getPrice() * i.getQuantity() * i.getGstRate() / 100.0;
                return !vendorStateCode.equals(companyStateCode) ? gstAmt : 0.0;
            }).sum();

            totalTaxableAmount +=invoiceTaxableAmount;
            totalInvoiceAmount += invoiceTotal;
            

            sheet.createRow(rowIdx++); // blank row
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Grand Total Row
        Row totalRow = sheet.createRow(rowIdx++);
        int col = 0;
        CellStyle boldStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        boldStyle.setFont(font);

        Cell totalLabel = totalRow.createCell(col);
        totalLabel.setCellValue("Grand Total");
        totalLabel.setCellStyle(boldStyle);
        
        col = 8; // CGST Amt
        Cell taxableAmountCell = totalRow.createCell(col++);
        taxableAmountCell.setCellValue(totalTaxableAmount);
        taxableAmountCell.setCellStyle(boldStyle);

        col++; // skip CGST %
        Cell cgstCell = totalRow.createCell(col++);
        cgstCell.setCellValue(totalCgstAmt);
        cgstCell.setCellStyle(boldStyle);

        col++; // skip SGST %
        Cell sgstCell = totalRow.createCell(col++);
        sgstCell.setCellValue(totalSgstAmt);
        sgstCell.setCellStyle(boldStyle);

        col++; // skip IGST %
        Cell igstCell = totalRow.createCell(col++);
        igstCell.setCellValue(totalIgstAmt);
        igstCell.setCellStyle(boldStyle);

        Cell totalGstCell = totalRow.createCell(col++);
        totalGstCell.setCellValue(totalGstAmt);
        totalGstCell.setCellStyle(boldStyle);

        Cell invoiceAmtCell = totalRow.createCell(col++);
        invoiceAmtCell.setCellValue(totalInvoiceAmount);
        invoiceAmtCell.setCellStyle(boldStyle);

        Cell invoiceTotalCell = totalRow.createCell(col++);
        invoiceTotalCell.setCellValue(totalInvoiceTotal);
        invoiceTotalCell.setCellStyle(boldStyle);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Purchase_Report.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
        
    }


