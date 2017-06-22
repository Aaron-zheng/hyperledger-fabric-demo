package com.example.demo.service.impl;


import com.example.demo.fabric.sdk.testutils.TestConfig;
import com.example.demo.fabric.sdkintegration.SampleOrg;
import com.example.demo.fabric.sdkintegration.SampleStore;
import com.example.demo.fabric.sdkintegration.SampleUser;
import com.example.demo.service.DemoService;

import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

@Service
public class DemoServiceImpl implements DemoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoServiceImpl.class);

    private static final TestConfig testConfig = TestConfig.getConfig();
    private static Collection<SampleOrg> testSampleOrgs;
    private static SampleOrg sampleOrg;

    @Override
    public void start() {

    }


    public static void main(String[] args) throws Exception{

        //////////////////////////////////////////////////////初始化，保存文件
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        LOGGER.info("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));
        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        SampleStore sampleStore = new SampleStore(sampleStoreFile);



        //////////////////////////////////////////////////////获取组织
        //sampleOrg的名称为peerOrg1
        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
        sampleOrg = testSampleOrgs.iterator().next();
        sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));


        //////////////////////////////////////////////////////注册ca（可以多次注册)
        HFCAClient ca = sampleOrg.getCAClient();
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleUser admin = sampleStore.getMember("admin", sampleOrg.getName());
        //enrollment这步会在，本地生成keypair和csr，然后发送到fabric-ca进行签名（enrollment会包含，私钥和数字证书）
        Enrollment adminEnrollment = ca.enroll(admin.getName(), "adminpw");
        admin.setEnrollment(adminEnrollment);
        admin.setMPSID(sampleOrg.getMSPID());
        //该机构的admin
        sampleOrg.setAdmin(admin);


        //////////////////////////////////////////////////////会员注册（同一个用户名，不可以重复注册）
        SampleUser user = sampleStore.getMember("user1", sampleOrg.getName());
        //affiliation
        RegistrationRequest registrationRequest = new RegistrationRequest(user.getName(), "org1.department1");
        //发送用户名+部门名，到fabric-ca进行注册，返回值只有一个密码
        String enrollmentSecret = ca.register(registrationRequest, admin);
        user.setEnrollmentSecret(enrollmentSecret);
        //获取enrollment
        Enrollment userEnrollment = ca.enroll(user.getName(), user.getEnrollmentSecret());
        user.setEnrollment(userEnrollment);
        user.setMPSID(sampleOrg.getMSPID());
        sampleOrg.addUser(user);


        //////////////////////////////////////////////////////设置peerOrgAdmin
        String sampleOrgName = sampleOrg.getName();
        String sampleOrgDomainName =sampleOrg.getDomainName();
        //参数为name, org, MSPID, privateKeyFile,certificateFile
        SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                findFile_sk(Paths.get(testConfig.getTestChannlePath(), "crypto-config/peerOrganizations/",
                        sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                Paths.get(testConfig.getTestChannlePath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                        format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());
        //该peerOrgAdmin是用来创建channel，添加peer，和安装chaincode的
        sampleOrg.setPeerAdmin(peerOrgAdmin);


        //////////////////////////////////////////////////////创建orderer
        String orderName = sampleOrg.getOrdererNames().iterator().next();
        Properties ordererProperties = testConfig.getOrdererProperties(orderName);
        //设置keepAlive
        ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[] {5L, TimeUnit.MINUTES});
        ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[] {8L, TimeUnit.SECONDS});
        Orderer orderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName), ordererProperties);


        //////////////////////////////////////////////////////创建channel
        client.setUserContext(sampleOrg.getPeerAdmin());
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File("src/main/java/com/example/demo/fabric/fixture/sdkintegration/e2e-2Orgs/channel/foo.tx"));
        //channelName, orderer, channelConfiguration, channelConfigurationSignatures
        Channel fooChannel = client.newChannel("foo", orderer, channelConfiguration,
                client.getChannelConfigurationSignature(channelConfiguration, sampleOrg.getPeerAdmin())
                );


        //////////////////////////////////////////////////////创建peer
        String peerName = sampleOrg.getPeerNames().iterator().next();
        String peerLocation = sampleOrg.getPeerLocation(peerName);
        Properties peerProperties = new Properties();
        //举例说明，设置一些特别的参数
        peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);
        Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
        fooChannel.joinPeer(peer);
        sampleOrg.addPeer(peer);







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
