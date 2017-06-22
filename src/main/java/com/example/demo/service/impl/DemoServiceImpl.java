package com.example.demo.service.impl;


import com.example.demo.fabric.sdk.testutils.TestConfig;
import com.example.demo.fabric.sdkintegration.SampleOrg;
import com.example.demo.fabric.sdkintegration.SampleStore;
import com.example.demo.fabric.sdkintegration.SampleUser;
import com.example.demo.service.DemoService;

import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collection;

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

        //////////////////////////////////////////////////////
        HFClient client = HFClient.createNewInstance();
        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        LOGGER.info("java.io.tmpdir: " + System.getProperty("java.io.tmpdir"));
        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        SampleStore sampleStore = new SampleStore(sampleStoreFile);



        //////////////////////////////////////////////////////获取组织
        testSampleOrgs = testConfig.getIntegrationTestsSampleOrgs();
        sampleOrg = testSampleOrgs.iterator().next();
        sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));


        //////////////////////////////////////////////////////注册ca（可以多次注册)
        HFCAClient ca = sampleOrg.getCAClient();
        ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        SampleUser admin = sampleStore.getMember("admin", sampleOrg.getName());
        //enrollment这步会在，本地生成keypair和csr，然后发送到fabric-ca进行签名（enrollment会包含，私钥和数字证书）
        Enrollment enrollment = ca.enroll(admin.getName(), "adminpw");
        admin.setEnrollment(enrollment);
        admin.setMPSID(sampleOrg.getMSPID());
        //该机构的admin
        sampleOrg.setAdmin(admin);


        //////////////////////////////////////////////////////会员注册（不可以重复注册）
        SampleUser user = sampleStore.getMember("user3", sampleOrg.getName());
        //affiliation
        RegistrationRequest registrationRequest = new RegistrationRequest(user.getName(), "org1.department1");
        //发送用户名+部门名，到fabric-ca进行注册，返回值只有一个密码
        String enrollmentSecret = ca.register(registrationRequest, admin);
        user.setEnrollmentSecret(enrollmentSecret);
        sampleOrg.addUser(user);



    }


}
