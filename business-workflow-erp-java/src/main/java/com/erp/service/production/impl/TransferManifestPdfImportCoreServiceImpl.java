package com.erp.service.production.impl;

import com.erp.controller.production.dto.ImportPdfTransferManifestResponse;
import com.erp.entity.production.TransferManifest;
import com.erp.mapper.production.TransferManifestMapper;
import com.erp.service.common.FileService;
import com.erp.service.production.ITransferManifestPdfImportCoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 转移联单 PDF 导入核心服务实现
 *
 * @author ERP System
 */
@Slf4j
@Service
public class TransferManifestPdfImportCoreServiceImpl implements ITransferManifestPdfImportCoreService {

    @Autowired
    private TransferManifestMapper transferManifestMapper;

    @Autowired
    private FileService fileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPdfTransferManifestResponse importFromPdfBytes(byte[] fileBytes,
                                                               String originalFilename,
                                                               Integer uploaderId) {
        PDDocument fullDoc;
        try {
            fullDoc = PDDocument.load(fileBytes);
        } catch (Exception e) {
            log.error("PDF 字节流加载失败", e);
            throw new RuntimeException("PDF 文件加载失败：" + e.getMessage());
        }
        return doPdfImport(fullDoc, uploaderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPdfTransferManifestResponse importFromPdf(MultipartFile file, Integer uploaderId) {
        PDDocument fullDoc;
        try {
            fullDoc = PDDocument.load(file.getInputStream());
        } catch (Exception e) {
            log.error("PDF 文件加载失败", e);
            throw new RuntimeException("PDF 文件加载失败：" + e.getMessage());
        }
        return doPdfImport(fullDoc, uploaderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportPdfTransferManifestResponse doPdfImport(PDDocument fullDoc, Integer uploaderId) {
        try {
            int totalPages = fullDoc.getNumberOfPages();
            if (totalPages == 0) throw new RuntimeException("PDF 文件不含任何页面");

            PDFTextStripper stripper = new PDFTextStripper();
            List<List<Integer>> groups = new ArrayList<>();
            List<String> pageTexts = new ArrayList<>(totalPages);
            for (int i = 0; i < totalPages; i++) {
                stripper.setStartPage(i + 1);
                stripper.setEndPage(i + 1);
                String pageText = stripper.getText(fullDoc);
                pageTexts.add(pageText == null ? "" : pageText);
                if (pageText != null && pageText.contains("危险废物转移联单")) {
                    groups.add(new ArrayList<>(Collections.singletonList(i)));
                } else if (!groups.isEmpty()) {
                    groups.get(groups.size() - 1).add(i);
                }
            }
            if (groups.isEmpty()) {
                throw new RuntimeException("PDF 中未找到任何「危险废物转移联单」标题页，无法解析");
            }

            int total = groups.size();
            Pattern digitPattern = Pattern.compile("\\d{6,}");
            String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());

            List<List<String>> groupDigits = new ArrayList<>(total);
            Set<String> allDigits = new LinkedHashSet<>();
            for (List<Integer> pageIndexes : groups) {
                StringBuilder sb = new StringBuilder();
                for (int pi : pageIndexes) sb.append(pageTexts.get(pi));
                Matcher m = digitPattern.matcher(sb.toString());
                Set<String> digitSet = new LinkedHashSet<>();
                while (m.find()) digitSet.add(m.group());
                List<String> digits = new ArrayList<>(digitSet);
                digits.sort((a, b) -> b.length() - a.length());
                groupDigits.add(digits);
                allDigits.addAll(digits);
            }

            Map<String, TransferManifest> digitToManifest = new HashMap<>();
            if (!allDigits.isEmpty()) {
                List<TransferManifest> candidates =
                        transferManifestMapper.selectByManifestNoDigitsBatch(new ArrayList<>(allDigits));
                for (TransferManifest tm : candidates) {
                    String gdNo2 = tm.getGdManifestNo() != null ? tm.getGdManifestNo() : "";
                    String natNo2 = tm.getNationalManifestNo() != null ? tm.getNationalManifestNo() : "";
                    for (String d : allDigits) {
                        if (!digitToManifest.containsKey(d)
                                && (gdNo2.contains(d) || natNo2.contains(d))) {
                            digitToManifest.put(d, tm);
                        }
                    }
                }
            }

            int matched = 0;
            List<String> unmatchedDetails = new ArrayList<>();
            List<TransferManifest> toUpdate = new ArrayList<>();

            for (int gi = 0; gi < groups.size(); gi++) {
                List<Integer> pageIndexes = groups.get(gi);
                int firstPage = pageIndexes.get(0) + 1;
                int lastPage = pageIndexes.get(pageIndexes.size() - 1) + 1;
                List<String> digits = groupDigits.get(gi);

                TransferManifest matchedManifest = null;
                String matchedDigit = null;
                for (String d : digits) {
                    if (digitToManifest.containsKey(d)) {
                        matchedManifest = digitToManifest.get(d);
                        matchedDigit = d;
                        break;
                    }
                }

                if (matchedManifest == null) {
                    unmatchedDetails.add(String.format(
                            "页 %d-%d：提取数字=%s，未匹配到任何联单记录",
                            firstPage, lastPage, digits.isEmpty() ? "(无)" : digits.get(0)));
                    continue;
                }

                byte[] subPdfBytes;
                try (PDDocument subDoc = new PDDocument()) {
                    for (int pi : pageIndexes) {
                        subDoc.importPage(fullDoc.getPage(pi));
                    }
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    subDoc.save(baos);
                    subPdfBytes = baos.toByteArray();
                } catch (Exception e) {
                    throw new RuntimeException(
                            String.format("页 %d-%d 子文件拆分失败：%s", firstPage, lastPage, e.getMessage()), e);
                }

                String gdNo = matchedManifest.getGdManifestNo();
                String subFileName = String.format("联单PDF_%s_%s_%02d.pdf",
                        gdNo != null ? gdNo : matchedDigit, timestamp, gi + 1);
                com.erp.entity.common.File savedFile = fileService.uploadBytesAndSave(
                        subPdfBytes, subFileName, "TRANSFER_MANIFEST",
                        matchedManifest.getManifestId(), uploaderId);

                TransferManifest updateEntry = new TransferManifest();
                updateEntry.setManifestId(matchedManifest.getManifestId());
                updateEntry.setPdfFileId(savedFile.getFileId());
                toUpdate.add(updateEntry);
                matched++;
            }

            if (!toUpdate.isEmpty()) {
                transferManifestMapper.batchUpdatePdfFileId(toUpdate);
            }

            return ImportPdfTransferManifestResponse.builder()
                    .total(total)
                    .matched(matched)
                    .unmatched(total - matched)
                    .unmatchedDetails(unmatchedDetails)
                    .build();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("PDF 导入处理失败", e);
            throw new RuntimeException("PDF 导入处理失败：" + e.getMessage(), e);
        } finally {
            try { fullDoc.close(); } catch (Exception ignore) {}
        }
    }
}
