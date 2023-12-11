package org.systemli.keycloak;

import java.nio.charset.StandardCharsets;

import com.goterl.lazysodium.LazySodiumJava;
import com.goterl.lazysodium.SodiumJava;
import com.goterl.lazysodium.interfaces.PwHash;
import com.goterl.lazysodium.utils.LibraryLoader;

public class PasswordVerifier {

    private PwHash.Native hasher;

    public PasswordVerifier() {
        SodiumJava sodium = new SodiumJava(LibraryLoader.Mode.BUNDLED_ONLY);
        LazySodiumJava lazySodium = new LazySodiumJava(sodium);
        this.hasher = lazySodium;
    }

    public boolean verify(String password, String hash) {
        // See https://github.com/terl/lazysodium-java/issues/39#issuecomment-420614131
        byte[] hashBytes = (hash + "\0").getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        return hasher.cryptoPwHashStrVerify(hashBytes, passwordBytes, passwordBytes.length);
    }
}
