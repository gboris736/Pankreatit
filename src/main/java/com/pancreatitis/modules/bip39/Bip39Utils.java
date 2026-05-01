package com.pancreatitis.modules.bip39;

import com.pancreatitis.modules.localstorage.DiskStorageControl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Внутренний класс для общих операций BIP39.
 * Не предназначен для использования извне этого пакета.
 */
class Bip39Utils {
    private static final int ENTROPY_BITS = 256;
    private static final int CHECKSUM_BITS = ENTROPY_BITS / 32;
    private static final List<String> WORD_LIST = loadWordList();

    private static List<String> loadWordList() {
        List<String> words = new ArrayList<>(2048);
        try {
            // Получаем корневую папку приложения
            Path appDir = DiskStorageControl.getInstance().getAppDir();
            Path wordListFile = appDir.resolve("wordlist_en.txt");

            if (!Files.exists(wordListFile)) {
                throw new RuntimeException("BIP39 word list file not found: " + wordListFile);
            }

            try (BufferedReader reader = Files.newBufferedReader(wordListFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim();
                    if (!word.isEmpty()) words.add(word);
                }
            }
            if (words.size() != 2048) {
                throw new RuntimeException("Invalid BIP39 word list size: " + words.size() + ", expected 2048");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load BIP39 word list", e);
        }
        return words;
    }

    /**
     * Генерирует 12-словную мнемоническую фразу из 16-байтового массива (энтропии).
     */
    static String generateMnemonic(byte[] entropy) {
        if (entropy.length != ENTROPY_BITS / 8)
            throw new IllegalArgumentException("Entropy must be 32 bytes (256 bits)");

        // Вычисление контрольной суммы (первые CHECKSUM_BITS бит SHA-256)
        byte[] hash = sha256(entropy);
        int checksum = (hash[0] & 0xFF) >> (8 - CHECKSUM_BITS); // только старшие CHECKSUM_BITS бит

        // Конвертация энтропии + контрольной суммы в битовую строку
        StringBuilder binaryString = new StringBuilder();
        for (byte b : entropy) {
            binaryString.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        binaryString.append(String.format("%8s", Integer.toBinaryString(checksum)).replace(' ', '0')
                .substring(0, CHECKSUM_BITS));

        // Разбиение на группы по 11 бит и подбор слов
        StringBuilder mnemonic = new StringBuilder();
        for (int i = 0; i < binaryString.length(); i += 11) {
            String chunk = binaryString.substring(i, Math.min(i + 11, binaryString.length()));
            int index = Integer.parseInt(chunk, 2);
            if (i > 0) mnemonic.append(' ');
            mnemonic.append(WORD_LIST.get(index));
        }
        return mnemonic.toString();
    }

    /**
     * Восстанавливает 16-байтовую энтропию из мнемонической фразы.
     * Выбрасывает IllegalArgumentException, если фраза некорректна.
     */
    static byte[] recoverEntropy(String mnemonic) {
        String[] words = mnemonic.trim().split("\\s+");
        if (words.length != 24)
            throw new IllegalArgumentException("Invalid mnemonic: must be 24 words");

        StringBuilder binaryString = new StringBuilder();
        for (String word : words) {
            int index = WORD_LIST.indexOf(word);
            if (index == -1)
                throw new IllegalArgumentException("Unknown word in mnemonic: " + word);
            binaryString.append(String.format("%11s", Integer.toBinaryString(index)).replace(' ', '0'));
        }

        // Отделяем энтропию и контрольную сумму
        String entropyBits = binaryString.substring(0, ENTROPY_BITS);
        String checksumBits = binaryString.substring(ENTROPY_BITS, ENTROPY_BITS + CHECKSUM_BITS);

        // Преобразуем битовую строку в байты
        byte[] entropy = new byte[ENTROPY_BITS / 8];
        for (int i = 0; i < ENTROPY_BITS; i += 8) {
            entropy[i / 8] = (byte) Integer.parseInt(entropyBits.substring(i, i + 8), 2);
        }

        // Проверка контрольной суммы
        byte[] hash = sha256(entropy);
        int expectedChecksum = (hash[0] & 0xFF) >> (8 - CHECKSUM_BITS);
        int actualChecksum = Integer.parseInt(checksumBits, 2);
        if (expectedChecksum != actualChecksum) {
            throw new IllegalArgumentException("Invalid checksum in mnemonic");
        }

        return entropy;
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}