package com.example.demo.fabric.util;

import org.hyperledger.fabric.sdk.*;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;


public class NormalScenario {

    private HFClient client = DefaultUtil.getHFClient();
    private ChaincodeID chaincodeID = DefaultUtil.getChaincodeID();
    private Channel channel = DefaultUtil.getChannel();

    @Test
    public void test() throws Exception {

        ////////////////////////////////////////////////////////测试查询
        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[] {"query", "a"});
        queryByChaincodeRequest.setFcn("invoke");
        queryByChaincodeRequest.setChaincodeID(chaincodeID);
        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
        queryByChaincodeRequest.setTransientMap(tm2);
        Collection<ProposalResponse> queryProposals = channel.queryByChaincode(queryByChaincodeRequest, channel.getPeers());
        for(ProposalResponse queryProposal : queryProposals) {
            String payload = queryProposal.getProposalResponse().getResponse().getPayload().toStringUtf8();
            System.out.println("===== queryProposal: " + queryProposal.getPeer().getName());
            System.out.println("===== payload: " + payload);
        }
    }

}
