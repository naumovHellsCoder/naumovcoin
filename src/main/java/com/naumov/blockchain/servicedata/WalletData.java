package com.naumov.blockchain.servicedata;

import com.naumov.blockchain.model.Wallet;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.sql.*;

public class WalletData {
    private Wallet wallet;
    private static WalletData instance;
     static {
        instance = new WalletData();
    }

    public static void main(String[] args) {
        System.out.println(System.getProperty("user.dir"));
    }
    public static WalletData getInstance(){
         return instance;
    }
    public void loadWallet() throws SQLException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        String basePath = System.getProperty("user.dir");
        Connection connectionToWallet = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/wallet.db");
        Statement statementWallet = connectionToWallet.createStatement();
        ResultSet resultSet;
        resultSet = statementWallet.executeQuery("SELECT * from WALLET");
        KeyFactory keyFactory = KeyFactory.getInstance("DSA");
        PublicKey publicKey = null;
        PrivateKey privateKey = null;
        while (resultSet.next()){
            publicKey = keyFactory.generatePublic(new X509EncodedKeySpec(resultSet.getBytes("PUBLIC_KEY")));
            privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(resultSet.getBytes("PRIVATE_KEY")));
        }
        this.wallet = new Wallet(publicKey, privateKey);
    }
    public Wallet getWallet(){
         return wallet;
    }
}
