package com.example.New.Lotus.Controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.io.*;
import java.time.format.DateTimeFormatter;
import com.example.New.Lotus.Model.Item;
import com.example.New.Lotus.Model.PurchaseItem;
import com.example.New.Lotus.Model.SalesItem;
import com.example.New.Lotus.Repository.ItemRepository;
import com.example.New.Lotus.Repository.SalesRepository;
import com.example.New.Lotus.entity.PurchaseInvoice;
import com.example.New.Lotus.entity.SalesInvoice;

import jakarta.servlet.http.HttpServletResponse;


@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SalesRepository salesRepo;
    
    @Autowired
    private ItemRepository itemRepo;

    @PostMapping
    public ResponseEntity<?> createSale(@RequestBody SalesInvoice invoice) {
        for (SalesItem saleItem : invoice.getItems()) {
            String itemId = saleItem.getItemId();
            int quantitySold = saleItem.getQuantity();

            Optional<Item> optionalItem = itemRepo.findById(itemId);
            if (optionalItem.isEmpty()) {
                return ResponseEntity.badRequest().body("Item not found with ID: " + itemId);
            }

            Item item = optionalItem.get();
            if (quantitySold > item.getStock()) {
                return ResponseEntity.badRequest().body("Not enough stock for item: " + item.getName());
            }
        }

        // Calculate GST based on state code
        String customerStateCode = invoice.getGstin() != null && invoice.getGstin().length() >= 2
            ? invoice.getGstin().substring(0, 2)
            : "";
        String companyStateCode = "23"; // Madhya Pradesh

        double cgst = 0, sgst = 0, igst = 0, subtotal = 0;

        for (SalesItem item : invoice.getItems()) {
            double itemTotal = item.getQuantity() * item.getPrice();
            double gstAmount = itemTotal * item.getGstRate() / 100;

            item.setTotal(itemTotal);
            item.setGstAmount(gstAmount);

            subtotal += itemTotal;

            if (customerStateCode.equals(companyStateCode)) {
                cgst += gstAmount / 2;
                sgst += gstAmount / 2;
            } else {
                igst += gstAmount;
            }
        }

        invoice.setSubtotal(subtotal);
        invoice.setCgst(cgst);
        invoice.setSgst(sgst);
        invoice.setIgst(igst);
        invoice.setTotal(subtotal + cgst + sgst + igst + invoice.getFreight());

        SalesInvoice savedInvoice = salesRepo.save(invoice);

        for (SalesItem saleItem : invoice.getItems()) {
            String itemId = saleItem.getItemId();
            int quantitySold = saleItem.getQuantity();

            Item item = itemRepo.findById(itemId).get();
            item.setStock(item.getStock() - quantitySold);
            itemRepo.save(item);
        }

        return ResponseEntity.ok(savedInvoice);
    }





    @GetMapping
    public ResponseEntity<List<SalesInvoice>> getAllSales() {
        return ResponseEntity.ok(salesRepo.findAll());
    }   
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSale(@PathVariable String id, @RequestBody SalesInvoice updatedInvoice) {
        Optional<SalesInvoice> existingOpt = salesRepo.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SalesInvoice existingInvoice = existingOpt.get();

        // Step 1: Restore stock from original sale
        for (SalesItem oldItem : existingInvoice.getItems()) {
            Optional<Item> itemOpt = itemRepo.findById(oldItem.getItemId());
            itemOpt.ifPresent(item -> {
                item.setStock(item.getStock() - oldItem.getQuantity()); // revert old sale
                itemRepo.save(item);
            });
        }

        // Step 2: Check new stock availability
        for (SalesItem newItem : updatedInvoice.getItems()) {
            Optional<Item> itemOpt = itemRepo.findById(newItem.getItemId());
            if (itemOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Item not found: " + newItem.getItemId());
            }
            Item item = itemOpt.get();
            if (newItem.getQuantity() > item.getStock()) {
                return ResponseEntity.badRequest().body("Not enough stock for item: " + item.getName());
            }
        }

        // Step 3: Calculate GST and totals
        String customerStateCode = updatedInvoice.getGstin() != null && updatedInvoice.getGstin().length() >= 2
            ? updatedInvoice.getGstin().substring(0, 2)
            : "";
        String companyStateCode = "23"; // MP

        double subtotal = 0, cgst = 0, sgst = 0, igst = 0;

        for (SalesItem item : updatedInvoice.getItems()) {
            double itemTotal = item.getQuantity() * item.getPrice() * (1 - item.getDiscount() / 100);
            double gstAmount = itemTotal * item.getGstRate() / 100;

            item.setTotal(itemTotal);
            item.setGstAmount(gstAmount);

            subtotal += itemTotal;

            if (companyStateCode.equals(customerStateCode)) {
                cgst += gstAmount / 2;
                sgst += gstAmount / 2;
            } else {
                igst += gstAmount;
            }
        }

        updatedInvoice.setId(id); // Ensure ID is preserved
        updatedInvoice.setSubtotal(subtotal);
        updatedInvoice.setCgst(cgst);
        updatedInvoice.setSgst(sgst);
        updatedInvoice.setIgst(igst);
        updatedInvoice.setTotal(subtotal + cgst + sgst + igst + updatedInvoice.getFreight());

        // Step 4: Save invoice
        SalesInvoice saved = salesRepo.save(updatedInvoice);

        // Step 5: Deduct stock as per new sale
        for (SalesItem item : updatedInvoice.getItems()) {
            Item dbItem = itemRepo.findById(item.getItemId()).get();
            dbItem.setStock(dbItem.getStock() - item.getQuantity());
            itemRepo.save(dbItem);
        }

        // Step 6: Enrich items with originalQuantity and currentStock (for frontend)
        List<SalesItem> enrichedItems = updatedInvoice.getItems().stream().map(i -> {
            itemRepo.findById(i.getItemId()).ifPresent(dbItem -> {
                i.setOriginalQuantity(i.getQuantity()); // For edit tracking
                i.setCurrentStock(dbItem.getStock());   // Stock after update
            });
            return i;
        }).collect(Collectors.toList());

        saved.setItems(enrichedItems);

        return ResponseEntity.ok(saved);
    }


    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestBody Map<String, String> payload) {
        String newStatus = payload.get("status");
        Optional<SalesInvoice> optional = salesRepo.findById(id);
        if (optional.isPresent()) {
            SalesInvoice invoice = optional.get();
            invoice.setStatus(newStatus);
            salesRepo.save(invoice);
            return ResponseEntity.ok("Status updated");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Sale not found");
    }

    @GetMapping("/profit/{itemId}")
    public ResponseEntity<?> calculateProfit(@PathVariable String itemId) {
        List<SalesInvoice> allSales = salesRepo.findAll();
        Optional<Item> optionalItem = itemRepo.findById(itemId);

        if (optionalItem.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Item not found");
        }

        Item item = optionalItem.get();
        double buyingPrice = item.getBuyingPrice();
        double commissionValue = item.getCommission();
        boolean isPercent = item.isCommissionPercent();

        int totalQty = 0;
        double totalProfit = 0;
        double totalSelling = 0;
        double totalCommission = 0;

        for (SalesInvoice invoice : allSales) {
            for (SalesItem saleItem : invoice.getItems()) {
                if (!saleItem.getItemId().equals(itemId)) continue;

                int qty = saleItem.getQuantity();
                double sp = saleItem.getPrice();

                double commission = isPercent
                    ? (sp * qty * commissionValue / 100.0)  // percentage of total sale
                    : commissionValue; // one-time fixed commission

                totalQty += qty;
                totalSelling += sp * qty;
                totalCommission += commission;
                totalProfit += ((sp - buyingPrice) * qty) - commission;
            }
        }

        double averageSellingPrice = totalQty > 0 ? totalSelling / totalQty : 0;

        return ResponseEntity.ok(Map.of(
            "itemId", itemId,
            "itemName", item.getName(),
            "buyingPrice", buyingPrice,
            "totalQuantitySold", totalQty,
            "averageSellingPrice", averageSellingPrice,
            "totalCommission", totalCommission,
            "totalProfit", totalProfit
        ));
    }


    @GetMapping("/profit/all")
    public ResponseEntity<?> getAllItemProfits(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {

        List<Item> items = itemRepo.findAll();
        List<SalesInvoice> allSales = salesRepo.findAll();

        LocalDate fromDate = (from != null) ? LocalDate.parse(from) : null;
        LocalDate toDate = (to != null) ? LocalDate.parse(to) : null;

        List<Map<String, Object>> result = new ArrayList<>();

        for (Item item : items) {
            String itemId = item.getId();
            double buyingPrice = item.getBuyingPrice();
            double commissionValue = item.getCommission();
            boolean isPercent = item.isCommissionPercent();

            int totalQty = 0;
            double totalSelling = 0;
            double totalCommission = 0;
            double totalProfit = 0;

            for (SalesInvoice invoice : allSales) {
                LocalDate saleDate = invoice.getDate(); // assuming it's LocalDate
                if ((fromDate != null && saleDate.isBefore(fromDate)) ||
                    (toDate != null && saleDate.isAfter(toDate))) {
                    continue;
                }

                for (SalesItem saleItem : invoice.getItems()) {
                    if (!saleItem.getItemId().equals(itemId)) continue;

                    int qty = saleItem.getQuantity();
                    double sp = saleItem.getPrice();

                    double commission = isPercent
                        ? (sp * qty * commissionValue / 100.0)  // percent on total selling
                        : commissionValue; // fixed one-time commission per invoice

                    totalQty += qty;
                    totalSelling += sp * qty;
                    totalCommission += commission;
                    totalProfit += ((sp - buyingPrice) * qty) - commission;
                }
            }

            double avgSp = totalQty > 0 ? totalSelling / totalQty : 0;

            result.add(Map.of(
                    "itemId", itemId,
                    "itemName", item.getName(),
                    "buyingPrice", buyingPrice,
                    "quantitySold", totalQty,
                    "averageSellingPrice", avgSp,
                    "totalCommission", totalCommission,
                    "totalProfit", totalProfit
            ));
        }

        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/download-report")
    public void downloadPurchaseReport(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            HttpServletResponse response
    ) throws IOException {

        List<SalesInvoice> sales;
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
            sales = salesRepo.findInvoicesBetweenDates(fromDate, toDate);
        } else {
        	sales = salesRepo.findAll();
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

        for (SalesInvoice invoice : sales) {
            String billNo = invoice.getInvoiceNo();
            String billDate = invoice.getInvoiceDate().format(formatter);
            String supplier = invoice.getCustomerName();
            String gstNo = invoice.getGstin();
            double freight = invoice.getFreight();

            String vendorStateCode = gstNo != null && gstNo.length() >= 2 ? gstNo.substring(0, 2) : "";
            String companyStateCode = "23"; // MP

            double invoiceTotal = 0.0;
            double invoiceTaxableAmount = 0.0;

            int invoiceStartRow = rowIdx;

            for (SalesItem item : invoice.getItems()) {
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
        response.setHeader("Content-Disposition", "attachment; filename=Sales_Report.xlsx");

        workbook.write(response.getOutputStream());
        workbook.close();
    }
    
    


}
