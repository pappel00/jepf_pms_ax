package com.platform.module.pms.service;

import java.util.Map;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** JSONB 컬럼(selection_json, before_json, after_json)에 넣고 꺼낼 때 쓰는 JSON 변환. */
final class PmsJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private PmsJson() {
    }

    static String write(Object value) {
        return MAPPER.writeValueAsString(value);
    }

    static <T> T read(String json, Class<T> type) {
        return MAPPER.readValue(json, type);
    }

    static Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
        });
    }
}