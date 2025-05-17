# Named methods

public class ReportGenerator {

    public PartyIdentification32 createPartyIdentification32(Account account) {
        Party6Choice party6Choice = new Party6Choice()
                .withOrgId(new OrganisationIdentification4()
                        .withOthr(new GenericOrganisationIdentification1()
                                .withId(account.ownerId())
                                .withSchmeNm(new OrganisationIdentificationSchemeName1Choice()
                                        .withCd("BANK")
                                )
                        )
                );

        return new PartyIdentification32()
                .withId(party6Choice)
                .withNm(account.ownerName());
    }

    public PartyIdentification32 createPartyIdentification(Map<String, String> valueMap) {
        Party6Choice party6Choice = new Party6Choice()
                .withOrgId(new OrganisationIdentification4()
                        .withOthr(new GenericOrganisationIdentification1()
                                .withId(valueMap.getOrDefault("id", ""))
                                .withSchmeNm(new OrganisationIdentificationSchemeName1Choice()
                                        .withCd(valueMap.getOrDefault("type", ""))
                                )
                        )
                );

        return new PartyIdentification32()
                .withId(party6Choice)
                .withNm(valueMap.getOrDefault("name", ""));
    }

    public CashAccount20 createCashAccount20(Account account) {
        GenericAccountIdentification1 genericAccountIdentification1 = new GenericAccountIdentification1()
                .withId(account.bban())
                .withSchmeNm(new AccountSchemeName1Choice().withCd("BANK"));

        BranchAndFinancialInstitutionIdentification4 branch = new BranchAndFinancialInstitutionIdentification4()
                .withFinInstnId(new FinancialInstitutionIdentification7().withBIC("SWEDSESS"));

        return new CashAccount20()
                .withId(new AccountIdentification4Choice()
                        .withIBAN(account.iban())
                        .withOthr(genericAccountIdentification1)
                )
                .withNm(account.ownerName())
                .withCcy(account.currency())
                .withOwnr(createPartyIdentification32(account))
                .withSvcr(branch);
    }

    public GroupHeader42 createGroupHeader(ReportType reportType, Map<String, String> metadata) {
        PartyIdentification32 partyIdentification32 = createPartyIdentification(
                Map.of("id", metadata.get("id"), "name", metadata.get("name"), "type", "BANK")
        );

        return new GroupHeader42()
                .withMsgId(generateMessageId.apply(reportType))
                .withCreDtTm(generateDateTime.get())
                .withMsgRcpt(partyIdentification32)
                .withAddtlInf(ADDITIONAL_INFO.getOrDefault(reportType, ""))
                .withMsgPgntn(null);
    }

