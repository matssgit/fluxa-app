export class AvatarStorage {
  private static readonly LOCAL_KEY = "@Fluxa:local_avatar";
  private static readonly DB_IDENTIFIER = "local_cache";

  // Avalia se a string é um binário Base64 em memória
  static isLocalBase64(url?: string | null): boolean {
    return !!url && url.startsWith("data:image");
  }

  // Armazena localmente e devolve apenas a etiqueta para o Banco de Dados
  static save(base64Image: string): string {
    localStorage.setItem(this.LOCAL_KEY, base64Image);
    return this.DB_IDENTIFIER;
  }

  // Intercepta a leitura. Se a etiqueta for local, puxa da memória. Senão, usa a URL real.
  static load(dbAvatarUrl?: string | null): string | null {
    if (dbAvatarUrl === this.DB_IDENTIFIER) {
      return localStorage.getItem(this.LOCAL_KEY) || null;
    }
    return dbAvatarUrl || null;
  }

  // Limpa o armazenamento
  static clear(): void {
    localStorage.removeItem(this.LOCAL_KEY);
  }
}
