package com.naumov.blockchain.servicedata;

import com.naumov.blockchain.model.Block;
import com.naumov.blockchain.model.Transaction;
import com.naumov.blockchain.model.Wallet;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import sun.security.provider.DSAPublicKeyImpl;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

public class BlockchainData {
    private ObservableList<Transaction> newBlockTransactionsFx;
    private ObservableList<Transaction> newBlockTransactions;
    private LinkedList<Block> currentBlockchain = new LinkedList<>();
    private Block latestBlock;
    private boolean exit = false;
    private int miningPoints;
    private static final int TIMEOUT_INTERVAL = 65;
    private static final int MINING_INTERVAL = 60;
    private Signature signature = Signature.getInstance("SHA256withDSA");

    private static BlockchainData instance;
    static {
        try{
        instance = new BlockchainData();
        }catch (NoSuchAlgorithmException e){
            e.printStackTrace();
        }
    }
    public static BlockchainData getInstance(){
        return instance;
    }
    public BlockchainData() throws NoSuchAlgorithmException {
        newBlockTransactionsFx = FXCollections.observableArrayList();
        newBlockTransactions = FXCollections.observableArrayList();
    }
    Comparator<Transaction> transactionComparator =
            Comparator.comparing(Transaction::getTimestamp);

    public ObservableList<Transaction> getTransactionsFx(){
        newBlockTransactionsFx.clear();
        newBlockTransactions.sort(transactionComparator);
        newBlockTransactionsFx.addAll(newBlockTransactions);
        return FXCollections.observableArrayList(newBlockTransactionsFx);
    }
    public String getWalletBalanceFx(){
        return getBalance(currentBlockchain, newBlockTransactions, WalletData.getInstance().getWallet().getPublicKey()).toString() ;
    }
    public Integer getBalance(LinkedList<Block> blockchain,
                              ObservableList<Transaction> currentLedger,
                              PublicKey walletAddress){

        Integer balance = 0;
        for (Block block: blockchain){
            for (Transaction transaction: block.getTransactionLedger()){
                if(Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())){
                    balance -= transaction.getValue();
                }
                if(Arrays.equals(transaction.getTo(), walletAddress.getEncoded())){
                    balance += transaction.getValue();
                }
            }
        }
        for(Transaction transaction : currentLedger){
            if(Arrays.equals(transaction.getFrom(), walletAddress.getEncoded())){
                balance -= transaction.getValue();
            }
        }
        return balance;
    }
    private void verifyBlockChain(LinkedList<Block> currentBlockchain) throws GeneralSecurityException {
        for (Block block : currentBlockchain){
            if(!block.isVerified(signature)){
                throw new GeneralSecurityException("invalid BlockChain Exception");
            }
            ArrayList<Transaction> transactions = block.getTransactionLedger();
            for (Transaction transaction: transactions){
                if(!transaction.isVerified(signature)){
                    throw new GeneralSecurityException("invalid Transaction");
                }
            }
        }
    }
    public void addTransactionState(Transaction transaction){
         newBlockTransactions.add(transaction);
        newBlockTransactions.sort(transactionComparator);
    }
    public void addTransaction(Transaction transaction, boolean blockReward) throws GeneralSecurityException{
        try{
            if(getBalance(currentBlockchain, newBlockTransactions,
                    new DSAPublicKeyImpl(transaction.getFrom()))<transaction.getValue() && blockReward){
                throw new GeneralSecurityException("invalid add transaction");
            }else {
                String basePath = System.getProperty("user.dir");
                Connection connection = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/blockchain.db");
                PreparedStatement preparedStatement;
                preparedStatement = connection.prepareStatement("INSERT INTO TRANSACTIONS" +
                        "(\"FROM\", \"TO\", LEDGER_ID, VALUE, SIGNATURE, CREATED_ON) " +
                        " VALUES (?,?,?,?,?,?) ");
                preparedStatement.setBytes(1, transaction.getFrom());
                preparedStatement.setBytes(2, transaction.getTo());
                preparedStatement.setInt(3, transaction.getLedgerId());
                preparedStatement.setInt(4, transaction.getValue());
                preparedStatement.setBytes(5, transaction.getSignature());
                preparedStatement.setString(6, transaction.getTimestamp());
                preparedStatement.close();
                connection.close();
            }

        }catch (SQLException sqlException){
            sqlException.printStackTrace();
        }
    }
    public void loadBlockChain(){
        try{
            String basePath = System.getProperty("user.dir");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + basePath +  "/db/blockchain.db");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT * FROM BLOCKCHAIN");
            while(resultSet.next()){
                this.currentBlockchain.add(new Block(
                        resultSet.getBytes("PREVIOUS_HASH"),
                        resultSet.getBytes("CURRENT_HASH"),
                        resultSet.getString("CREATED_ON"),
                        resultSet.getBytes("CREATED_BY"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getInt("MINING_POINTS"),
                        resultSet.getDouble("LUCK"),
                        null
                        // load Transaction;
                ));

            }
            latestBlock = currentBlockchain.getLast();
            Transaction transaction = new Transaction(new Wallet(),
                    WalletData.getInstance().getWallet().getPublicKey().getEncoded(),
                    100, latestBlock.getLedgerId() + 1,signature );
            newBlockTransactions.clear();
            newBlockTransactions.add(transaction);
            verifyBlockChain(currentBlockchain);
            resultSet.close();
            statement.close();
            connection.close();
        }catch (SQLException  | NoSuchAlgorithmException exception){
            exception.printStackTrace();
        }catch (GeneralSecurityException e){
            e.printStackTrace();
        }
    }
    private ArrayList<Transaction> loadTransactionLedger(Integer ledgerId) throws SQLException{
        ArrayList<Transaction> transactions = new ArrayList<>();
        try{
            String basePath = System.getProperty("user.dir");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/blockchain.db");
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM TRANSACTIONS" +
                    " WHERE LEDGER_ID = ?");
            preparedStatement.setInt(1, ledgerId);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()){
                transactions.add(new Transaction(
                        resultSet.getBytes("FROM"),
                        resultSet.getBytes("TO"),
                        resultSet.getInt("VALUE"),
                        resultSet.getBytes("SIGNATURE"),
                        resultSet.getInt("LEDGER_ID"),
                        resultSet.getString("CREATED_ON")

                ));
                resultSet.close();
                preparedStatement.close();
                connection.close();
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return transactions;
    }
    private void miningBlock(){

    }
    private void finalizeBlock(Wallet minerWallet) throws GeneralSecurityException, SQLException{
        latestBlock = new Block(BlockchainData.getInstance().currentBlockchain);
        latestBlock.setTransactionLedger(new ArrayList<>(newBlockTransactions));
        latestBlock.setTimeStamp(LocalDate.now().toString());
        latestBlock.setMinedBy(minerWallet.getPublicKey().getEncoded());
        latestBlock.setMiningPoints(miningPoints);
        signature.initSign(minerWallet.getPrivateKey());
        signature.update(latestBlock.toString().getBytes());
        latestBlock.setCurrHash(signature.sign());
        currentBlockchain.add(latestBlock);
        miningPoints = 0;

        latestBlock.getTransactionLedger().sort(transactionComparator);
        addTransaction(latestBlock.getTransactionLedger().get(0), true);
        Transaction transaction = new Transaction(new Wallet(),
                minerWallet.getPublicKey().getEncoded(), 100, latestBlock.getLedgerId() + 1, signature);
        newBlockTransactions.clear();
        newBlockTransactions.add(transaction);
    }
    private void addBlock(Block block){
        try{
            String basePath = System.getProperty("user.dir");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/blockchain.db");
            PreparedStatement preparedStatement;
            preparedStatement = connection.prepareStatement ("INSERT INTO BLOCKCHAIN(PREVIOUS_HASH, CURRENT_HASH, LEDGER_ID, CREATED_ON," +
                    " CREATED_BY, MINING_POINTS, LUCK) VALUES (?,?,?,?,?,?,?) ");

            preparedStatement.setBytes(1, block.getPrevHash());
            preparedStatement.setBytes(2, block.getCurrHash());
            preparedStatement.setInt(3, block.getLedgerId());
            preparedStatement.setString(4, block.getTimeStamp());
            preparedStatement.setBytes(5,block.getMinedBy());
            preparedStatement.setInt(6, block.getMiningPoints());
            preparedStatement.setDouble(7, block.getLuck());
            preparedStatement.close();
            connection.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }
    private void replaceBlockchainInDatabase(LinkedList<Block> receivedBC){
    try{
        String basePath = System.getProperty("user.dir");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + basePath + "/db/blockchain.db");
        Statement clearDbStatement = connection.createStatement();
        clearDbStatement.executeUpdate("DELETE FROM BLOCKCHAIN");
        clearDbStatement.executeUpdate("DELETE FROM TRANSACTIONS");
        clearDbStatement.close();
        connection.close();
        for (Block block : receivedBC){
            addBlock(block);
            boolean rewardTransaction = true;
            block.getTransactionLedger().sort(transactionComparator);
            for(Transaction transaction: block.getTransactionLedger()){
                addTransaction(transaction, rewardTransaction);
                rewardTransaction = false;
            }
        }
    }catch (SQLException | GeneralSecurityException e){
        e.printStackTrace();
       }
    }
    public LinkedList<Block> getBlockchainConsensus(LinkedList<Block> receivedBC){
    try{
        verifyBlockChain(receivedBC);
        if(Arrays.equals(receivedBC.getLast().getCurrHash(), getCurrentBlockchain().getLast().getCurrHash())){
            if(checkIfOutdated(receivedBC) != null){
                return getCurrentBlockchain();
            }else {
                if(checkWhichIsCreatedFirst(receivedBC) != null){
                    return null;
                }else {

                }
            }
        } else if(!receivedBC.getLast().getTransactionLedger()
                .equals(getCurrentBlockchain().getLast().getTransactionLedger())){
            updateTransactionLedger(receivedBC);
           return receivedBC;
        }else {
            System.out.println("blockchains are identical");
        }
    }catch (GeneralSecurityException e){
        e.printStackTrace();
     }
    return receivedBC;
    }
    private void updateTransactionLedger(LinkedList<Block> receivedBC){

    }
    private  LinkedList<Block> checkIfOutdated(LinkedList<Block> receivedBlock){
        long lastMinedLocalBlock = LocalDateTime.parse
                (getCurrentBlockchain().getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        long lastMinedRcvdBlock  = LocalDateTime.parse
                (receivedBlock.getLast().getTimeStamp()).toEpochSecond(ZoneOffset.UTC);
        if((lastMinedLocalBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        && ((lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC))){
            System.out.println("both are old both are old ");
        } else if((lastMinedLocalBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        && (lastMinedRcvdBlock + TIMEOUT_INTERVAL) >= LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)){
            // setMining

        } else if((lastMinedLocalBlock + TIMEOUT_INTERVAL) > LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                && (lastMinedRcvdBlock + TIMEOUT_INTERVAL) < LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) ){
        return getCurrentBlockchain();
        }
        return null;
    }
    private LinkedList<Block> checkWhichIsCreatedFirst(LinkedList<Block> receivedBC){
        long initRcvBlockTime = LocalDateTime.parse(receivedBC.getFirst().getTimeStamp())
                .toEpochSecond(ZoneOffset.UTC);
        long initLocalBlockTIme = LocalDateTime.parse(getCurrentBlockchain().getFirst().getTimeStamp())
                .toEpochSecond(ZoneOffset.UTC);
        if(initLocalBlockTIme < initLocalBlockTIme){
            setMiningPoints(0);

            replaceBlockchainInDatabase(receivedBC);
            setCurrentBlockchain(new LinkedList<>());
            loadBlockChain();
            System.out.println("Peer Server's BC was old");
        }else if(initLocalBlockTIme<initRcvBlockTime){
            return getCurrentBlockchain();
        }

        return null;
    }
    public LinkedList<Block> getCurrentBlockchain(){
        return currentBlockchain;
    }

    public void setCurrentBlockchain(LinkedList<Block> currentBlockchain) {
        this.currentBlockchain = currentBlockchain;
    }

    public int getMiningPoints() {
        return miningPoints;
    }

    public Block getLatestBlock() {
        return latestBlock;
    }

    public static int getMiningInterval() {
        return MINING_INTERVAL;
    }

    public static int getTimeoutInterval() {
        return TIMEOUT_INTERVAL;
    }

    public void setLatestBlock(Block latestBlock) {
        this.latestBlock = latestBlock;
    }

    public void setMiningPoints(int miningPoints) {
        this.miningPoints = miningPoints;
    }
}
