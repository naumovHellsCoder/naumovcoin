package com.naumov.blockchain;

import com.naumov.blockchain.model.Wallet;
import javafx.application.Application;
import javafx.stage.Stage;
import java.io.IOException;
import java.sql.*;
public class Coin extends Application {

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void init(){
        try{
        String basePath = System.getProperty("user.dir");

        Connection walletConnection = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/wallet.db" );
            Statement walletStatement = walletConnection.createStatement();
            walletStatement.executeUpdate("CREATE TABLE IF NOT EXISTS WALLET ( " +
                    " PRIVATE_KEY BLOB NOT NULL UNIQUE, " +
                    " PUBLIC_KEY BLOB NOT NULL UNIQUE, " +
                    " PRIMARY KEY (PRIVATE_KEY, PUBLIC_KEY)" +
                    ") "
            );

            ResultSet resultSetWallet = walletStatement.executeQuery("SELECT * FROM WALLET");

            if(!resultSetWallet.next()){
                Wallet wallet = new Wallet();
                byte[] pubBlob = wallet.getPublicKey().getEncoded();
                byte[] prvBlob = wallet.getPrivateKey().getEncoded();
                PreparedStatement preparedStatement =
                        walletConnection.prepareStatement("INSERT INTO WALLET( PRIVATE_KEY, PUBLIC_KEY)" +
                                "VALUES (?, ?)");
                preparedStatement.setBytes(1, prvBlob);
                preparedStatement.setBytes(1, pubBlob);

            }

            resultSetWallet.close();
            walletStatement.close();
            walletConnection.close();
            // *****
               // WalletData
            // ****


            Connection blockchainConnection = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/blockchain.db");
            Statement blockchainStatement = blockchainConnection.createStatement();
            blockchainStatement.executeUpdate("CREATE TABLE IF NOT EXISTS BLOCKCHAIN ( " +
                    " ID INTEGER NOT NULL UNIQUE, " +
                    " PREVIOUS_HASH BLOB UNIQUE, " +
                    " CURRENT_HASH BLOB UNIQUE, " +
                    " LEDGER_ID INTEGER NOT NULL UNIQUE, " +
                    " CREATED_ON  TEXT, " +
                    " CREATED_BY  BLOB, " +
                    " MINING_POINTS  TEXT, " +
                    " LUCK  NUMERIC, " +
                    " PRIMARY KEY( ID AUTOINCREMENT) " +
                    ")"
            );
            blockchainStatement.close();
            blockchainConnection.close();

        }catch (SQLException e){
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    @Override
    public void start(Stage stage) throws IOException {

    }

}