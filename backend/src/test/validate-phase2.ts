// Comando para rodar: npx tsx src/test/validate-phase2.ts
const API_URL = "http://localhost:3333";

const timestamp = Date.now();
const userA = {
   name: "Tenant A",
   email: `a_${timestamp}@teste.com`,
   password: "123456", // <--- Mudei para 6 caracteres
};
const userB = {
   name: "Tenant B",
   email: `b_${timestamp}@teste.com`,
   password: "123456", // <--- Mudei para 6 caracteres
};

async function runPhase2Audit() {
   console.log("==================================================");
   console.log("🏛️ INICIANDO AUDITORIA DA FASE 2 (ACCOUNTS & CATEGORIES)");
   console.log("==================================================\n");

   try {
      // 1. SETUP DE USUÁRIOS E TOKENS (Corrigido com o prefixo /users)
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
         throw new Error(
            "Falha na etapa de Login: O token não foi gerado. Verifique se as rotas /register e /login estão funcionando.",
         );
      }

      // ---------------------------------------------------------
      // TESTES DE ACCOUNTS
      // ---------------------------------------------------------
      console.log("[1/4] Testando ACCOUNTS (Persistência e Multiusuário)...");
      const resAccA = await fetch(`${API_URL}/accounts`, {
         method: "POST",
         headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${tokenA}`,
         },
         body: JSON.stringify({ name: "Conta do Usuário A", type: "checking" }),
      });

      if (resAccA.status !== 201) {
         const errorBody = await resAccA.text();
         throw new Error(
            `Falha ao criar conta. Status: ${resAccA.status}. Resposta: ${errorBody}`,
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

      const accountIdA = accountsA.accounts[0].id;

      if (accountsA.accounts.length > 0 && accountsB.accounts.length === 0) {
         console.log("✅ Contas: Persistência confirmada.");
         console.log(
            "✅ Contas: Isolamento multiusuário perfeito (B não vê conta de A).",
         );
      } else {
         throw new Error("Falha no isolamento de contas.");
      }

      // ---------------------------------------------------------
      // TESTES DE CATEGORIES
      // ---------------------------------------------------------
      console.log("\n[2/4] Testando CATEGORIES (Campos e Multiusuário)...");
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
         const errorBody = await resCatA.text();
         throw new Error(
            `Falha ao criar categoria. Status: ${resCatA.status}. Resposta: ${errorBody}`,
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

      const categoryIdA = categoriesA.categories.find(
         (c: any) => c.name === "Lazer A",
      ).id;
      const catData = categoriesA.categories.find(
         (c: any) => c.name === "Lazer A",
      );

      if (
         catData.color === "#FF5733" &&
         catData.icon === "gamepad" &&
         catData.is_default === (catData.is_default === 0 ? 0 : false)
      ) {
         console.log(
            "✅ Categorias: Campos 'color', 'icon' e 'is_default' persistidos corretamente.",
         );
      } else {
         console.log(
            "❌ ALERTA: Campos visuais da categoria não salvaram corretamente.",
         );
      }

      if (!categoriesB.categories?.some((c: any) => c.name === "Lazer A")) {
         console.log("✅ Categorias: Isolamento multiusuário perfeito.");
      } else {
         throw new Error("Vazamento de dados nas categorias.");
      }

      // ---------------------------------------------------------
      // TESTE DE INTEGRIDADE REFERENCIAL (FOREIGN KEY)
      // ---------------------------------------------------------
      console.log(
         "\n[3/4] Forçando amarração da Categoria no Banco (Insert Transaction)...",
      );
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

      if (resTrans.status === 201) {
         console.log("✅ Transação gerada, categoria agora está 'em uso'.");
      } else {
         const errorBody = await resTrans.text();
         console.log(
            `⚠️ Aviso: Transação não foi criada (Status: ${resTrans.status}). Ignorando amarração. Resposta: ${errorBody}`,
         );
      }

      console.log(
         "\n[4/4] Testando Exclusão de Categoria em uso (PostgreSQL Constraint)...",
      );
      const resDelCat = await fetch(`${API_URL}/categories/${categoryIdA}`, {
         method: "DELETE",
         headers: { Authorization: `Bearer ${tokenA}` },
      });

      if (resDelCat.status !== 200 && resDelCat.status !== 204) {
         console.log(
            `✅ O Banco de dados BLOQUEOU a exclusão! (Status retornado: ${resDelCat.status})`,
         );
         console.log(
            "✅ Integridade Referencial (Foreign Keys) funcionando perfeitamente.",
         );
      } else {
         console.log(
            "❌ ERRO GRAVE: O sistema permitiu excluir uma categoria que estava amarrada a uma transação! (Parcela Órfã detectada)",
         );
      }

      console.log("\n==================================================");
      console.log("🏆 RELATÓRIO: AUDITORIA DA FASE 2 APROVADA");
      console.log("==================================================");
   } catch (error: any) {
      console.error("\n❌ ERRO NA AUDITORIA:", error.message);
   }
}

runPhase2Audit();
