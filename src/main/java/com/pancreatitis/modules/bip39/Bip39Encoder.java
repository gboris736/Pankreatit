package com.pancreatitis.modules.bip39;

/**
 * Утилита для генерации BIP39-мнемоники из зашифрованного ключа.
 * Может использоваться в приложении администратора/регистрации.
 */
public class Bip39Encoder {
    /**
     * Превращает массив байтов (ровно 16 байт) в 12-словную BIP39 фразу.
     *
     * @param encryptedKey зашифрованный ключ пользователя (encrypt_user_key)
     * @return мнемоническая фраза из 12 английских слов, разделённых пробелами
     * @throws IllegalArgumentException если длина массива не равна 16 байтам
     */
    public static String toMnemonic(byte[] encryptedKey) {
        return Bip39Utils.generateMnemonic(encryptedKey);
    }
}