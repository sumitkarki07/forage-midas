package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionListener {

    private final DatabaseConduit databaseConduit;

    public TransactionListener(DatabaseConduit databaseConduit) {
        this.databaseConduit = databaseConduit;
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
        float senderBalance = sender.getBalance();
        float amount = transaction.getAmount();
        if (senderBalance < amount) return;

        // 4️⃣ update balances
        sender.setBalance(senderBalance - amount);
        recipient.setBalance(recipient.getBalance() + amount);

        // 5️⃣ persist users
        databaseConduit.save(sender);
        databaseConduit.save(recipient);

        // 6️⃣ persist transaction
        TransactionRecord record =
                new TransactionRecord(sender, recipient, amount);
        databaseConduit.save(record);
    }
}
