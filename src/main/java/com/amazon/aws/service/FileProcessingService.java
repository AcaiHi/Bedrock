package com.amazon.aws.service;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileProcessingService {

    private static final int BATCH_SIZE = 100; // Adjust batch size as needed

    public String processFiles(MultipartFile wordFile, MultipartFile excelFile) throws Exception {
        String wordContent = processWordFile(wordFile);
        String excelContent = processExcelFile(excelFile);

        return wordContent + "\n\n" + excelContent;
    }

    private String processWordFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        InputStream inputStream = file.getInputStream();
        if (fileName.endsWith(".docx")) {
            return readWordFile(inputStream);
        } else if (fileName.endsWith(".doc")) {
            return readOldWordFile(inputStream);
        } else {
            throw new IllegalArgumentException("Unsupported word file type");
        }
    }

    private String processExcelFile(MultipartFile file) throws Exception {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        InputStream inputStream = file.getInputStream();
        if (fileName.endsWith(".xlsx")) {
            return readExcelFile(inputStream);
        } else if (fileName.endsWith(".xls")) {
            return readOldExcelFile(inputStream);
        } else {
            throw new IllegalArgumentException("Unsupported excel file type");
        }
    }

    private String readWordFile(InputStream inputStream) throws Exception {
        XWPFDocument document = new XWPFDocument(inputStream);
        List<String> batchContent = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        for (XWPFParagraph paragraph : document.getParagraphs()) {
            content.append(paragraph.getText()).append("\n");
            if (batchContent.size() >= BATCH_SIZE) {
                // Process batch
                processBatch(batchContent);
                batchContent.clear();
            }
        }

        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    content.append(cell.getText()).append("\t");
                }
                content.append("\n");
            }
            content.append("\n");
            if (batchContent.size() >= BATCH_SIZE) {
                // Process batch
                processBatch(batchContent);
                batchContent.clear();
            }
        }

        if (!batchContent.isEmpty()) {
            processBatch(batchContent);
        }

        return content.toString();
    }

    private String readOldWordFile(InputStream inputStream) throws Exception {
        HWPFDocument document = new HWPFDocument(inputStream);
        WordExtractor extractor = new WordExtractor(document);
        List<String> batchContent = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        Range range = document.getRange();
        for (int i = 0; i < range.numParagraphs(); i++) {
            Paragraph paragraph = range.getParagraph(i);
            content.append(paragraph.text()).append("\n");
            if (batchContent.size() >= BATCH_SIZE) {
                // Process batch
                processBatch(batchContent);
                batchContent.clear();
            }
        }

        for (int i = 0; i < range.numParagraphs(); i++) {
            Paragraph paragraph = range.getParagraph(i);
            if (paragraph.isInTable()) {
                Table table = range.getTable(paragraph);
                for (int j = 0; j < table.numRows(); j++) {
                    TableRow row = table.getRow(j);
                    for (int k = 0; k < row.numCells(); k++) {
                        TableCell cell = row.getCell(k);
                        content.append(cell.getParagraph(0).text()).append("\t");
                    }
                    content.append("\n");
                    if (batchContent.size() >= BATCH_SIZE) {
                        // Process batch
                        processBatch(batchContent);
                        batchContent.clear();
                    }
                }
                content.append("\n");
                // Skip over the rest of the table
                i += table.numParagraphs();
            }
        }

        if (!batchContent.isEmpty()) {
            processBatch(batchContent);
        }

        return content.toString();
    }

    private String readExcelFile(InputStream inputStream) throws Exception {
        XSSFWorkbook workbook = new XSSFWorkbook(inputStream);
        XSSFSheet sheet = workbook.getSheetAt(0);
        List<String> batchContent = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        sheet.forEach(row -> {
            row.forEach(cell -> {
                switch (cell.getCellType()) {
                    case STRING:
                        content.append(cell.getStringCellValue()).append("\t");
                        break;
                    case NUMERIC:
                        content.append(cell.getNumericCellValue()).append("\t");
                        break;
                    default:
                        content.append(" ").append("\t");
                }
            });
            content.append("\n");
            if (batchContent.size() >= BATCH_SIZE) {
                // Process batch
                processBatch(batchContent);
                batchContent.clear();
            }
        });

        if (!batchContent.isEmpty()) {
            processBatch(batchContent);
        }

        return content.toString();
    }

    private String readOldExcelFile(InputStream inputStream) throws Exception {
        HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
        HSSFSheet sheet = workbook.getSheetAt(0);
        List<String> batchContent = new ArrayList<>();
        StringBuilder content = new StringBuilder();

        sheet.forEach(row -> {
            row.forEach(cell -> {
                switch (cell.getCellType()) {
                    case STRING:
                        content.append(cell.getStringCellValue()).append("\t");
                        break;
                    case NUMERIC:
                        content.append(cell.getNumericCellValue()).append("\t");
                        break;
                    default:
                        content.append(" ").append("\t");
                }
            });
            content.append("\n");
            if (batchContent.size() >= BATCH_SIZE) {
                // Process batch
                processBatch(batchContent);
                batchContent.clear();
            }
        });

        if (!batchContent.isEmpty()) {
            processBatch(batchContent);
        }

        return content.toString();
    }

    private void processBatch(List<String> batchContent) {
        // Process each batch content, for example, saving to file or database
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt", true))) {
            for (String line : batchContent) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
