const API_URL = "http://localhost:3333";

const timestamp = Date.now();
const userA = {
  name: "Tenant A",
  email: `a_${timestamp}@teste.com`,
  password: "123456",
};
const userB = {
  name: "Tenant B",
  email: `b_${timestamp}@teste.com`,
  password: "123456",
};

async function runPhase2Audit() {
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

    const resAccA = await fetch(`${API_URL}/accounts`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({ name: "Conta do Usuário A", type: "checking" }),
    });

    if (resAccA.status !== 201) {
      throw new Error(
        `Falha ao criar conta [Status: ${resAccA.status}]: ${await resAccA.text()}`,
      );
    }

    const accountsA = await (
      await fetch(`${API_URL}/accounts`, {
        headers: { Authorization: `Bearer ${tokenA}` },
      })
    ).json();
    const accountsB = await (
      await fetch(`${API_URL}/accounts`, {
        headers: { Authorization: `Bearer ${tokenB}` },
      })
    ).json();

    const accountIdA = accountsA.accounts[0]?.id;

    if (
      !accountIdA ||
      accountsA.accounts.length === 0 ||
      accountsB.accounts.length > 0
    ) {
      throw new Error("Falha na persistência ou isolamento de contas.");
    }

    const resCatA = await fetch(`${API_URL}/categories`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({
        name: "Lazer A",
        color: "#FF5733",
        icon: "gamepad",
        type: "expense",
        is_default: false,
      }),
    });

    if (resCatA.status !== 201) {
      throw new Error(
        `Falha ao criar categoria [Status: ${resCatA.status}]: ${await resCatA.text()}`,
      );
    }

    const categoriesA = await (
      await fetch(`${API_URL}/categories`, {
        headers: { Authorization: `Bearer ${tokenA}` },
      })
    ).json();
    const categoriesB = await (
      await fetch(`${API_URL}/categories`, {
        headers: { Authorization: `Bearer ${tokenB}` },
      })
    ).json();

    const catData = categoriesA.categories.find(
      (c: any) => c.name === "Lazer A",
    );
    const categoryIdA = catData?.id;

    if (
      !catData ||
      catData.color !== "#FF5733" ||
      catData.icon !== "gamepad" ||
      Boolean(catData.is_default) !== false
    ) {
      throw new Error("Falha na persistência dos campos visuais da categoria.");
    }

    if (categoriesB.categories?.some((c: any) => c.name === "Lazer A")) {
      throw new Error("Vazamento de dados nas categorias.");
    }

    const resTrans = await fetch(`${API_URL}/transactions`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({
        title: "Compra Teste FK",
        amount: 50.0,
        type: "expense",
        account_id: accountIdA,
        category_id: categoryIdA,
        date: new Date().toISOString(),
      }),
    });

    if (resTrans.status !== 201) {
      throw new Error(
        `Falha ao criar transação para teste de FK [Status: ${resTrans.status}]: ${await resTrans.text()}`,
      );
    }

    const resDelCat = await fetch(`${API_URL}/categories/${categoryIdA}`, {
      method: "DELETE",
      headers: { Authorization: `Bearer ${tokenA}` },
    });

    if (resDelCat.status === 200 || resDelCat.status === 204) {
      throw new Error(
        "Falha crítica: O sistema permitiu excluir uma categoria amarrada a uma transação (FK quebrada).",
      );
    }

    console.log(
      "Auditoria da Fase 2 (Accounts, Categories e FK) concluída com sucesso.",
    );
  } catch (error: any) {
    console.error("Erro na auditoria da Fase 2:", error.message);
  }
}

runPhase2Audit();
