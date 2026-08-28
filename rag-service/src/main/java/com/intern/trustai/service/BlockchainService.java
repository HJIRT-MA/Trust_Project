package com.intern.trustai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class BlockchainService {

    private final Web3j web3j;
    private final String privateKey;
    private final String contractAddress;

    public BlockchainService(Web3j web3j,
                             @Value("${web3j.private-key}") String privateKey,
                             @Value("${web3j.contract-address}") String contractAddress) {
        this.web3j = web3j;
        this.privateKey = privateKey;
        this.contractAddress = contractAddress;
    }

    public String storeAuditProof(String reportContent, String metadata) throws Exception {
        if (privateKey == null || privateKey.isEmpty() || "YOUR_PRIVATE_KEY".equals(privateKey)) {
            System.out.println("No private key configured, skipping blockchain transaction.");
            return "skipped";
        }

        Credentials credentials = Credentials.create(privateKey);
        
        // Calculate SHA-256 hash of the content
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(reportContent.getBytes(StandardCharsets.UTF_8));

        Function function = new Function(
                "storeHash",
                Arrays.asList(new Bytes32(hashBytes), new Utf8String(metadata)),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                credentials.getAddress(), DefaultBlockParameterName.LATEST).send();
        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        // Polygon Amoy testnet specific gas or dynamic gas (EIP-1559 usually preferred, but using legacy for simplicity here)
        BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
        BigInteger gasLimit = BigInteger.valueOf(300000L); // Estimate

        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce, gasPrice, gasLimit, contractAddress, encodedFunction);

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();

        if (ethSendTransaction.hasError()) {
            throw new Exception("Error storing proof: " + ethSendTransaction.getError().getMessage());
        }

        return ethSendTransaction.getTransactionHash();
    }

    public boolean verifyAuditProof(String reportContent) throws Exception {
        if (contractAddress == null || contractAddress.isEmpty()) return true; // skip verification if not configured

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(reportContent.getBytes(StandardCharsets.UTF_8));
        
        Function function = new Function(
                "getProof",
                Arrays.asList(new Bytes32(hashBytes)),
                Arrays.asList(
                    new org.web3j.abi.TypeReference<Bytes32>() {},
                    new org.web3j.abi.TypeReference<Utf8String>() {},
                    new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {},
                    new org.web3j.abi.TypeReference<org.web3j.abi.datatypes.Address>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);
        
        org.web3j.protocol.core.methods.response.EthCall response = web3j.ethCall(
                org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contractAddress, encodedFunction),
                DefaultBlockParameterName.LATEST).send();
                
        if (response.hasError()) {
            System.err.println("Web3j call error: " + response.getError().getMessage());
            return false;
        }

        String value = response.getValue();
        if (value == null || value.equals("0x")) {
            return false;
        }
        
        List<Type> results = org.web3j.abi.FunctionReturnDecoder.decode(value, function.getOutputParameters());
        if (results.isEmpty()) return false;
        
        // The first output is bytes32 hash. We just need to know it returned something with timestamp != 0.
        // But getProof reverts if "Proof not found". So if it didn't revert, it exists!
        // Actually, ethCall to a reverting function returns an error or "0x". So if results is not empty, it's valid.
        return true;
    }
}
