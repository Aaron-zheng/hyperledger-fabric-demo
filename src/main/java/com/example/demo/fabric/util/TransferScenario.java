package com.example.demo.fabric.util;


import org.hyperledger.fabric.sdk.*;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;


public class TransferScenario {

    private HFClient client = DefaultUtil.getHFClient();
    private ChaincodeID chaincodeID = DefaultUtil.getChaincodeID();
    private Channel channel = DefaultUtil.getChannel();


    @Test
    public void test() throws Exception {

        ////////////////////////////////////////////////////////添加
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chaincodeID);
        transactionProposalRequest.setFcn("invoke");
        transactionProposalRequest.setArgs(new String[] {"move", "a", "b", "100"});


        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
        transactionProposalRequest.setTransientMap(tm2);

        Collection<ProposalResponse> responses = channel.sendTransactionProposal(transactionProposalRequest, channel.getPeers());

        channel.sendTransaction(responses, channel.getOrderers());
    }
}
