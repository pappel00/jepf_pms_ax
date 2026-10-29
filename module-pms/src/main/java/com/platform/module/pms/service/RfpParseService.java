package com.platform.module.pms.service;

import com.platform.framework.core.common.exception.BusinessException;
import com.platform.module.pms.dto.RfpParseDto;
import com.platform.module.pms.exception.PmsErrorCode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * RFP 문서 자동 분해 — 문서에서 요구사항을 읽어 항목 후보(미리보기)를 만든다. 저장은 하지 않는다(화면에서 확인 후 항목 일괄 등록).
 *
 * 지원 형식: hwpx(한글). 다음 두 가지 표 모양을 읽는다.
 *  - 요구사항 목록표: 「구분 / 고유번호 / 요구사항 명칭」 — 분류(구분) 이름과 순서, 누락 점검에 쓴다.
 *  - 요구사항 상세 카드표: 첫 행에 「구분 … 고유번호 XXX-000」, 이어서 「명칭 / 요구사항(정의) / 세부내용 / 산출문서」 행.
 *    세부내용 뒤~산출문서 앞의 추가 행(예: DW DB, ETL)은 세부내용 줄로 합친다.
 * 옵션 splitLines: 세부내용의 문단(줄) 하나하나를 별도 항목으로 만든다(코드 FUN-001-01 …). 세부내용이 없으면 요구사항 단위로 둔다.
 * 쪽수는 hwpx 에 없어(흐름 레이아웃) 채우지 않는다.
 */
@Service
public class RfpParseService {

    private static final long MAX_BYTES = 30L * 1024 * 1024;
    private static final Pattern CODE = Pattern.compile("^[A-Z]{2,4}-\\d{2,4}$");
    private static final Pattern BULLET = Pattern.compile("^[\\s\\u3000\\-–·ㆍ•○●◦▪■□※*▶>]+");

    /** 문서 고유번호 접두어 → 우리 요구사항 분류 코드. 목록에 없으면 접두어를 그대로 분류로 쓴다. */
    private static final Map<String, String> CATEGORY = Map.of("FUN", "SFR");

