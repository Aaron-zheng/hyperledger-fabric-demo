package com.example.demo.fabric.util;


import com.example.demo.fabric.sdk.testutils.TestConfig;
import com.example.demo.fabric.sdkintegration.SampleOrg;
import com.example.demo.fabric.sdkintegration.SampleStore;
import com.example.demo.fabric.sdkintegration.SampleUser;
import org.hyperledger.fabric.sdk.*;
import org.junit.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class StartupScenario {

    private static final TestConfig TEST_CONFIG = TestConfig.getConfig();

    private HFClient client = DefaultUtil.getHFClient();
    private ChaincodeID chaincodeID = DefaultUtil.getChaincodeID();



    @Test
    public void test() throws Exception {

        ////////////////////////////////////////////////////////初始化数据
        Collection<SampleOrg> testSampleOrgs = TEST_CONFIG.getIntegrationTestsSampleOrgs();
        Collection<ProposalResponse> proposalResponses = new LinkedList<>();

        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        if (sampleStoreFile.exists()) { //For testing start fresh
            sampleStoreFile.delete();
        }
        final SampleStore sampleStore = new SampleStore(sampleStoreFile);


        ////////////////////////////////////////////////////////设置peer的admin
        SampleOrg sampleOrg = testSampleOrgs.iterator().next();
        final String sampleOrgName = sampleOrg.getName();
        final String sampleOrgDomainName = sampleOrg.getDomainName();
        SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                findSKFile(Paths.get(TEST_CONFIG.getTestChannlePath(), "crypto-config/peerOrganizations/",
                        sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                Paths.get(TEST_CONFIG.getTestChannlePath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                        format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
        client.setUserContext(peerOrgAdmin);


        ////////////////////////////////////////////////////////channel的初始化
        String orderName = sampleOrg.getOrdererNames().iterator().next();
        Orderer anOrderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                TEST_CONFIG.getOrdererProperties(orderName));
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/e2e-2Orgs/channel/foo.tx"));
        Channel channel = client.newChannel("foo", anOrderer, channelConfiguration, client.getChannelConfigurationSignature(channelConfiguration, peerOrgAdmin));
        String eventHubName = sampleOrg.getEventHubNames().iterator().next();
        EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                TEST_CONFIG.getEventHubProperties(eventHubName));
        channel.addEventHub(eventHub);
        String peerName = sampleOrg.getPeerNames().iterator().next();
        String peerLocation = sampleOrg.getPeerLocation(peerName);
        Peer peer = client.newPeer(peerName, peerLocation, null);
        channel.joinPeer(peer);
        sampleOrg.addPeer(peer);
        channel.initialize();


        ////////////////////////////////////////////////////////安装chaincode
        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);
        installProposalRequest.setChaincodeSourceLocation(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/gocc/sample1"));
        installProposalRequest.setChaincodeVersion("1");
        Set<Peer> peersFromOrg = sampleOrg.getPeers();
        client.sendInstallProposal(installProposalRequest, peersFromOrg);


        ////////////////////////////////////////////////////////初始化chaincode，a有500，b有200
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(new String[]{"a", "500", "b", "200"});
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/chaincodeendorsementpolicy.yaml"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        proposalResponses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());


        ////////////////////////////////////////////////////////把proposal的回应发送到order，order会进行广播
        channel.sendTransaction(proposalResponses, channel.getOrderers());

    }


    private File findSKFile(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));
        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }
        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];
    }


}
