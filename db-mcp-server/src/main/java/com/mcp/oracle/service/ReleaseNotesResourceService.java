package com.mcp.oracle.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;

/**
 * MCP resources for serving release notes as Markdown plus an index.
 *
 * Example JSON-RPC flows:
 * resources/list ->
 * {
 *   "resources": [
 *     {"uri": "relnote://index", "name": "releaseNotesIndex", "mimeType": "application/json"}
 *   ],
 *   "resourceTemplates": [
 *     {"uriTemplate": "relnote://{version}", "name": "releaseNotesByVersion", "mimeType": "text/markdown"}
 *   ]
 * }
 * resources/read relnote://index ->
 * {"versions":["2026.0.5","2026.1.1"]}
 * resources/read relnote://2026.0.5 ->
 * "# 2026.0.5\n- Fixed transaction retries..."
 */
@Service
public class ReleaseNotesResourceService {

    private static final Logger logger = LoggerFactory.getLogger(ReleaseNotesResourceService.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("^\\d{4}\\.[0-3]\\.\\d+$");
    private static final String RELEASE_NOTES_GLOB = "classpath:release-notes/*.md";
    private static final String RELEASE_NOTES_PREFIX = "classpath:release-notes/";

    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @McpResource(
            name = "releaseNotesIndex",
            title = "Release notes index",
            uri = "relnote://index",
            description = "Lists available release note versions",
            mimeType = "application/json")
    public String releaseNotesIndex() {
        List<String> versions = loadAvailableVersions();
        try {
            return "{\"versions\":" + toJsonArray(versions) + "}";
        } catch (Exception ex) {
            throw buildInternalError("Failed to render release notes index", ex);
        }
    }

    @McpResource(
            name = "releaseNotesByVersion",
            title = "Release notes by version",
            uri = "relnote://{version}",
            description = "Markdown content for a given release version",
            mimeType = "text/markdown")
    public String releaseNotesByVersion(String version) {
        if (version == null || !VERSION_PATTERN.matcher(version).matches()) {
            throw McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
                    .message("Version must match YYYY.<quarter>.<count> (e.g., 2026.0.5)")
                    .data(Map.of("version", version))
                    .build();
        }

        Resource resource = resolver.getResource(RELEASE_NOTES_PREFIX + version + ".md");
        if (!resource.exists()) {
            throw McpError.builder(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND)
                    .message("Release notes not found for version " + version)
                    .data(Map.of("uri", "relnote://" + version))
                    .build();
        }

        try (InputStream inputStream = resource.getInputStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw buildInternalError("Failed to read release notes for " + version, ex);
        }
    }

    private List<String> loadAvailableVersions() {
        try {
            Resource[] resources = resolver.getResources(RELEASE_NOTES_GLOB);
            List<String> versions = new ArrayList<>();
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !filename.endsWith(".md")) {
                    continue;
                }
                String version = filename.substring(0, filename.length() - 3);
                if (VERSION_PATTERN.matcher(version).matches()) {
                    versions.add(version);
                } else {
                    logger.warn("Skipping release note with unexpected filename: {}", filename);
                }
            }
            Collections.sort(versions, Collections.reverseOrder());
            return versions;
        } catch (IOException ex) {
            throw buildInternalError("Failed to list available release notes", ex);
        }
    }

    private String toJsonArray(List<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < values.size(); i++) {
            builder.append("\"").append(values.get(i)).append("\"");
            if (i < values.size() - 1) {
                builder.append(",");
            }
        }
        builder.append("]");
        return builder.toString();
    }

    private McpError buildInternalError(String message, Exception ex) {
        logger.error(message, ex);
        return McpError.builder(McpSchema.ErrorCodes.INTERNAL_ERROR)
                .message(message)
                .data(Map.of("cause", ex.getMessage()))
                .build();
    }
}
