package com.pharoah.llm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads role system prompts from resources/prompts/<role>.txt with caching.
 */
public class PromptLoader {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    public static String loadPrompt(String role) {
        if (role == null || role.isBlank()) {
            return "You are a helpful, expert AI agent.";
        }

        String normalized = role.toLowerCase().trim();
        return CACHE.computeIfAbsent(normalized, PromptLoader::readFromDiskOrClasspath);
    }

    private static String readFromDiskOrClasspath(String role) {
        String filename = role + ".txt";

        // 1. Try classpath
        try (InputStream is = PromptLoader.class.getResourceAsStream("/prompts/" + filename)) {
            if (is != null) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {}

        // 2. Try resources/prompts on disk
        File f1 = new File("resources/prompts/" + filename);
        if (f1.exists()) {
            try (InputStream is = new FileInputStream(f1)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            } catch (Exception ignored) {}
        }

        // 3. Try src/main/resources/prompts on disk
        File f2 = new File("src/main/resources/prompts/" + filename);
        if (f2.exists()) {
            try (InputStream is = new FileInputStream(f2)) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
            } catch (Exception ignored) {}
        }

        return "You are the " + role + " agent in the PHAROAH Multi-Agent Orchestration Swarm. Perform your assigned task with academic rigor and clear structure.";
    }
}
