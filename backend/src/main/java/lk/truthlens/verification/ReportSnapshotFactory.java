package lk.truthlens.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import lk.truthlens.integration.article.FacebookPostExtractor;
import lk.truthlens.model.Submission;

public final class ReportSnapshotFactory {

    public static String initialSnapshot(Submission row, ObjectMapper json) {
        return snapshot(row, json, Map.of(), "RECEIVED");
    }

    public static String snapshot(
        Submission row,
        ObjectMapper json,
        Map<String, Integer> metrics,
        String stage
    ) {
        var passage = Map.of(
            "id",
            1,
            "text",
            row.getContent(),
            "finding",
            "Not assessed",
            "explanation",
            "This submission has not been fact-checked. No evidence has been retrieved or assessed.",
            "support",
            List.of(),
            "against",
            List.of(),
            "context",
            "Source dates, conflicts and confidence are unavailable until evidence verification is performed.",
            "confidence",
            "Not assessed"
        );
        var report = new LinkedHashMap<String, Object>();
        report.put("id", row.getId());
        report.put("title", row.getTitle());
        report.put("content", row.getContent());
        report.put("date", row.getCreatedAt());
        report.put("sample", false);
        report.put("inputType", row.getInputType());
        if (row.getOriginalUrl() != null) {
            report.put("originalUrl", row.getOriginalUrl());
            report.put("resolvedUrl", row.getResolvedUrl());
            if (FacebookPostExtractor.facebook(row.getResolvedUrl())) report.put(
                "extractionNote",
                FacebookPostExtractor.NOTE
            );
        }
        report.put("stored", true);
        report.put("accountOwned", row.getOwnerId() != null);
        report.put("status", row.getStatus());
        report.put("stage", stage);
        if (!metrics.isEmpty()) report.put("textMetrics", metrics);
        report.put("claims", List.of(passage));
        try {
            return json.writeValueAsString(report);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Unable to prepare report", e);
        }
    }

    public static String withClaims(Submission row, ObjectMapper json, List<Map<String,Object>> claims, String stage) {
        try {
            var report = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(snapshot(row, json, TextStatistics.measure(row.getContent()), stage));
            report.set("claims", json.valueToTree(claims));
            report.put("evidenceMethod", "Wikipedia public search (discovery only; no automatic truth verification)");
            return json.writeValueAsString(report);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not save claim report", e);
        }
    }

    public static String failure(Submission row, ObjectMapper json, String error) {
        try {
            var report = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(row.getReportJson());
            report.put("stage", "FAILED"); report.put("error", error);
            return json.writeValueAsString(report);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not save partial report", e);
        }
    }

    private ReportSnapshotFactory() {}
}
