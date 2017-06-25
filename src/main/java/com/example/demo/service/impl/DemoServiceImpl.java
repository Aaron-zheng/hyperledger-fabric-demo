package com.example.demo.service.impl;


import com.example.demo.fabric.sdk.testutils.TestConfig;
import com.example.demo.fabric.sdkintegration.SampleOrg;
import com.example.demo.fabric.sdkintegration.SampleStore;
import com.example.demo.fabric.sdkintegration.SampleUser;
import com.example.demo.service.DemoService;

import org.hyperledger.fabric.protos.peer.FabricTransaction;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class DemoServiceImpl implements DemoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoServiceImpl.class);

    private static final TestConfig testConfig = TestConfig.getConfig();
    private static SampleOrg sampleOrg;
    private static HFClient client = HFClient.createNewInstance();
    private static SampleStore sampleStore;
    private static HFCAClient ca;
    private static SampleUser admin;
    private static Channel channel;
    private static ChaincodeID chaincodeID;
    private static Orderer orderer;
    private static Peer peer;

    static {
        LOGGER.info("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));

        try {
            //////////////////////////////////////////////////////初始化，保存文件
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
            sampleStore = new SampleStore(sampleStoreFile);
            //sampleOrg的名称为peerOrg1
            sampleOrg = testConfig.getIntegrationTestsSampleOrgs().iterator().next();
            sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
            ca = sampleOrg.getCAClient();
            chaincodeID = ChaincodeID.newBuilder()
                    .setName("demo_cc_go")
                    .setVersion("1")
                    .setPath("github.com/demo_cc")
                    .build();
            //创建order这里，并没有发送到fabric
            String orderName = sampleOrg.getOrdererNames().iterator().next();
            orderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName));


            //////////////////////////////////////////////////////设置peerOrgAdmin
            String sampleOrgName = sampleOrg.getName();
            String sampleOrgDomainName =sampleOrg.getDomainName();
            //参数为name, org, MSPID, privateKeyFile,certificateFile
            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                    findFile_sk(Paths.get(testConfig.getTestChannlePath(), "crypto-config/peerOrganizations/",
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(testConfig.getTestChannlePath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
            //该peerOrgAdmin是用来创建channel，添加peer，和安装chaincode
            sampleOrg.setPeerAdmin(peerOrgAdmin);
            //创建peer
            String peerName = sampleOrg.getPeerNames().iterator().next();
            String peerLocation = sampleOrg.getPeerLocation(peerName);
            //创建peer这步不会连接到fabric
            peer = client.newPeer(peerName, peerLocation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String start() {
        String result = "";
        try {
            createChannel();
            peerJoinChannel();
            initialChannel();
            installChaincode();
            instantiateChaincode();
            result = "启动成功";
        } catch (Exception ex) {
            result = "启动失败";
            LOGGER.error(ex.getMessage(), ex);
        }

        return result;

    }

    @Override
    public String transfer() {
        String result = "";
        try {
            transferChaincode();
            result = "转账成功";
        } catch (Exception ex) {
            result = "转账失败";
            LOGGER.error(ex.getMessage(), ex);
        }

        return result;
    }

    @Override
    public String query() {
        try {
            return queryChaincode();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }

        return "查询失败";
    }


    public static void main(String[] args) throws Exception{
        //////////////////////////////////////////////////////注册ca（可以多次注册，每次生成的证书和密钥对都不一样)
//        enrollAdmin();
        //////////////////////////////////////////////////////会员注册（同一个用户名，不可以重复注册）
//        enrollMember();
        //////////////////////////////////////////////////////创建channel（不可以重复创建）
//        createChannel();
        //////////////////////////////////////////////////////把peer加入到channel（不可以重复加入）
//        peerJoinChannel();
        //////////////////////////////////////////////////////初始化channel
//        initialChannel();
        //////////////////////////////////////////////////////安装chaincode
//        installChaincode();
        //////////////////////////////////////////////////////实例化chaincode
//        instantiateChaincode();
        //////////////////////////////////////////////////////查询结果
//        Thread.sleep(5000);
//        queryChaincode();
        //////////////////////////////////////////////////////转发结果
//        transferChaincode();
    }


    private static void enrollAdmin() throws Exception {
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        admin = sampleStore.getMember("admin", sampleOrg.getName());
        //enrollment这步会在，本地生成keypair和csr，然后发送到fabric-ca进行签名（enrollment会包含，私钥和数字证书）
        Enrollment adminEnrollment = ca.enroll(admin.getName(), "adminpw");
        admin.setEnrollment(adminEnrollment);
        admin.setMPSID(sampleOrg.getMSPID());
        //该机构的admin
        sampleOrg.setAdmin(admin);
    }

    private static void enrollMember() throws Exception{
        SampleUser user = sampleStore.getMember("user1", sampleOrg.getName());
        //affiliation
        RegistrationRequest registrationRequest = new RegistrationRequest(user.getName(), "org1.department1");
        //发送用户名+部门名，到fabric-ca进行注册，返回值只有一个密码
        String enrollmentSecret = ca.register(registrationRequest, admin);
        user.setEnrollmentSecret(enrollmentSecret);
        //获取enrollment
        //如果重复注册，则会返回 {"success":false,"result":null,"errors":[{"code":0,"message":"Identity 'user1' is already registered"}],"messages":[]}
        Enrollment userEnrollment = ca.enroll(user.getName(), user.getEnrollmentSecret());
        user.setEnrollment(userEnrollment);
        user.setMPSID(sampleOrg.getMSPID());
        sampleOrg.addUser(user);
    }


    private static void createChannel() throws Exception {
        //userContext不能为空
        client.setUserContext(sampleOrg.getPeerAdmin());
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/e2e-2Orgs/channel/foo.tx"));
        //创建channel，是需要连接到fabric
        //参数channelName, orderer, channelConfiguration, channelConfigurationSignatures
        //如果重复创建，会返回New channel foo error. StatusValue 400. Status BAD_REQUEST
        channel = client.newChannel("foo", orderer, channelConfiguration,
                client.getChannelConfigurationSignature(channelConfiguration, sampleOrg.getPeerAdmin())
        );
        //channel的创建，其实是通过orderer.sendTransaction实现的
        //会创建创世区块getGenesisBlock，并且需要添加共识节点orderer
    }

    private static void peerJoinChannel() throws Exception{
        //channel添加peer节点，会连接到fabric
        //重复添加的话，会提示status: 500, message: Cannot create ledger from genesis block, due to LedgerID already exists
        channel.joinPeer(peer);
        //通过Envelope发去orderer来创建创世块
        sampleOrg.addPeer(peer);
    }

    private static void initialChannel() throws Exception {
        //Starts the channel. event hubs will connect
        channel.initialize();
    }

    private static void installChaincode() throws Exception {
        Collection<ProposalResponse> responses;
        client.setUserContext(sampleOrg.getPeerAdmin());
        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chaincodeID);
        installProposalRequest.setChaincodeSourceLocation(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/gocc/sample1"));
        installProposalRequest.setChaincodeVersion("1");
        responses = client.sendInstallProposal(installProposalRequest, sampleOrg.getPeers());

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        for(ProposalResponse proposalResponse : responses) {
            if(proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(proposalResponse);
            } else {
                failed.add(proposalResponse);
            }
        }
    }

    private static void instantiateChaincode() throws Exception{
        Collection<ProposalResponse> responses;
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(testConfig.getProposalWaitTime());
        instantiateProposalRequest.setChaincodeID(chaincodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(new String[] {"a", "500", "b", "200"});
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/chaincodeendorsementpolicy.yaml"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);
        //发送实例化请求到fabric
        responses = channel.sendInstantiationProposal(instantiateProposalRequest);

        //////////////////////////////////////////////////////发送结果到orderer
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        for(ProposalResponse proposalResponse : responses) {
            if(proposalResponse.isVerified() && proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(proposalResponse);
            } else {
                failed.add(proposalResponse);
            }
        }
        channel.sendTransaction(successful, channel.getOrderers());
    }

    private static void check() throws Exception {
        if(null == client.getUserContext()) {
            client.setUserContext(sampleOrg.getPeerAdmin());
        }
        if(null == channel) {
            channel = client.newChannel("foo");
        }
        if(channel.getOrderers().isEmpty()) {
            channel.addOrderer(orderer);
        }
        if(channel.getPeers().isEmpty()) {
            channel.addPeer(peer);
        }
        if(!channel.isInitialized()) {
            channel.initialize();
        }

    }


    private static String queryChaincode() throws Exception {
        check();

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[] {"query", "b"});
        queryByChaincodeRequest.setFcn("invoke");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
        queryByChaincodeRequest.setTransientMap(tm2);

        String result = "";
        Collection<ProposalResponse> responses = channel.queryByChaincode(queryByChaincodeRequest);
        for(ProposalResponse proposalResponse: responses) {
            if(proposalResponse.isVerified() && proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                LOGGER.info("payload: " + payload);
                if(result.length() > 0) {
                    result += ",";
                }
                result += payload;
            }
        }
        return result;
    }

    private static void transferChaincode() throws Exception {
        check();

        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn("invoke");
        transactionProposalRequest.setArgs(new String[]{"move", "a", "b", "100"});

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
        transactionProposalRequest.setTransientMap(tm2);


        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<ProposalResponse> responses = channel.sendTransactionProposal(transactionProposalRequest);
        for(ProposalResponse proposalResponse : responses) {
            if(proposalResponse.isVerified() && proposalResponse.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(proposalResponse);
            } else {
                failed.add(proposalResponse);
            }
        }

        channel.sendTransaction(successful);


    }




    private static File findFile_sk(File directory) {

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