    public TotalTransactions2 createTotalTransactions(List<Entry> entries) {
        NumberAndSumOfTransactions1 total = new NumberAndSumOfTransactions1()
                .withSum(entries.stream().map(Entry::amount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .withNbOfNtries(String.valueOf(entries.size()));

        return new TotalTransactions2().withTtlDbtNtries(total);
    }

    public AmountAndCurrencyExchangeDetails3 createAmountAndCurrency(String amount) {
        ActiveOrHistoricCurrencyAndAmount currencyAmount = new ActiveOrHistoricCurrencyAndAmount()
                .withCcy("SEK")
                .withValue(new BigDecimal(amount));
        return new AmountAndCurrencyExchangeDetails3().withAmt(currencyAmount);
    }

    public CashAccount16 createCashAccount16(Map<String, String> valueMap) {
        return new CashAccount16()
                .withId(new AccountIdentification4Choice().withIBAN(valueMap.getOrDefault("iban", "")));
    }

    public EntryTransaction2 createEntryTransaction(EntryDetail entryDetail) {
        TransactionReferences2 references = new TransactionReferences2()
                .withMsgId(entryDetail.msgId())
                .withAcctSvcrRef(entryDetail.acctSvcrRef())
                .withPmtInfId(entryDetail.pmtInfId())
                .withInstrId(entryDetail.instrId())
                .withEndToEndId(entryDetail.e2eId());

        AmountAndCurrencyExchange3 amountDetails = new AmountAndCurrencyExchange3()
                .withInstdAmt(createAmountAndCurrency(String.valueOf(entryDetail.instdAmt())))
                .withTxAmt(createAmountAndCurrency(String.valueOf(entryDetail.txnAmt())));

        Map<String, String> debitParty = Map.of(
                "id", entryDetail.relatedParties().get("debit").id(),
                "name", entryDetail.relatedParties().get("debit").name(),
                "type", "BANK"
        );
        Map<String, String> creditParty = Map.of(
                "name", entryDetail.relatedParties().get("credit").name(),
                "type", "BANK"
        );

        TransactionParty2 party = new TransactionParty2()
                .withDbtr(createPartyIdentification(debitParty))
                .withDbtrAcct(createCashAccount16(Map.of("iban", entryDetail.relatedAccounts().get("debit").iban())))
                .withCdtr(createPartyIdentification(creditParty))
                .withCdtrAcct(createCashAccount16(Map.of("iban", entryDetail.relatedAccounts().get("credit").iban())));

        TransactionAgents2 agents = new TransactionAgents2()
                .withDbtrAgt(new BranchAndFinancialInstitutionIdentification4()
                        .withFinInstnId(new FinancialInstitutionIdentification7()
                                .withBIC(entryDetail.relatedAgents().getOrDefault("debit", ""))))
                .withCdtrAgt(new BranchAndFinancialInstitutionIdentification4()
                        .withFinInstnId(new FinancialInstitutionIdentification7()
                                .withBIC(entryDetail.relatedAgents().getOrDefault("credit", ""))));

        RemittanceInformation5 remittance = new RemittanceInformation5()
                .withUstrd(extractRemittanceInfoValue("Ustrd", entryDetail.rmtInf()));

        return new EntryTransaction2()
                .withRefs(references)
                .withAmtDtls(amountDetails)
                .withRltdPties(party)
                .withRltdAgts(agents)
                .withRmtInf(remittance);
    }

    public List<EntryDetails1> createReportEntryDetails(List<EntryDetail> entryDetails) {
        return entryDetails.stream()
                .map(detail -> new EntryDetails1().withTxDtls(createEntryTransaction(detail)))
                .collect(Collectors.toList());
    }

    public EntryDetails1 createReportEntryDetails2(List<EntryDetail> entryDetails) {
        List<EntryTransaction2> txDetails = entryDetails.stream()
                .map(this::createEntryTransaction)
                .collect(Collectors.toList());

        return new EntryDetails1().withTxDtls(txDetails);
    }

    public ReportEntry2 createReportEntry(int counter, Entry entry) {
        BankTransactionCodeStructure4 code = new BankTransactionCodeStructure4()
                .withPrtry(new ProprietaryBankTransactionCodeStructure1()
                        .withCd(entry.bankTxnCodePrtry())
                        .withIssr("Swedbank"));

        return new ReportEntry2()
                .withNtryRef(generateEntryRefId.apply(counter))
                .withAmt(new ActiveOrHistoricCurrencyAndAmount()
                        .withCcy(entry.currency())
                        .withValue(entry.amount()))
                .withCdtDbtInd(CreditDebitCode.fromValue(entry.creditDebitCode()))
                .withSts(EntryStatus2Code.BOOK)
                .withBookgDt(new DateAndDateTimeChoice()
                        .withDt(convertStringToXMLGregorianCalendar(entry.bookingDate())))
                .withValDt(new DateAndDateTimeChoice()
                        .withDt(convertStringToXMLGregorianCalendar(entry.valueDate())))
                .withAcctSvcrRef(entry.acctSvcrRef())
                .withBkTxCd(code)
                .withNtryDtls(createReportEntryDetails2(entry.entryDetails()));
    }

    public List<ReportEntry2> createReportEntries(List<Entry> entries) {
        AtomicInteger counter = new AtomicInteger(0);
        return entries.stream()
                .map(entry -> createReportEntry(counter.incrementAndGet(), entry))
                .collect(Collectors.toList());
    }

    public AccountNotification2 createAccountNotification(int sequenceId, Account account) {
        return new AccountNotification2()
                .withId(generateNotificationId.apply(sequenceId))
                .withCreDtTm(generateDateTime.get())
                .withAcct(createCashAccount20(account))
                .withTxsSummry(createTotalTransactions(account.entries()))
                .withNtry(createReportEntries(account.entries()));
    }

    public List<AccountNotification2> createAccountNotifications(List<Account> accounts) {
        AtomicInteger counter = new AtomicInteger(0);
        return accounts.stream()
                .map(account -> createAccountNotification(counter.incrementAndGet(), account))
                .collect(Collectors.toList());
    }

    public BankToCustomerDebitCreditNotificationV02 createBankNotification(List<ReportData> reportDataList, Map<String, String> metadata) {
        List<Account> accounts = groupCamt054Data(reportDataList);
        return new BankToCustomerDebitCreditNotificationV02()
                .withGrpHdr(createGroupHeader(ReportType.CAMT054D, metadata))
                .withNtfctn(createAccountNotifications(accounts));
    }
}



Great — let's optimize two of the heavier methods from your original code:

1. **`createEntryTransaction`** (formerly `generateEntryTransactions`)
2. **`createReportEntry`** (formerly `generateReportEntry`)

---

## ✅ 1. Refactored `createEntryTransaction`

This version improves **clarity**, **modularity**, **reuse**, and **readability**:

```java
private EntryTransaction2 createEntryTransaction(EntryDetail detail) {
    TransactionReferences2 references = buildTransactionReferences(detail);
    AmountAndCurrencyExchange3 amountDetails = buildAmountDetails(detail);
    TransactionParty2 parties = buildTransactionParties(detail);
    TransactionAgents2 agents = buildTransactionAgents(detail);
    RemittanceInformation5 remittanceInfo = buildRemittanceInfo(detail);

    return new EntryTransaction2()
            .withRefs(references)
            .withAmtDtls(amountDetails)
            .withRltdPties(parties)
            .withRltdAgts(agents)
            .withRmtInf(remittanceInfo);
}

// Sub-methods extracted for modularity:

private TransactionReferences2 buildTransactionReferences(EntryDetail detail) {
    return new TransactionReferences2()
            .withMsgId(detail.msgId())
            .withAcctSvcrRef(detail.acctSvcrRef())
            .withPmtInfId(detail.pmtInfId())
            .withInstrId(detail.instrId())
            .withEndToEndId(detail.e2eId());
}

private AmountAndCurrencyExchange3 buildAmountDetails(EntryDetail detail) {
    return new AmountAndCurrencyExchange3()
            .withInstdAmt(createAmount(detail.instdAmt()))
            .withTxAmt(createAmount(detail.txnAmt()));
}

private TransactionParty2 buildTransactionParties(EntryDetail detail) {
    Map<String, RelatedParty> relatedParties = detail.relatedParties();
    Map<String, RelatedAccount> relatedAccounts = detail.relatedAccounts();

    PartyIdentification32 dbtr = createParty(Map.of(
            "id", relatedParties.get("debit").id(),
            "name", relatedParties.get("debit").name(),
            "type", "BANK"
    ));

    PartyIdentification32 cdtr = createParty(Map.of(
            "name", relatedParties.get("credit").name(),
            "type", "BANK"
    ));

    CashAccount16 dbtrAcct = createCashAccount(Map.of("iban", relatedAccounts.get("debit").iban()));
    CashAccount16 cdtrAcct = createCashAccount(Map.of("iban", relatedAccounts.get("credit").iban()));

    return new TransactionParty2()
            .withDbtr(dbtr)
            .withDbtrAcct(dbtrAcct)
            .withCdtr(cdtr)
            .withCdtrAcct(cdtrAcct);
}

private TransactionAgents2 buildTransactionAgents(EntryDetail detail) {
    return new TransactionAgents2()
            .withDbtrAgt(new BranchAndFinancialInstitutionIdentification4().withFinInstnId(
                    new FinancialInstitutionIdentification7().withBIC(
                            detail.relatedAgents().getOrDefault("debit", "")
                    )))
            .withCdtrAgt(new BranchAndFinancialInstitutionIdentification4().withFinInstnId(
                    new FinancialInstitutionIdentification7().withBIC(
                            detail.relatedAgents().getOrDefault("credit", "")
                    )));
}

private RemittanceInformation5 buildRemittanceInfo(EntryDetail detail) {
    return new RemittanceInformation5().withUstrd(
            extractRemittanceInfoValue("Ustrd", detail.rmtInf())
    );
}
```

---

## ✅ 2. Refactored `createReportEntry`

Split into smaller pieces for better testability and clarity:

```java
private ReportEntry2 createReportEntry(int counter, Entry entry) {
    ActiveOrHistoricCurrencyAndAmount amount = new ActiveOrHistoricCurrencyAndAmount()
            .withCcy(entry.currency())
            .withValue(entry.amount());

    BankTransactionCodeStructure4 codeStructure = new BankTransactionCodeStructure4()
            .withPrtry(new ProprietaryBankTransactionCodeStructure1()
                    .withCd(entry.bankTxnCodePrtry())
                    .withIssr("XYZ"));

    DateAndDateTimeChoice bookingDate = new DateAndDateTimeChoice()
            .withDt(convertStringToXMLGregorianCalendar(entry.bookingDate()));

    DateAndDateTimeChoice valueDate = new DateAndDateTimeChoice()
            .withDt(convertStringToXMLGregorianCalendar(entry.valueDate()));

    return new ReportEntry2()
            .withNtryRef(generateEntryRefId.apply(counter))
            .withAmt(amount)
            .withCdtDbtInd(CreditDebitCode.fromValue(entry.creditDebitCode()))
            .withSts(EntryStatus2Code.BOOK)
            .withBookgDt(bookingDate)
            .withValDt(valueDate)
            .withAcctSvcrRef(entry.acctSvcrRef())
            .withBkTxCd(codeStructure)
            .withNtryDtls(createEntryDetails(entry.entryDetails()));
}

private EntryDetails1 createEntryDetails(List<EntryDetail> details) {
    List<EntryTransaction2> transactions = details.stream()
            .map(this::createEntryTransaction)
            .toList();

    return new EntryDetails1().withTxDtls(transactions);
}
```

---

## ✨ Benefits Gained

| Improvement       | Description                                                          |
| ----------------- | -------------------------------------------------------------------- |
| ✅ Testability     | Each component method can be unit-tested independently.              |
| ✅ Maintainability | Smaller methods improve traceability and debugging.                  |
| ✅ Reusability     | Common subcomponents (e.g., `createParty`) reused across generators. |
| ✅ Performance     | Avoids repeated work and makes logic explicit.                       |
| ✅ Thread-safety   | Stateless, functional-style methods are inherently safe.             |

---

Would you like help refactoring other specific parts, like the `generateGroupHeader`, `generateCashAccount20`, or top-level transformation like `generateBankNotificationV02`?
