package com.oracle.cloud.baremetal.jenkins;

import org.junit.Assert;
import org.junit.Test;

import jenkins.bouncycastle.api.SecurityProviderInitializer;

public class SshKeyUtilUnitTest {
    static {
        new SecurityProviderInitializer();
    }

    private static final String TEST_RSA_PRIVATE_KEY_PEM =
            "-----BEGIN RSA PRIVATE KEY-----\n" +
            "MIIBywIBAAJhAKmEwj68Ssf3v5tkolZzwANvDs/PDGBSxC8A1FqsXQ+hrGa/j/JB\n" +
            "/R+xXPSvr/a1KWaPilXqhALt8+7LIfg4TbUxdhXVdVJupha7JwBUCBH87DFVQzc5\n" +
            "wqJJ7J6iIGZNNwIDAQABAmARLCi9UDfHIBrh9ATZ+ynVbzex54iabWgAVvYsJU/c\n" +
            "GIWtdvRvFy48OqxvASkzNdDMlI5QxpD92cfoykxFd/U4lPjcgKpInm7CkGVvJFtC\n" +
            "Qr2MG87iILNAuQWHwlljyuECMQDcJzo9u1+ue9wlcBUjhtfo7nCKxoEg9xsTRIWn\n" +
            "JnrWiGw6oyy/AIKxw/pSxN1d3DECMQDFHuYKp3VKMo4kz+J2XdXeYKg/iZ8ebeNu\n" +
            "id8tiTtiUtgoAA5znMwM5JAhh7EALecCMQCjunjSGFwcg/lBzo2qEkrY7Ru92cuH\n" +
            "HL+CIN/VZATPMD5tjZVlp5eLZVjx3X9UosECMQCjf/CBD8r6gxphsEh/s29MZ1HG\n" +
            "eckQfUcyjYsfAv/NmzeNXhaekISzgPWHyjvnESsCMD0vJJ8DsP01Zi4CGnN3Cw1t\n" +
            "D5T/rai8O9b0G4JOOXRTjv8v68ajYDotjolRwTCULw==\n" +
            "-----END RSA PRIVATE KEY-----\n";
    private static final String TEST_RSA_PUBLIC_KEY_SSH =
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAYQCphMI+vErH97+bZKJWc8ADbw7PzwxgUsQvANRarF0Poaxmv4/yQf0fsVz0r6/2tSlmj4pV6oQC7fPuyyH4OE21MXYV1XVSbqYWuycAVAgR/OwxVUM3OcKiSeyeoiBmTTc=";

    private static final String TEST_ED25519_OPENSSH_PRIVATE_KEY =
            "-----BEGIN OPENSSH PRIVATE KEY-----\n" +
            "b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW\n" +
            "QyNTUxOQAAACAOFo/mArf3l4rtJjbIT5occ3ZvItls9/tclbrLZAmI6gAAAJhPtEloT7RJ\n" +
            "aAAAAAtzc2gtZWQyNTUxOQAAACAOFo/mArf3l4rtJjbIT5occ3ZvItls9/tclbrLZAmI6g\n" +
            "AAAED0V3Tdcb1/Txr7OfngauamtYwWA1dXcOZ7/5wskKSpNg4Wj+YCt/eXiu0mNshPmhxz\n" +
            "dm8i2Wz3+1yVustkCYjqAAAAFUNTT0xBTktJQENTT0xBTktJLW1hYw==\n" +
            "-----END OPENSSH PRIVATE KEY-----\n";
    private static final String TEST_ED25519_PUBLIC_KEY_SSH =
            "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIA4Wj+YCt/eXiu0mNshPmhxzdm8i2Wz3+1yVustkCYjq";

    private static final String TEST_ECDSA_PKCS8_PRIVATE_KEY =
            "-----BEGIN PRIVATE KEY-----\n" +
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgGWrpclaalY9ambPr\n" +
            "PZa64t0RcI5tYbC62IPifbjVHv6hRANCAATsYCi7D1sTN850vz8qYG4hcG+y4k3q\n" +
            "7qMgRYPEe19n3MDXIZO5G7s6nP7YchNYKpcitwJM3sJ9S9dbuM/wBRdk\n" +
            "-----END PRIVATE KEY-----\n";
    private static final String TEST_ECDSA_PUBLIC_KEY_SSH =
            "ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBOxgKLsPWxM3znS/PypgbiFwb7LiTeruoyBFg8R7X2fcwNchk7kbuzqc/thyE1gqlyK3Akzewn1L11u4z/AFF2Q=";

    @Test
    public void testGetPublicKeySupportsLegacyRsaPem() throws Exception {
        Assert.assertEquals(TEST_RSA_PUBLIC_KEY_SSH, SshKeyUtil.getPublicKey(TEST_RSA_PRIVATE_KEY_PEM, null));
    }

    @Test
    public void testGetPublicKeySupportsOpenSshEd25519() throws Exception {
        Assert.assertEquals(TEST_ED25519_PUBLIC_KEY_SSH, SshKeyUtil.getPublicKey(TEST_ED25519_OPENSSH_PRIVATE_KEY, null));
    }

    @Test
    public void testGetPublicKeySupportsPkcs8Ecdsa() throws Exception {
        Assert.assertEquals(TEST_ECDSA_PUBLIC_KEY_SSH, SshKeyUtil.getPublicKey(TEST_ECDSA_PKCS8_PRIVATE_KEY, null));
    }
}
