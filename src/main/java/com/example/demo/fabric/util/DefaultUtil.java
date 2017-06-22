package com.example.demo.fabric.util;


import com.example.demo.fabric.sdk.testutils.TestConfig;
import com.example.demo.fabric.sdkintegration.SampleOrg;
import com.example.demo.fabric.sdkintegration.SampleStore;
import com.example.demo.fabric.sdkintegration.SampleUser;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;

import static java.lang.String.format;

public class DefaultUtil {

    private static final TestConfig TEST_CONFIG = TestConfig.getConfig();
    private static final HFClient client = HFClient.createNewInstance();
    private static Channel channel = null;

    private static final ChaincodeID chaincodeID = ChaincodeID.newBuilder()
            .setName("example_cc_go")
            .setVersion("1")
            .setPath("github.com/example_cc")
            .build();

    static {
        try {
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            Collection<SampleOrg> testSampleOrgs = TEST_CONFIG.getIntegrationTestsSampleOrgs();
            SampleOrg sampleOrg = testSampleOrgs.iterator().next();
            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();
            File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
            if (sampleStoreFile.exists()) { //For testing start fresh
                sampleStoreFile.delete();
            }
            final SampleStore sampleStore = new SampleStore(sampleStoreFile);
            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                    findSKFile(Paths.get(TEST_CONFIG.getTestChannlePath(), "crypto-config/peerOrganizations/",
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(TEST_CONFIG.getTestChannlePath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
            client.setUserContext(peerOrgAdmin);

            channel = client.newChannel("foo");

            String peerName = sampleOrg.getPeerNames().iterator().next();
            String peerLocation = sampleOrg.getPeerLocation(peerName);
            Peer peer = client.newPeer(peerName, peerLocation, null);

            String orderName = sampleOrg.getOrdererNames().iterator().next();
            Orderer anOrderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    TEST_CONFIG.getOrdererProperties(orderName));
            channel.addOrderer(anOrderer);
            channel.addPeer(peer);
            channel.initialize();
            sampleOrg.addPeer(peer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static File findSKFile(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];
    }

    public static HFClient getHFClient() {
        return client;
    }

    public static ChaincodeID getChaincodeID() {
        return chaincodeID;
    }

    public static Channel getChannel() {
        return channel;
    }
}
