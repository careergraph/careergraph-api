package com.hcmute.careergraph.helper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Trích text thuần từ nội dung file CV (PDF hoặc DOCX).
 */
public final class ResumeDocumentTextExtractor {

    private ResumeDocumentTextExtractor() {
    }

    public static String extractText(byte[] data, String mimeType, String fileName) throws IOException {
        if (data == null || data.length == 0) {
            return "";
        }
        String mt = mimeType != null ? mimeType.toLowerCase(Locale.ROOT) : "";
        String fn = fileName != null ? fileName.toLowerCase(Locale.ROOT) : "";

        if (mt.contains("pdf") || fn.endsWith(".pdf")) {
            return extractPdf(data);
        }
        if (mt.contains("wordprocessingml") || mt.contains("officedocument") || fn.endsWith(".docx")) {
            return extractDocx(data);
        }
        if (mt.contains("msword") || fn.endsWith(".doc")) {
            throw new IOException("Định dạng .doc cũ chưa được hỗ trợ; vui lòng tải PDF hoặc DOCX.");
        }
        if (fn.endsWith(".pdf")) {
            return extractPdf(data);
        }
        if (fn.endsWith(".docx")) {
            return extractDocx(data);
        }
        throw new IOException("Không hỗ trợ trích text cho loại file: " + mimeType);
    }

    private static String extractPdf(byte[] data) throws IOException {
        try (PDDocument doc = Loader.loadPDF(data)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            return normalizeWhitespace(text);
        }
    }

    private static String extractDocx(byte[] data) throws IOException {
        try (ByteArrayInputStream in = new ByteArrayInputStream(data);
                XWPFDocument doc = new XWPFDocument(in)) {
            String text = doc.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("\n"));
            return normalizeWhitespace(text);
        }
    }

    private static String normalizeWhitespace(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        return raw.replace("\u0000", " ")
                .lines()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
    }
}
