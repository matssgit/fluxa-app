// Comando para rodar: npx tsx src/test/validate-phase1.ts
const API_URL = "http://localhost:3333";

// Gerador de e-mails únicos para podermos rodar o teste várias vezes sem dar erro de "e-mail já existe"
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
   console.log("==================================================");
   console.log("🛡️ INICIANDO AUDITORIA DA FASE 1 (AUTENTICAÇÃO)");
   console.log("==================================================\n");

   let tokenA = "";
   let tokenB = "";

   try {
      // ---------------------------------------------------------
      // 1. CADASTRO DE USUÁRIO (USER A e USER B)
      // ---------------------------------------------------------
      console.log("[1/6] Testando Cadastro de Usuários (POST /users)...");
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

      if (resCreateA.status === 201 && resCreateB.status === 201) {
         console.log(
            "✅ Usuários A e B criados com sucesso no banco de dados.",
         );
      } else {
         throw new Error(
            `Falha ao criar usuários. Status A: ${resCreateA.status}, Status B: ${resCreateB.status}`,
         );
      }

      // ---------------------------------------------------------
      // 2. LOGIN E GERAÇÃO DE JWT
      // ---------------------------------------------------------
      console.log(
         "\n[2/6] Testando Login e Geração de Token (POST /users/login)...",
      );
      const resLoginA = await fetch(`${API_URL}/users/login`, {
         method: "POST",
         headers: { "Content-Type": "application/json" },
         body: JSON.stringify({ email: userA.email, password: userA.password }),
      });
      const dataA = await resLoginA.json();

      if (resLoginA.status === 200 && dataA.token) {
         tokenA = dataA.token;
         console.log("✅ Login do Usuário A bem-sucedido. JWT recebido.");

         // Validando o payload básico do JWT (deve ter 3 partes separadas por ponto)
         if (tokenA.split(".").length === 3) {
            console.log(
               "✅ Payload do token estruturalmente válido (Header.Payload.Signature).",
            );
         }
      } else {
         throw new Error("Falha ao realizar login ou token não retornado.");
      }

      // Pegando o token do B também para o teste de isolamento
      const resLoginB = await fetch(`${API_URL}/users/login`, {
         method: "POST",
         headers: { "Content-Type": "application/json" },
         body: JSON.stringify({ email: userB.email, password: userB.password }),
      });
      tokenB = (await resLoginB.json()).token;

      // ---------------------------------------------------------
      // 3. ROTAS PROTEGIDAS SEM TOKEN
      // ---------------------------------------------------------
      console.log("\n[3/6] Testando bloqueio de rota sem Token...");
      // Tentando acessar os cartões sem enviar o Authorization
      const resNoToken = await fetch(`${API_URL}/credit/cards`);
      if (resNoToken.status === 401) {
         console.log(
            "✅ Sistema barrou acesso sem token (Retornou 401 Unauthorized).",
         );
      } else {
         console.log(
            `❌ ALERTA: A rota deveria ter retornado 401, mas retornou ${resNoToken.status}`,
         );
      }

      // ---------------------------------------------------------
      // 4. TOKEN INVÁLIDO OU FORJADO
      // ---------------------------------------------------------
      console.log("\n[4/6] Testando proteção contra Token Inválido...");
      const resInvalidToken = await fetch(`${API_URL}/credit/cards`, {
         headers: {
            Authorization: "Bearer token_inventado_ou_modificado_12345",
         },
      });
      if (resInvalidToken.status === 401) {
         console.log(
            "✅ Sistema barrou acesso com token inválido (Retornou 401 Unauthorized).",
         );
      } else {
         console.log(
            `❌ ALERTA: A rota aceitou um token falso! Status: ${resInvalidToken.status}`,
         );
      }

      // ---------------------------------------------------------
      // 5. ISOLAMENTO MULTIUSUÁRIO
      // ---------------------------------------------------------
      console.log("\n[5/6] Testando Isolamento Multiusuário...");
      // 5.1 Usuário A cria uma categoria
      const resCatA = await fetch(`${API_URL}/categories`, {
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

      // 5.2 Usuário B tenta listar as categorias
      const resListB = await fetch(`${API_URL}/categories`, {
         headers: { Authorization: `Bearer ${tokenB}` },
      });
      const dataCatB = await resListB.json();

      // Se a categoria criada pelo A aparecer na lista do B, o isolamento falhou
      const achouCategoriaDoA = dataCatB.categories?.some(
         (c: any) => c.name === "Categoria Secreta do A",
      );

      if (!achouCategoriaDoA) {
         console.log(
            "✅ Isolamento de dados aprovado! O Usuário B não consegue ver os dados do Usuário A.",
         );
      } else {
         console.log(
            "❌ VAZAMENTO DE DADOS: O Usuário B conseguiu acessar informações do Usuário A!",
         );
      }

      // ---------------------------------------------------------
      // CONCLUSÃO
      // ---------------------------------------------------------
      console.log("\n==================================================");
      console.log("🏆 RELATÓRIO: AUDITORIA CONCLUÍDA SEM ERROS FATAIS");
      console.log("==================================================");
      console.log(
         "Nota sobre Hash de Senha: Abra a tabela 'users' no banco de dados e confirme visualmente que a coluna 'password' está embaralhada (formato bcrypt) e não em texto limpo.",
      );
   } catch (error: any) {
      console.log("\n==================================================");
      console.log("❌ RELATÓRIO: AUDITORIA REPROVADA");
      console.log("==================================================");
      console.error(error.message);
   }
}

runAuthAudit();
