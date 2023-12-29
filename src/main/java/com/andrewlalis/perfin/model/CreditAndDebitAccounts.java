package com.andrewlalis.perfin.model;

import java.util.function.Consumer;

public record CreditAndDebitAccounts(Account creditAccount, Account debitAccount) {
    public boolean hasCredit() {
        return creditAccount != null;
    }

    public boolean hasDebit() {
        return debitAccount != null;
    }

    public void ifCredit(Consumer<Account> accountConsumer) {
        if (hasCredit()) accountConsumer.accept(creditAccount);
    }

    public void ifDebit(Consumer<Account> accountConsumer) {
        if (hasDebit()) accountConsumer.accept(debitAccount);
    }
}
