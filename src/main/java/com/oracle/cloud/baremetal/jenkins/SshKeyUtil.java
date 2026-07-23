package com.oracle.cloud.baremetal.jenkins;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.NotImplementedException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed448PrivateKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;

public class SshKeyUtil {
    private static final String OPENSSH_PRIVATE_KEY_BEGIN = "-----BEGIN OPENSSH PRIVATE KEY-----";
    private static final byte[] OPENSSH_AUTH_MAGIC = "openssh-key-v1\0".getBytes(StandardCharsets.UTF_8);
    private static final Provider BC_PROVIDER = new BouncyCastleProvider();

    public static String getPublicKey(String privateSshKey, String privateSshKeyPassphrase)
            throws IOException, NotImplementedException {
        AsymmetricKeyParameter privateKey = parsePrivateKey(privateSshKey, privateSshKeyPassphrase);
        AsymmetricKeyParameter publicKey = derivePublicKey(privateKey);
        byte[] encodedPublicKey = OpenSSHPublicKeyUtil.encodePublicKey(publicKey);
        return readSshString(encodedPublicKey) + " " + Base64.encodeBase64String(encodedPublicKey);
    }

    private static AsymmetricKeyParameter parsePrivateKey(String privateSshKey, String privateSshKeyPassphrase)
            throws IOException {
        ensureBouncyCastleProvider();

        if (privateSshKey == null) {
            throw new IOException("SSH private key must not be null");
        }

        if (privateSshKey.contains(OPENSSH_PRIVATE_KEY_BEGIN)) {
            byte[] keyBlob = decodePemBody(privateSshKey);
            if (isEncryptedOpenSshPrivateKey(keyBlob)) {
                throw new IOException("Encrypted OpenSSH private keys are not supported");
            }
            return OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(keyBlob);
        }

        try {
            return parsePemPrivateKey(privateSshKey, privateSshKeyPassphrase);
        } catch (OperatorCreationException | PKCSException e) {
            throw new IOException("Failed to decrypt SSH private key", e);
        }
    }

    private static AsymmetricKeyParameter parsePemPrivateKey(String privateSshKey, String privateSshKeyPassphrase)
            throws IOException, OperatorCreationException, PKCSException {
        try (PEMParser parser = new PEMParser(new StringReader(privateSshKey))) {
            Object keyObj = parser.readObject();
            if (keyObj instanceof PEMKeyPair) {
                return PrivateKeyFactory.createKey(((PEMKeyPair) keyObj).getPrivateKeyInfo());
            }
            if (keyObj instanceof PEMEncryptedKeyPair) {
                PEMDecryptorProvider decryptionProvider = new JcePEMDecryptorProviderBuilder()
                        .setProvider(BC_PROVIDER)
                        .build(toPassphraseChars(privateSshKeyPassphrase));
                PEMKeyPair keyPair = ((PEMEncryptedKeyPair) keyObj).decryptKeyPair(decryptionProvider);
                return PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo());
            }
            if (keyObj instanceof PrivateKeyInfo) {
                return PrivateKeyFactory.createKey((PrivateKeyInfo) keyObj);
            }
            if (keyObj instanceof PKCS8EncryptedPrivateKeyInfo) {
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .setProvider(BC_PROVIDER)
                        .build(toPassphraseChars(privateSshKeyPassphrase));
                PrivateKeyInfo privateKeyInfo =
                        ((PKCS8EncryptedPrivateKeyInfo) keyObj).decryptPrivateKeyInfo(decryptorProvider);
                return PrivateKeyFactory.createKey(privateKeyInfo);
            }
            throw new IOException(
                    "Unsupported SSH private key format: " + (keyObj == null ? "null" : keyObj.getClass().getName()));
        }
    }

    private static AsymmetricKeyParameter derivePublicKey(AsymmetricKeyParameter privateKey) {
        if (privateKey instanceof RSAPrivateCrtKeyParameters) {
            RSAPrivateCrtKeyParameters rsaKey = (RSAPrivateCrtKeyParameters) privateKey;
            return new RSAKeyParameters(false, rsaKey.getModulus(), rsaKey.getPublicExponent());
        }
        if (privateKey instanceof DSAPrivateKeyParameters) {
            DSAPrivateKeyParameters dsaKey = (DSAPrivateKeyParameters) privateKey;
            return new DSAPublicKeyParameters(
                    dsaKey.getParameters().getG().modPow(dsaKey.getX(), dsaKey.getParameters().getP()),
                    dsaKey.getParameters());
        }
        if (privateKey instanceof ECPrivateKeyParameters) {
            ECPrivateKeyParameters ecKey = (ECPrivateKeyParameters) privateKey;
            return new ECPublicKeyParameters(
                    ecKey.getParameters().getG().multiply(ecKey.getD()).normalize(),
                    ecKey.getParameters());
        }
        if (privateKey instanceof Ed25519PrivateKeyParameters) {
            return ((Ed25519PrivateKeyParameters) privateKey).generatePublicKey();
        }
        if (privateKey instanceof Ed448PrivateKeyParameters) {
            return ((Ed448PrivateKeyParameters) privateKey).generatePublicKey();
        }
        throw new NotImplementedException("Unsupported SSH private key algorithm: " + privateKey.getClass().getName());
    }

    private static void ensureBouncyCastleProvider() {
        if (Security.getProvider(BC_PROVIDER.getName()) == null) {
            Security.addProvider(BC_PROVIDER);
        }
    }

    private static char[] toPassphraseChars(String privateSshKeyPassphrase) {
        return privateSshKeyPassphrase == null ? new char[0] : privateSshKeyPassphrase.toCharArray();
    }

    private static byte[] decodePemBody(String pemText) {
        StringBuilder body = new StringBuilder();
        for (String line : pemText.split("\\R")) {
            if (line.startsWith("-----BEGIN ") || line.startsWith("-----END ")) {
                continue;
            }
            body.append(line.trim());
        }
        return java.util.Base64.getMimeDecoder().decode(body.toString());
    }

    private static boolean isEncryptedOpenSshPrivateKey(byte[] keyBlob) throws IOException {
        if (keyBlob.length < OPENSSH_AUTH_MAGIC.length
                || !Arrays.equals(OPENSSH_AUTH_MAGIC, Arrays.copyOf(keyBlob, OPENSSH_AUTH_MAGIC.length))) {
            throw new IOException("Invalid OpenSSH private key blob");
        }

        return !"none".equals(readSshString(keyBlob, OPENSSH_AUTH_MAGIC.length));
    }

    private static String readSshString(byte[] keyBlob) throws IOException {
        return readSshString(keyBlob, 0);
    }

    private static String readSshString(byte[] keyBlob, int offset) throws IOException {
        if (offset + 4 > keyBlob.length) {
            throw new IOException("Invalid SSH key payload");
        }

        int length = ((keyBlob[offset] & 0xff) << 24)
                | ((keyBlob[offset + 1] & 0xff) << 16)
                | ((keyBlob[offset + 2] & 0xff) << 8)
                | (keyBlob[offset + 3] & 0xff);
        int start = offset + 4;
        int end = start + length;

        if (length < 0 || end > keyBlob.length) {
            throw new IOException("Invalid SSH key payload");
        }

        return new String(keyBlob, start, length, StandardCharsets.UTF_8);
    }
}
