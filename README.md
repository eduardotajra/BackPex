---

# BackPex - Mochilas Customizáveis com Níveis

**BackPex** é um plugin para servidores **Spigot/Paper (API 1.20+)** que introduz mochilas evolutivas com sistema de níveis e personalização. Crie sua mochila através de um ritual mágico, use-a para armazenar itens e evolua-a para aumentar sua capacidade.

---

## 🌟 Funcionalidades

* **Criação Mística**
  Crie uma BackPex realizando um ritual com itens específicos jogados em um caldeirão com água.

* **Sistema de Níveis**
  Comece com uma mochila pequena e evolua por até 12 níveis, expandindo o espaço de armazenamento.

* **Totalmente Configurável**
  Todos os custos de upgrade são ajustáveis no arquivo `config.yml`.

* **Mochila como Item e Bloco**
  Utilize diretamente do inventário ou coloque no mundo como um baú funcional.

* **Segurança e Propriedade**
  Cada mochila tem um dono. Use `/backpex lock` para impedir que outros jogadores acessem ou quebrem sua BackPex.

* **Persistência de Dados**
  Os itens armazenados são mantidos com segurança, mesmo se a mochila estiver no mundo como bloco.

* **Interface Intuitiva**
  Sistema de páginas e menus para navegação e evolução da mochila.

---

## 🚀 Como Começar

### 1. Criando sua BackPex

Para criar sua primeira mochila:

* Encontre ou coloque um **Caldeirão com Água**.
* Jogue ao redor do caldeirão os seguintes itens:

    * `1x Baú (normal)`
    * `1x Mesa de Trabalho`
    * `1x Corante Roxo`
* Clique com o botão direito no caldeirão.
  Se feito corretamente, os itens serão consumidos, parte da água desaparecerá, e você receberá uma **BackPex Nível 0**.

### 2. Usando a BackPex

* **Abrir a Mochila:** clique com o botão direito com a BackPex na mão.
* **Colocar no Chão:** clique com o botão direito em um bloco.
* **Pegar do Chão:** agache-se e clique com o botão direito na mochila (apenas o dono pode fazer isso).
* **Quebrar a Mochila:** somente o dono pode quebrar uma mochila trancada. O item da mochila com seu conteúdo será dropado.

### 3. Evoluindo a BackPex

* Abra a GUI da BackPex.
* Clique no item que representa o nível atual da mochila.
* Veja os materiais necessários para o próximo nível.
* Se possuir os recursos, clique no botão verde para evoluir.

---

## 🛠️ Comandos e Permissões

| Comando           | Descrição                                             | Permissão        | Padrão |
| ----------------- | ----------------------------------------------------- | ---------------- | ------ |
| `/backpex lock`   | Tranca ou destranca a mochila que está sendo segurada | `backpex.lock`   | true   |
| `/backpex reload` | Recarrega o arquivo `config.yml`                      | `backpex.reload` | op     |

---

## ⚙️ Configuração (`config.yml`)

O arquivo `config.yml` permite definir os custos de evolução por nível.

```yaml
upgrades:
  # DO nível 0 PARA o nível 1
  level-0:
    required-material: 'OAK_PLANKS'
    required-amount: 64

  # DO nível 1 PARA o nível 2
  level-1:
    required-material: 'OAK_LOG'
    required-amount: 128

  # E assim por diante até o nível 11...
```

Utilize nomes de materiais válidos da API do Spigot. [Lista oficial de materiais aqui](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html).

---

## 💾 Instalação

1. Baixe o arquivo `BackPex.jar`.
2. Coloque-o na pasta `plugins` do seu servidor Spigot/Paper.
3. Reinicie ou recarregue o servidor.
4. A pasta `BackPex/` com o arquivo `config.yml` será gerada automaticamente.
5. Edite o arquivo de configuração conforme desejado.

---

## 👨‍💻 Para Desenvolvedores

Se você deseja modificar o código ou compilá-lo a partir do fonte, siga os passos abaixo.

### Pré-requisitos

- Java Development Kit (JDK) 17 ou superior
- Apache Maven 3.8 ou superior

### Compilando a Partir do Código-Fonte

```bash
# Clone o repositório:
git clone https://github.com/eduardotajra/BackPex.git

# Navegue até o diretório do projeto:
cd BackPex

# Execute o comando de compilação do Maven:
mvn clean package
```

O arquivo `BackPex-1.0.jar` estará localizado na pasta `target`.
Utilize o `BackPex-1.0-SNAPSHOT-shaded.jar`.

---

## Autor

Plugin desenvolvido por **Eduardo Tajra**.

---
