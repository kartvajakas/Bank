package ee.bcs.bank.restbank;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    public static final String ATM = "ATM";
    public static final char NEW_ACCOUNT = 'n';
    public static final char DEPOSIT = 'd';
    public static final char WITHDRAWAL = 'w';
    public static final char SEND_MONEY = 's';
    public static final char RECEIVE_MONEY = 'r';

    @Resource
    private AccountService accountService;

    @Resource
    private BalanceService balanceService;


//    transactionType
//    n - new account
//    d - deposit
//    w - withdrawal
//    s - send money
//    r - receive money


    // TODO:    createExampleTransaction()
    //  account id 123
    //  balance 1000
    //  amount 100
    //  transactionType 's'
    //  receiver EE123
    //  sender EE456

    public TransactionDto createExampleTransaction() {
        TransactionDto transactionDto = new TransactionDto();
        transactionDto.setAccountId(123);
        transactionDto.setBalance(1000);
        transactionDto.setAmount(100);
        transactionDto.setTransactionType(SEND_MONEY);
        transactionDto.setReceiverAccountNumber("EE123");
        transactionDto.setSenderAccountNumber("EE456");
        transactionDto.setLocalDateTime(LocalDateTime.now());
        return transactionDto;
    }

    public RequestResult addNewTransaction(Bank bank, TransactionDto transactionDto) {
        //loon vajalikud objektid (tühjad)
        RequestResult requestResult = new RequestResult();

        List<AccountDto> accounts = bank.getAccounts();
        int accountId = transactionDto.getAccountId();

        //  kontrolli kas konto eksisteerib
        if (!accountService.accountIdExist(accounts, accountId)) {
            requestResult.setAccountId(accountId);
            requestResult.setError("Account ID " + accountId + " does not exist!!!");
            return requestResult;
        }

        //Vajalike andmete väljapärimine
        Character transactionType = transactionDto.getTransactionType();

        Integer amount = transactionDto.getAmount();

        int transactionId = bank.getTransactionIdCount();






        // päri välja accountId abiga õige konto ja balance'i
        AccountDto account = accountService.getAccountByID(accounts, accountId);
        Integer balance = account.getBalance();

        //töötleme läbi erinevad olukorrad
        int newBalance;

        String receiverAccountNumber;

        switch (transactionType) {
            case NEW_ACCOUNT:

                // TODO: kontrolli kas account id eksisteerib, kui mitte tagasta error sõnum

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(null);
                transactionDto.setReceiverAccountNumber(null);
                transactionDto.setBalance(0);
                transactionDto.setAmount(0);
                transactionDto.setLocalDateTime(LocalDateTime.now());

                transactionDto.setId(transactionId);

                // lisame tehingu transactionite alla (pluss incrementeerime)
                bank.addTransactionToTransactions(transactionDto);
                bank.incrementTransactionId();

                // meisterdame valmis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully added 'new account' transaction");
                return requestResult;

            case DEPOSIT:
                // arvutame välja  uue balance'i
                newBalance = balance + amount;//JSONist saame summa mida tahetakse üle kanda

                // TODO: kontrolli kas account id eksisteerib, kui mitte tagasta error sõnum

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(ATM);
                transactionDto.setReceiverAccountNumber(account.getAccountNumber());
                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionite alla (pluss inkrementeerime)
                bank.addTransactionToTransactions(transactionDto);
                bank.incrementTransactionId();

                //uuendame konto balance'it
                account.setBalance(newBalance);

                // meisterdame valmis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully made deposit transaction");
                return requestResult;
            case WITHDRAWAL:
                if (!balanceService.enoughMoneyOnAccount(balance, amount)) {
                    requestResult.setAccountId(accountId);
                    requestResult.setError("Not enough money" + amount);
                    return requestResult;
                }

                // TODO: 26.01.2022 järg!!!

                // arvutame välja  uue balance'i
                newBalance = balance - amount;//JSONist saame summa mida tahetakse üle kanda


                // TODO: kontrolli kas account id eksisteerib, kui mitte tagasta error sõnum

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(account.getAccountNumber());
                transactionDto.setReceiverAccountNumber(ATM);
                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionite alla (pluss inkrementeerime)
                bank.addTransactionToTransactions(transactionDto);
                bank.incrementTransactionId();

                //uuendame konto balance'it
                account.setBalance(newBalance);

                // meisterdame valmis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully made withdrawal transaction");
                return requestResult;
            case SEND_MONEY:
                if (!balanceService.enoughMoneyOnAccount(balance, amount)) {
                    requestResult.setAccountId(accountId);
                    requestResult.setError("Not enough money" + amount);
                    return requestResult;
                }

                // arvutame välja  uue balance'i
                newBalance = balance - amount;//JSONist saame summa mida tahetakse üle kanda


                // TODO: kontrolli kas account id eksisteerib, kui mitte tagasta error sõnum

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(account.getAccountNumber());

               // transactionDto.setReceiverAccountNumber(ATM); selle

                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionite alla (pluss inkrementeerime)
                bank.addTransactionToTransactions(transactionDto);
                bank.incrementTransactionId();

                //uuendame konto balance'it
                account.setBalance(newBalance);

                // meisterdame valmis result objekti. Kui saadame võõrasse kohta, siis üks transaction. Kui saadame oma pangas teisele kontole, siis kaks transactionit
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully sent money");


                // teeme SAAJA transaktsiooni
                receiverAccountNumber = transactionDto.getReceiverAccountNumber();


                //kontrollime kas saaja kontonumber eksisteerib (bank)
                if (accountService.accountNumberExists(accounts, receiverAccountNumber)) {

                    AccountDto receiverAccount = accountService.getAccountByAccountNumber(accounts, receiverAccountNumber);

                    int receiverNewBalance = receiverAccount.getBalance() + amount;

                    // kui eksisteerib, siis loome uue saaja transaktsiooni objekti, et sinna transaktsiooni infot saaks hakata panema
                    TransactionDto receiverTransactionDto = new TransactionDto();

                    // täidame ära transactionDto
                    receiverTransactionDto.setSenderAccountNumber(account.getAccountNumber());
                    receiverTransactionDto.setReceiverAccountNumber(receiverAccountNumber);
                    receiverTransactionDto.setBalance(receiverNewBalance);
                    receiverTransactionDto.setLocalDateTime(LocalDateTime.now());
                    receiverTransactionDto.setId(bank.getTransactionIdCount()); //kasutame järgmise nr ära, nüüd peame ka inkrementeerima järgmisel real
                    bank.incrementTransactionId(); // siin inkrementeerime

                    receiverTransactionDto.setAmount(amount);
                    receiverTransactionDto.setTransactionType(RECEIVE_MONEY);

                    // lisame tehingu transactionite alla (pluss inkrementeerime)
                    bank.addTransactionToTransactions(receiverTransactionDto);
                    bank.incrementTransactionId();
                    receiverAccount.setBalance(receiverNewBalance);
                }

                return requestResult;

            case RECEIVE_MONEY:

                receiverAccountNumber = transactionDto.getReceiverAccountNumber();

                if (!accountService.accountNumberExists(accounts, receiverAccountNumber)) {
                    requestResult.setAccountId(accountId);
                    requestResult.setError("Account number does not exist" + receiverAccountNumber);
                    return requestResult;
                }

                AccountDto receiverAccount = accountService.getAccountByAccountNumber(accounts, receiverAccountNumber);


                // arvutame välja  uue balance'i
                newBalance = balance + amount;//JSONist saame summa mida tahetakse üle kanda

                // TODO: kontrolli kas account id eksisteerib, kui mitte tagasta error sõnum

                // täidame ära transactionDto
                transactionDto.setSenderAccountNumber(ATM);
                transactionDto.setReceiverAccountNumber(account.getAccountNumber());
                transactionDto.setBalance(newBalance);
                transactionDto.setLocalDateTime(LocalDateTime.now());
                transactionDto.setId(transactionId);

                // lisame tehingu transactionite alla (pluss inkrementeerime)
                bank.addTransactionToTransactions(transactionDto);
                bank.incrementTransactionId();

                //uuendame konto balance'it
                account.setBalance(newBalance);

                // meisterdame valmis result objekti
                requestResult.setTransactionId(transactionId);
                requestResult.setAccountId(accountId);
                requestResult.setMessage("Successfully made deposit transaction");
                return requestResult;


            default:
                requestResult.setError("Unknown transaction type: " + transactionType);
                return requestResult;

        }

    }


    // TODO:    createTransactionForNewAccount()
    //  account number
    //  balance 0
    //  amount 0
    //  transactionType 'n'
    //  receiver jääb null
    //  sender jääb null


}
