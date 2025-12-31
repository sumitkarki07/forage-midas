package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Incentive;
import com.jpmc.midascore.foundation.Transaction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionListener {

    private final DatabaseConduit databaseConduit;
    private final RestTemplate restTemplate;

    public TransactionListener(DatabaseConduit databaseConduit,
                               RestTemplate restTemplate) {
        this.databaseConduit = databaseConduit;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core")
    @Transactional
    public void listen(Transaction transaction) {
        if (transaction == null) return;

        // 1️⃣ find users
        UserRecord sender =
                databaseConduit.findUserById(transaction.getSenderId());
        UserRecord recipient =
                databaseConduit.findUserById(transaction.getRecipientId());

        // 2️⃣ validate users
        if (sender == null || recipient == null) return;

        // 3️⃣ validate balance
        float amount = transaction.getAmount();
        if (sender.getBalance() < amount) return;

        // 4️⃣ call Incentive API
        Incentive incentive =
                restTemplate.postForObject(
                        "http://localhost:8080/incentive",
                        transaction,
                        Incentive.class
                );

        float incentiveAmount =
                incentive == null ? 0f : incentive.getAmount();

        // 5️⃣ update balances
        sender.setBalance(sender.getBalance() - amount);
        recipient.setBalance(
                recipient.getBalance() + amount + incentiveAmount
        );

        // 6️⃣ persist users
        databaseConduit.save(sender);
        databaseConduit.save(recipient);

        // 7️⃣ persist transaction WITH incentive
        TransactionRecord record =
                new TransactionRecord(
                        sender,
                        recipient,
                        amount,
                        incentiveAmount
                );

        databaseConduit.save(record);
        if ("wilbur".equals(sender.getName()) || "wilbur".equals(recipient.getName())) {
    UserRecord wilbur =
        "wilbur".equals(sender.getName()) ? sender : recipient;

    System.out.println("WILBUR_BALANCE=" + wilbur.getBalance());
}

    }
}
