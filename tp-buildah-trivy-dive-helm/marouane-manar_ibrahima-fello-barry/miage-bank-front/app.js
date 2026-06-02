// Simulation de données pour donner vie à l'interface
const comptes = [
    { type: "Compte Courant", balance: "€4,250.00", iban: "FR76 1234 5678 9012" },
    { type: "Livret A", balance: "€15,000.00", iban: "FR76 0987 6543 2109" },
    { type: "Compte Épargne", balance: "€5,312.00", iban: "FR76 4567 8901 2345" }
];

const transactions = [
    { title: "Virement: AMAZON EU", date: "Aujourd'hui, 14:30", amount: "-€34.99" },
    { title: "Salaire MARS", date: "01 Avr 2026", amount: "+€2,450.00", positive: true },
    { title: "Paiement: RESTAURANT LE BISTROT", date: "30 Mar 2026", amount: "-€65.00" },
    { title: "Prélèvement: EDF", date: "28 Mar 2026", amount: "-€45.20" }
];

document.addEventListener('DOMContentLoaded', () => {
    // Remplir les comptes
    const accountList = document.getElementById('accountList');
    comptes.forEach(acc => {
        const li = document.createElement('li');
        li.className = 'account-item';
        li.innerHTML = `
            <div class="acc-type">${acc.type}</div>
            <div class="acc-bal">${acc.balance}</div>
            <div class="acc-num">${acc.iban}</div>
        `;
        accountList.appendChild(li);
    });

    // Remplir les transactions
    const transactionList = document.getElementById('transactionList');
    transactions.forEach(tx => {
        const li = document.createElement('li');
        li.className = 'tx-item';
        const colorClass = tx.positive ? 'positive' : '';
        li.innerHTML = `
            <div class="tx-left">
                <span class="tx-title">${tx.title}</span>
                <span class="tx-date">${tx.date}</span>
            </div>
            <div class="tx-amount ${colorClass}">${tx.amount}</div>
        `;
        transactionList.appendChild(li);
    });
});