    public RfpParseDto.Response parse(MultipartFile file, boolean splitLines) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "문서를 선택하세요.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "문서 크기는 30MB 이하여야 합니다.");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        if (!name.toLowerCase().endsWith(".hwpx")) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "현재는 hwpx(한글) 문서만 자동 분해할 수 있습니다. 다른 형식은 엑셀 템플릿을 사용하세요.");
        }
        List<HwpxTableReader.Table> tables;
        try {
            tables = HwpxTableReader.read(file.getInputStream());
        } catch (IOException e) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID, "문서를 읽지 못했습니다: " + e.getMessage());
        }
        return build(name, tables, splitLines);
    }

    /** 표 목록에서 요구사항을 추려 미리보기를 만든다(파일 없이 테스트할 수 있게 분리). */
    RfpParseDto.Response build(String fileName, List<HwpxTableReader.Table> tables, boolean splitLines) {
        List<String> warnings = new ArrayList<>();

        // 1) 목록표: 고유번호 → (명칭, 구분 이름), 문서 순서
        Map<String, String[]> listed = new LinkedHashMap<>();
        for (HwpxTableReader.Table t : tables) {
            if (!isListTable(t)) {
                continue;
            }
            String category = "";
            for (HwpxTableReader.Row r : t.rows()) {
                List<HwpxTableReader.Cell> c = r.cells();
                if (c.isEmpty() || isHeader(c)) {
                    continue;
                }
                int base = c.size() >= 4 || (c.size() == 3 && !CODE.matcher(c.get(0).text().trim()).matches()) ? 1 : 0;
                if (base == 1) {
                    category = clean(c.get(0).text());
                }
                if (c.size() > base + 1 && CODE.matcher(c.get(base).text().trim()).matches()) {
                    listed.putIfAbsent(c.get(base).text().trim(), new String[]{c.get(base + 1).text().trim(), category});
                }
            }
        }

        // 2) 카드표
        Map<String, Card> cards = new LinkedHashMap<>();
        for (HwpxTableReader.Table t : tables) {
            Card card = readCard(t);
            if (card != null) {
                Card old = cards.get(card.code);
                if (old == null || card.size() > old.size()) {
                    cards.put(card.code, card);
                }
            }
        }
        if (cards.isEmpty()) {
            throw new BusinessException(PmsErrorCode.RFP_INVALID,
                    "요구사항 상세표(구분·고유번호·명칭·세부내용)를 찾지 못했습니다. 문서 형식이 다르면 엑셀 템플릿을 사용하세요.");
        }

        // 3) 순서: 목록표가 있으면 그 순서, 없는 카드는 뒤에
        List<Card> ordered = new ArrayList<>();
        for (String code : listed.keySet()) {
            Card c = cards.get(code);
            if (c == null) {
                warnings.add(code + " " + listed.get(code)[0] + ": 목록에는 있으나 상세표를 찾지 못했습니다.");
            } else {
                ordered.add(c);
            }
        }
        cards.values().stream().filter(c -> !listed.containsKey(c.code)).forEach(c -> {
            ordered.add(c);
            if (!listed.isEmpty()) {
                warnings.add(c.code + " " + c.name + ": 상세표는 있으나 목록표에 없습니다.");
            }
        });

        // 4) 항목 만들기
        List<RfpParseDto.Item> items = new ArrayList<>();
        for (Card c : ordered) {
            String prefix = c.code.substring(0, c.code.indexOf('-'));
            String category = CATEGORY.getOrDefault(prefix, prefix);
            String area = shorten(areaOf(c, listed), 100);
            if (c.lines.isEmpty() && c.definition.isBlank()) {
                warnings.add(c.code + " " + c.name + ": 정의·세부내용이 비어 있습니다.");
            }
            if (!splitLines || c.lines.isEmpty()) {
                items.add(new RfpParseDto.Item(c.code, category, area, shorten(c.name, 300), content(c), true, c.code, 0));
                continue;
            }
            int n = 0;
            for (String line : c.lines) {
                n++;
                String text = BULLET.matcher(line).replaceFirst("").strip();
                if (text.isEmpty()) {
                    continue;
                }
                String content = text + "\n(상위 요구사항: " + c.code + " " + c.name + ")";
                items.add(new RfpParseDto.Item(String.format("%s-%02d", c.code, n), category, area, shorten(text, 100), content, true, c.code, n));
            }
        }
        return new RfpParseDto.Response(fileName, "hwpx", ordered.size(), items.size(), splitLines, items, warnings);
    }

    // ---------- 표 해석 ----------

    private record Card(String code, String name, String category, String definition, List<String> lines, String outputDoc) {
        int size() {
            return definition.length() + lines.stream().mapToInt(String::length).sum();
        }
    }

    private static String areaOf(Card c, Map<String, String[]> listed) {
        String[] l = listed.get(c.code);
        String fromList = l == null ? "" : l[1];
        return !fromList.isBlank() ? fromList : c.category;
    }

    private static String content(Card c) {
        StringBuilder sb = new StringBuilder();
        if (!c.definition.isBlank()) {
            sb.append("[정의] ").append(c.definition);
        }
        if (!c.lines.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("[세부내용]\n").append(String.join("\n", c.lines));
        }
        if (!c.outputDoc.isBlank()) {
            sb.append("\n\n[산출문서] ").append(c.outputDoc);
        }
        return sb.toString();
    }

    private static boolean isListTable(HwpxTableReader.Table t) {
        if (t.rows().size() < 5 || t.rows().get(0).cells().size() < 3) {
            return false;
        }
        String head = norm(t.rows().get(0).cells().stream().map(HwpxTableReader.Cell::text).reduce("", (a, b) -> a + b));
        return head.contains("고유번호") && head.contains("명칭") && !head.contains("세부내용");
    }

    private static boolean isHeader(List<HwpxTableReader.Cell> c) {
        return c.stream().anyMatch(x -> norm(x.text()).equals("고유번호"));
    }

    /** 카드표면 읽어서 돌려주고, 아니면 null. */
    private static Card readCard(HwpxTableReader.Table t) {
        if (t.rows().isEmpty()) {
            return null;
        }
        List<HwpxTableReader.Cell> first = t.rows().get(0).cells();
        if (first.size() < 4 || !norm(first.get(0).text()).equals("구분")) {
            return null;
        }
        String code = null;
        for (int i = 0; i < first.size() - 1; i++) {
            if (norm(first.get(i).text()).equals("고유번호")) {
                String v = first.get(i + 1).text().trim();
                if (CODE.matcher(v).matches()) {
                    code = v;
                }
            }
        }
        if (code == null) {
            return null;
        }
        String category = clean(first.get(1).text());
        String name = "";
        String definition = "";
        String outputDoc = "";
        List<String> lines = new ArrayList<>();
        boolean inDetail = false;
        for (int r = 1; r < t.rows().size(); r++) {
            List<HwpxTableReader.Cell> cells = t.rows().get(r).cells();
            if (cells.isEmpty()) {
                continue;
            }
            String key = norm(cells.get(0).text());
            if (key.equals("명칭")) {
                name = cells.size() > 1 ? cells.get(1).text().replace('\n', ' ').trim() : "";
                inDetail = false;
            } else if (key.equals("요구사항")) {
                // [요구사항 | 정의 | 값] 또는 [요구사항 | 값]
                definition = cells.get(cells.size() - 1).text().replace('\n', ' ').trim();
                inDetail = false;
            } else if (key.equals("세부내용")) {
                inDetail = true;
                for (int i = 1; i < cells.size(); i++) {
                    addLines(lines, cells.get(i));
                }
            } else if (key.equals("산출문서")) {
                outputDoc = cells.size() > 1 ? cells.get(cells.size() - 1).text().replace('\n', ' ').trim() : "";
                inDetail = false;
            } else if (inDetail) {
                // 세부내용 뒤의 추가 행(예: DW DB / OLAP / ETL) — 「라벨: 내용」 줄로 합친다.
                String label = cells.get(0).text().replace('\n', ' ').trim();
                for (int i = 1; i < cells.size(); i++) {
                    for (String p : cells.get(i).paragraphs()) {
                        lines.add(label.isEmpty() ? p : label + ": " + p);
                    }
                }
            }
        }
        return new Card(code, name, category, definition, lines, outputDoc);
    }

    private static void addLines(List<String> lines, HwpxTableReader.Cell cell) {
        for (String p : cell.paragraphs()) {
            String s = p.strip();
            if (!s.isEmpty()) {
                lines.add(s);
            }
        }
    }

    /** 공백을 모두 지운 비교용 문자열(「구  분」 → 「구분」). */
    private static String norm(String s) {
        return s == null ? "" : s.replaceAll("\\s+", "");
    }

    /** 「기능요구사항(System Function Requirements)」 → 「기능요구사항」. */
    private static String clean(String s) {
        Matcher m = Pattern.compile("^(.*?)\\s*[(（].*$").matcher(s == null ? "" : s.replace('\n', ' ').trim());
        return (m.matches() ? m.group(1) : s == null ? "" : s.replace('\n', ' ')).trim();
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace('\n', ' ').trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
