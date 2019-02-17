package net.gjerull.etherpad.client;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class StringGenerator extends Generator<String> {

    private static final String[] SENTENCES = {
        "Here is my BTC address!",
        "Your token is",
        "Try with this credentials...",
        "Keep this password:",
        "I've found the hash,"
    };
    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghiklmnnopqrstuvwxyz0123456789";
    private static final int SIZE = 100;

    public StringGenerator() {
        super(String.class);
    }

    public String generate(SourceOfRandomness random, GenerationStatus status) {
        StringBuilder sb = new StringBuilder(SIZE);
        sb.append(SENTENCES[random.nextInt(SENTENCES.length)] + " ");
        for (int i = 0; i < SIZE - sb.length(); i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
