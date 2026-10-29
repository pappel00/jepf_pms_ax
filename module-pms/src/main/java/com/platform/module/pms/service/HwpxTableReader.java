package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.exception.PmsErrorCode;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * 한글(hwpx) 문서에서 표를 읽는다. hwpx 는 ZIP 안의 XML(Contents/section*.xml)이며, 표는 hp:tbl → hp:tr → hp:tc → hp:p(문단) 구조다.
 *  - 문서 순서대로 모든 표(중첩 표 포함)를 돌려주고, 셀은 문단 목록으로 보관한다(문단 경계가 줄 단위 분해의 기준).
 *  - 중첩 표의 텍스트는 바깥 셀에도 문단으로 합쳐 둔다.
 * 보안: 외부 엔티티·DTD 를 끄고, 압축 해제 크기·항목 수를 제한한다(압축 폭탄 방어). 이미지 등 본문 외 항목은 읽지 않는다.
 */
final class HwpxTableReader {

    /** 해제 후 XML 합계 상한. */
    private static final long MAX_UNCOMPRESSED = 200L * 1024 * 1024;
    private static final int MAX_ENTRIES = 500;

    record Cell(List<String> paragraphs) {
        String text() {
            return String.join("\n", paragraphs);
        }
    }

    record Row(List<Cell> cells) {
    }

    record Table(List<Row> rows) {
    }

    private HwpxTableReader() {
    }

    /** hwpx(ZIP) 스트림에서 본문 섹션의 모든 표를 문서 순서대로 읽는다. */
    static List<Table> read(InputStream in) {
        List<Table> tables = new ArrayList<>();
        long[] total = {0};
        int entries = 0;
        try (ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new BusinessException(PmsErrorCode.RFP_INVALID, "문서 구조가 올바르지 않습니다(항목이 너무 많습니다).");
                }
                String name = e.getName();
                if (!name.startsWith("Contents/section") || !name.endsWith(".xml")) {
                    continue;
                }
                parseSection(new CountingStream(zip, total), tables);
            }
        } catch (IOException | XMLStreamException ex) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "hwpx 문서를 읽지 못했습니다: " + ex.getMessage());
        }
        if (entries == 0) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "hwpx 문서가 아닙니다.");
        }
        return tables;
    }

    private static void parseSection(InputStream in, List<Table> out) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        XMLStreamReader r = f.createXMLStreamReader(in);
        try {
            Deque<TableB> tables = new ArrayDeque<>();
            Deque<StringBuilder> paras = new ArrayDeque<>();
            boolean inText = false;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String n = hp(r);
                    if ("tbl".equals(n)) {
                        tables.push(new TableB());
                    } else if ("tr".equals(n) && !tables.isEmpty()) {
                        tables.peek().row = new ArrayList<>();
                    } else if ("tc".equals(n) && !tables.isEmpty()) {
                        tables.peek().cell = new ArrayList<>();
                    } else if ("p".equals(n)) {
                        paras.push(new StringBuilder());
                    } else if ("t".equals(n)) {
                        inText = true;
                    }
                } else if (ev == XMLStreamConstants.CHARACTERS && inText && !paras.isEmpty()) {
                    paras.peek().append(r.getText());
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String n = hp(r);
                    if ("t".equals(n)) {
                        inText = false;
                    } else if ("p".equals(n) && !paras.isEmpty()) {
                        // 전각 공백(U+3000)·NBSP 도 공백으로 본다(한글 문서의 빈 줄은 전각 공백인 경우가 많다).
                        String text = paras.pop().toString().replaceAll("[\\s\\u3000\\u00a0]+", " ").strip();
                        if (!text.isEmpty() && !tables.isEmpty() && tables.peek().cell != null) {
                            tables.peek().cell.add(text);
                        }
                    } else if ("tc".equals(n) && !tables.isEmpty()) {
                        TableB t = tables.peek();
                        if (t.row != null && t.cell != null) {
                            t.row.add(new Cell(t.cell));
                        }
                        t.cell = null;
                    } else if ("tr".equals(n) && !tables.isEmpty()) {
                        TableB t = tables.peek();
                        if (t.row != null) {
                            t.rows.add(new Row(t.row));
                        }
                        t.row = null;
                    } else if ("tbl".equals(n) && !tables.isEmpty()) {
                        TableB done = tables.pop();
                        Table table = new Table(done.rows);
                        // 중첩 표: 바깥 셀에도 문단으로 합쳐 둔다.
                        if (!tables.isEmpty() && tables.peek().cell != null) {
                            for (Row row : table.rows()) {
                                for (Cell c : row.cells()) {
                                    tables.peek().cell.addAll(c.paragraphs());
                                }
                            }
                        }
                        out.add(table);
                    }
                }
            }
        } finally {
            r.close();
        }
    }

    private static String hp(XMLStreamReader r) {
        return "hp".equals(r.getPrefix()) ? r.getLocalName() : "";
    }

    private static final class TableB {
        final List<Row> rows = new ArrayList<>();
        List<Cell> row;
        List<String> cell;
    }

    /** 해제된 바이트 수를 세어 상한을 넘으면 중단한다. */
    private static final class CountingStream extends FilterInputStream {
        private final long[] total;

        CountingStream(InputStream in, long[] total) {
            super(in);
            this.total = total;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b >= 0) {
                add(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buf, int off, int len) throws IOException {
            int n = super.read(buf, off, len);
            if (n > 0) {
                add(n);
            }
            return n;
        }

        /** XML 파서가 닫아도 ZIP 스트림은 닫지 않는다. */
        @Override
        public void close() {
            // 다음 ZIP 항목을 계속 읽어야 한다.
        }

        private void add(long n) throws IOException {
            total[0] += n;
            if (total[0] > MAX_UNCOMPRESSED) {
                throw new IOException("압축 해제 크기가 상한을 넘었습니다.");
            }
        }
    }
}
