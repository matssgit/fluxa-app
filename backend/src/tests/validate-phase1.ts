const API_URL = "http://localhost:3333";

const timestamp = Date.now();
const userA = {
  name: "Usuário A",
  email: `usera_${timestamp}@teste.com`,
  password: "senha_segura_123",
};
const userB = {
  name: "Usuário B",
  email: `userb_${timestamp}@teste.com`,
  password: "senha_segura_456",
};

async function runAuthAudit() {
  try {
    const resCreateA = await fetch(`${API_URL}/users/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userA),
    });
    const resCreateB = await fetch(`${API_URL}/users/register`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(userB),
    });

    if (resCreateA.status !== 201 || resCreateB.status !== 201) {
      throw new Error(
        `Falha ao criar usuários [Status A: ${resCreateA.status}, Status B: ${resCreateB.status}]`,
      );
    }

    const resLoginA = await fetch(`${API_URL}/users/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: userA.email, password: userA.password }),
    });
    const dataA = await resLoginA.json();

    if (
      resLoginA.status !== 200 ||
      !dataA.token ||
      dataA.token.split(".").length !== 3
    ) {
      throw new Error(
        "Falha ao realizar login ou payload de token inválido para o Usuário A.",
      );
    }
    const tokenA = dataA.token;

    const resLoginB = await fetch(`${API_URL}/users/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: userB.email, password: userB.password }),
    });
    const tokenB = (await resLoginB.json()).token;

    const resNoToken = await fetch(`${API_URL}/credit/cards`);
    if (resNoToken.status !== 401) {
      throw new Error(
        `Falha na proteção de rota sem token [Esperado: 401, Recebido: ${resNoToken.status}]`,
      );
    }

    const resInvalidToken = await fetch(`${API_URL}/credit/cards`, {
      headers: { Authorization: "Bearer token_inventado_ou_modificado_12345" },
    });
    if (resInvalidToken.status !== 401) {
      throw new Error(
        `Falha na proteção de rota com token inválido [Esperado: 401, Recebido: ${resInvalidToken.status}]`,
      );
    }

    await fetch(`${API_URL}/categories`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${tokenA}`,
      },
      body: JSON.stringify({
        name: "Categoria Secreta do A",
        color: "#000",
        type: "expense",
      }),
    });

    const resListB = await fetch(`${API_URL}/categories`, {
      headers: { Authorization: `Bearer ${tokenB}` },
    });
    const dataCatB = await resListB.json();

    const achouCategoriaDoA = dataCatB.categories?.some(
      (c: any) => c.name === "Categoria Secreta do A",
    );

    if (achouCategoriaDoA) {
      throw new Error(
        "Vazamento de dados: O Usuário B conseguiu acessar informações restritas do Usuário A.",
      );
    }

    console.log(
      "Auditoria de autenticação e isolamento multiusuário concluída com sucesso.",
    );
  } catch (error: any) {
    console.error("Erro na auditoria de autenticação:", error.message);
  }
}

runAuthAudit();
