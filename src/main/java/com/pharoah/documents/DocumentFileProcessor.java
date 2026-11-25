package com.pharoah.documents;

import com.pharoah.core.model.Task;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * File I/O Engine for PHAROAH.
 * 
 * Demonstrates:
 * 1. Byte Streams: FileInputStream and FileOutputStream for binary file replication.
 * 2. Character Streams: FileReader and FileWriter with BufferedReader and BufferedWriter.
 * 3. StreamTokenizer: Parsing words, numbers, and tokens from structured text and CSV.
 * 4. Exception-Safe resource management with try-with-resources.
 */
public class DocumentFileProcessor {

    /**
     * Writes textual report to disk using BufferedWriter and FileWriter.
     */
    public void writeReport(String filePath, String content) throws IOException {
        File file = new File(filePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
            writer.flush();
        }
    }

    /**
     * Reads line-by-line using BufferedReader and FileReader.
     */
    public String readTextFile(String filePath) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString().trim();
    }

    /**
     * Replicates binary files using FileInputStream and FileOutputStream.
     */
    public void copyBinaryFile(File source, File destination) throws IOException {
        if (!source.exists()) {
            throw new FileNotFoundException("Source file not found: " + source.getAbsolutePath());
        }

        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(destination)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
        }
    }

    /**
     * Demonstrates StreamTokenizer: Tokenizes words, numbers, and line endings.
     */
    public List<String> tokenizeWords(String filePath) throws IOException {
        List<String> tokens = new ArrayList<>();

        try (FileReader fr = new FileReader(filePath)) {
            StreamTokenizer tokenizer = new StreamTokenizer(fr);
            tokenizer.eolIsSignificant(false); // Ignore newline tokens

            int tokenType;
            while ((tokenType = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
                if (tokenType == StreamTokenizer.TT_WORD) {
                    tokens.add("WORD:" + tokenizer.sval);
                } else if (tokenType == StreamTokenizer.TT_NUMBER) {
                    tokens.add("NUM:" + tokenizer.nval);
                }
            }
        }
        return tokens;
    }

    /**
     * Exports a collection of tasks to a standard CSV file.
     */
    public void exportTasksToCsv(List<Task> tasks, String csvFilePath) throws IOException {
        File file = new File(csvFilePath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("task_id,title,type,priority,status,subject_id");
            writer.newLine();

            for (Task t : tasks) {
                // Escape commas in title
                String cleanTitle = "\"" + t.getTitle().replace("\"", "\"\"") + "\"";
                writer.write(String.format("%s,%s,%s,%s,%s,%s",
                        t.getTaskId(), cleanTitle, t.getType(), t.getPriority(), t.getStatus(), t.getSubjectId()));
                writer.newLine();
            }
            writer.flush();
        }
    }
}
