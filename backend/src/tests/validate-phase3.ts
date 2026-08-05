const API_URL = "http://localhost:3333";

const timestamp = Date.now();
const userA = {
  name: "Tenant A",
  email: `a3_${timestamp}@teste.com`,
  password: "123456",
};
const userB = {
  name: "Tenant B",
  email: `b3_${timestamp}@teste.com`,
  password: "123456",
};

async function runPhase3Audit() {
  try {
    await fetch(`${API_URL}/users/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userA),
    });
    await fetch(`${API_URL}/users/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userB),
    });

    const tokenA = (
      await (
        await fetch(`${API_URL}/users/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: userA.email,
            password: userA.password,
          }),
        })
      ).json()
    ).token;

    const tokenB = (
      await (
        await fetch(`${API_URL}/users/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: userB.email,
            password: userB.password,
          }),
        })
      ).json()
    ).token;

    if (!tokenA || !tokenB) {
      throw new Error("Falha na geração de token (Login).");
    }

    await fetch(`${API_URL}/accounts`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({ name: "Conta Principal", type: "checking" }),
    });

    await fetch(`${API_URL}/categories`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({
        name: "Salário",
        color: "#00FF00",
        icon: "money",
        type: "income",
        is_default: false,
      }),
    });

    const accountIdA = (
      await (
        await fetch(`${API_URL}/accounts`, {
          headers: { Authorization: `Bearer ${tokenA}` },
        })
      ).json()
    ).accounts[0]?.id;

    const categoryIdA = (
      await (
        await fetch(`${API_URL}/categories`, {
          headers: { Authorization: `Bearer ${tokenA}` },
        })
      ).json()
    ).categories[0]?.id;

    if (!accountIdA || !categoryIdA) {
      throw new Error("Falha ao recuperar Account ou Category gerada.");
    }

    const resIncome = await fetch(`${API_URL}/transactions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({
        title: "Salário",
        amount: 5000.0,
        status: "completed",
        account_id: accountIdA,
        category_id: categoryIdA,
      }),
    });

    const resExpense = await fetch(`${API_URL}/transactions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({
        title: "Aluguel",
        amount: -1000.0,
        status: "pending",
        account_id: accountIdA,
        category_id: categoryIdA,
      }),
    });

    if (resIncome.status !== 201 || resExpense.status !== 201) {
      throw new Error("Falha na criação das transações base.");
    }

    const transactions = await (
      await fetch(`${API_URL}/transactions`, {
        headers: { Authorization: `Bearer ${tokenA}` },
      })
    ).json();

    const pendingTx = transactions.transactions.find(
      (t: any) => t.title === "Aluguel",
    );

    if (!pendingTx) {
      throw new Error("Transação pendente não encontrada na listagem.");
    }

    const resUpdate = await fetch(
      `${API_URL}/transactions/${pendingTx.id}/complete`,
      {
        method: "PATCH",
        headers: { Authorization: `Bearer ${tokenA}` },
      },
    );

    if (resUpdate.status !== 204) {
      throw new Error(
        `Falha ao completar transação [Status: ${resUpdate.status}]: ${await resUpdate.text()}`,
      );
    }

    const summaryA = await (
      await fetch(`${API_URL}/transactions/summary`, {
        headers: { Authorization: `Bearer ${tokenA}` },
      })
    ).json();

    const income = Number(summaryA.summary?.income);
    const outcome = Number(summaryA.summary?.expense);
    const total = Number(summaryA.summary?.amount);

    if (income !== 5000 || outcome !== -1000 || total !== 4000) {
      throw new Error(
        `Valores incorretos do Summary. Entradas: ${income}, Saídas: ${outcome}, Total: ${total}`,
      );
    }

    const summaryB_response = await fetch(`${API_URL}/transactions/summary`, {
      headers: { Authorization: `Bearer ${tokenB}` },
    });

    if (summaryB_response.status === 200) {
      const summaryB = await summaryB_response.json();
      const totalB = Number(summaryB.summary?.amount || 0);

      if (totalB !== 0) {
        throw new Error(
          `Vazamento de dados: O Usuário B está vendo R$ ${totalB} no resumo.`,
        );
      }
    }

    console.log(
      "Auditoria da Fase 3 (Transactions & Summary) concluída com sucesso.",
    );
  } catch (error: any) {
    console.error("Erro na auditoria da Fase 3:", error.message);
  }
}

runPhase3Audit();
