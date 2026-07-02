// Comando para rodar: npx tsx src/test/validate-phase3.ts
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
   console.log("==================================================");
   console.log("💰 INICIANDO AUDITORIA DA FASE 3 (TRANSACTIONS & SUMMARY)");
   console.log("==================================================\n");

   try {
      // 1. SETUP INICIAL
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
      ).accounts[0].id;
      const categoryIdA = (
         await (
            await fetch(`${API_URL}/categories`, {
               headers: { Authorization: `Bearer ${tokenA}` },
            })
         ).json()
      ).categories[0].id;

      // 2. CRIANDO RECEITA (5000) E DESPESA PENDENTE (-1000)
      console.log("[1/4] Gerando Receita e Despesa...");

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

      if (resIncome.status === 201 && resExpense.status === 201) {
         console.log(
            "✅ Receita (completed) e Despesa (pending) criadas com sucesso.",
         );
      }

      // 3. DAR BAIXA EM PENDÊNCIA
      console.log("\n[2/4] Testando baixa de pendência...");
      const transactions = await (
         await fetch(`${API_URL}/transactions`, {
            headers: { Authorization: `Bearer ${tokenA}` },
         })
      ).json();
      const pendingTx = transactions.transactions.find(
         (t: any) => t.title === "Aluguel",
      );

      // CORREÇÃO: Tiramos o Content-Type para não dar erro de "Empty JSON Body" no Fastify
      const resUpdate = await fetch(
         `${API_URL}/transactions/${pendingTx.id}/complete`,
         {
            method: "PATCH",
            headers: { Authorization: `Bearer ${tokenA}` },
         },
      );

      if (resUpdate.status === 204) {
         console.log(
            "✅ Baixa na pendência executada com sucesso! (Retornou 204 No Content)",
         );
      } else {
         const errorBody = await resUpdate.text();
         throw new Error(
            `Falha ao dar baixa. Status: ${resUpdate.status}. Resposta: ${errorBody}`,
         );
      }

      // 4. VALIDAÇÃO MATEMÁTICA DO SUMMARY
      console.log("\n[3/4] Validando cálculo do Resumo (Summary)...");
      const summaryA = await (
         await fetch(`${API_URL}/transactions/summary`, {
            headers: { Authorization: `Bearer ${tokenA}` },
         })
      ).json();

      const income = Number(summaryA.summary.income);
      const outcome = Number(summaryA.summary.expense);
      const total = Number(summaryA.summary.amount);

      console.log(`📊 Entradas retornadas: R$ ${income}`);
      console.log(`📊 Saídas retornadas: R$ ${outcome}`);
      console.log(`📊 Saldo retornado: R$ ${total}`);

      if (income === 5000 && outcome === -1000 && total === 4000) {
         console.log(
            "✅ SUCESSO! A API retornou a matemática perfeita: 5000 - 1000 = 4000.",
         );
      } else {
         throw new Error("Valores incorretos calculados pelo backend.");
      }

      // 5. ISOLAMENTO MULTIUSUÁRIO NO SUMMARY
      console.log("\n[4/4] Verificando vazamento de dados (Multiusuário)...");
      const summaryB_response = await fetch(`${API_URL}/transactions/summary`, {
         headers: { Authorization: `Bearer ${tokenB}` },
      });

      if (summaryB_response.status !== 200) {
         console.log(
            "✅ Isolamento confirmado! O Usuário B não acessou os R$ 4000 do Usuário A.",
         );
      } else {
         const summaryB = await summaryB_response.json();
         const totalB = Number(summaryB.summary?.amount || 0);

         if (totalB === 0) {
            console.log(
               "✅ Isolamento confirmado! O Usuário B não vê as transações de A (Saldo: 0).",
            );
         } else {
            throw new Error(
               `VAZAMENTO DE DADOS GRAVE: O Usuário B está vendo R$ ${totalB} no resumo!`,
            );
         }
      }

      console.log("\n==================================================");
      console.log("🏆 RELATÓRIO: AUDITORIA DA FASE 3 CONCLUÍDA");
      console.log("==================================================");
   } catch (error: any) {
      console.error("\n❌ ERRO NA AUDITORIA:", error.message);
   }
}

runPhase3Audit();
