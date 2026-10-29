package com.platform.module.pms.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public final class RfpParseDto {

    private RfpParseDto() {
    }

    /** 문서에서 읽은 항목 한 건 — 화면에서 확인·수정한 뒤 RFP 항목 일괄 등록 API 로 저장한다. */
    @Schema(name = "PmsRfpParsedItem")
    public record Item(
            @Schema(description = "문서의 고유번호 또는 줄 단위 분해 번호(FUN-001, FUN-001-03)") String itemCode,
            @Schema(description = "우리 분류 코드(FUN → SFR 등)") String categoryCode,
            @Schema(description = "문서의 구분(영역)") String area,
            String itemName,
            String content,
            boolean mandatory,
            @Schema(description = "줄 단위 분해일 때 상위 요구사항의 고유번호(아니면 itemCode 와 같다)") String parentCode,
            @Schema(description = "줄 단위 분해 번호(0 이면 요구사항 단위)") int lineNo
    ) {
    }

    @Schema(name = "PmsRfpParseResponse")
    public record Response(
            String fileName,
            String format,
            @Schema(description = "읽은 요구사항(카드) 수") int requirementCount,
            @Schema(description = "미리보기 항목 수(줄 단위 분해면 줄 수)") int itemCount,
            boolean splitLines,
            List<Item> items,
            @Schema(description = "확인이 필요한 점(카드를 못 찾은 목록 항목, 세부내용이 비어 있는 요구사항 등)") List<String> warnings
    ) {
    }
}
