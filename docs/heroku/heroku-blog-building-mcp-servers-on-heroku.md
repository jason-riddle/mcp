Title: Heroku AI: Build and Deploy Enterprise Grade MCP Servers

URL Source: https://www.heroku.com/blog/building-mcp-servers-on-heroku/

Published Time: 2025-05-15T15:58:21Z

Markdown Content:
Agents hold immense power, but their true potential shines when they connect to the real world, fetching data, triggering actions, or leveraging external tools. The Model Context Protocol (MCP) offers a standardized way for AI agents to do this.

> MCP is an open protocol that standardizes how applications provide context to LLMs. Think of MCP like a USB-C port for AI applications. Just as USB-C provides a standardized way to connect your devices to various peripherals and accessories, MCP provides a standardized way to connect AI models to different data sources and tools.
>
>
> – [official MCP website](https://modelcontextprotocol.io/introduction)

[**Heroku Managed Inference and Agents**](https://elements.heroku.com/addons/heroku-inference) dramatically simplifies hosting these MCP servers and making them available, not only to itself, but also to external agents like [Claude,](https://www.anthropic.com/claude)[Cursor](https://www.cursor.com/), or [Agentforce](https://www.salesforce.com/agentforce/). These new capabilities accelerate industry standardization towards [agent interoperability](https://www.salesforce.com/blog/agent-interoperability/) by reducing the infrastructure, security, and discovery challenges in building and running MCP servers. Heroku Managed Inference and Agents provides:

*   **Community SDK support**: Build your servers using the official MCP SDK, or any other MCP SDK of your choice.
*   **Effortless Management**: Once you have a server running, set up your Procfile and push to Heroku. The Managed Inference and Agents add-on automatically manages server registration with the MCP Toolkit.
*   **Unified Endpoint**: Managed Inference and Agents automatically has access to all registered servers. Additionally, a MCP Toolkit URL is generated, which can be used to access your servers in external clients.
*   **Only Pay for What You Use**: MCP servers managed by the MCP Toolkit are spun up when in use, and are spun down when there are no requests.

![Image 1: Diagram showing the workflow of a customer's Heroku account, detailing inference models, apps, Heroku managed inference, MCP protocol, and connections to external resources.](https://www.heroku.com/wp-content/uploads/2025/05/heroku-managed-inference-and-agents-diagram-wide-icons.png)

This guide walks you through setting up your own MCP server on Heroku and enabling your Agent to securely and efficiently perform real-world tasks.

Before getting started
----------------------

MCP Servers are just like any other software application, and therefore can be deployed to Heroku as standalone apps. So while you could build your own multi-tenant SSE server and deploy it yourself, Heroku MCP Toolkits help you do things that standalone servers cannot do.

1.   First and foremost, they make it seamless to integrate servers with your Heroku Managed Inference and Agents.
2.   Secondly, they allow tools to be scaled to 0 by default, and spun up only when needed – making them more cost efficient for infrequent requests.
3.   Thirdly, they provide code isolation which enables secure code execution for LLM generated code.
4.   Finally, they wrap multiple servers in a single url making it incredibly easy to connect with external clients.

Getting started: Create and deploy your first MCP Server
--------------------------------------------------------

1.   Step 1 – Build your Server
    1.   Use an [official MCP SDK](https://github.com/modelcontextprotocol) to create an MCP Server. Note: At this stage, Heroku MCP Toolkits **only support STDIO servers**. We are working on streamlining platform support for SSE/http servers with authentication.
    2.   MCP Servers are normal Heroku Apps built on the language of your choice. For example, if you are using node, you’ll want to follow best practices and ensure your node and npm engines are set in your package.json like you would typically for a node app on Heroku.

2.   Step 2 – Add the MCP process type
    1.   Define your MCP process via Procfile with a process prefix of `mcp*`. E.g. `mcp-heroku: npm start` ([example](https://github.com/heroku/mcp-doc-reader/blob/main/Procfile#L2))

3.   Step 3 – Deploy your server
    1.   Once your app is deployed, all `mcp*` process types will be ready to be picked up by the Heroku Managed Inference and Agents add-on.

For more examples, take a look at the sample servers listed in our [dev center documentation](https://devcenter.heroku.com/articles/heroku-inference-working-with-mcp#registering-using-custom-mcp-servers-with-heroku).

Creating an MCP Toolkit
-----------------------

Attach the [Heroku Managed Inference and Agents add-on](https://elements.heroku.com/addons/heroku-inference) to the app that you just created. This will register any apps defined in the app to the MCP Toolkit. Each new Managed Inference and Agents add-on will correspond to a new MCP Toolkit.

1.   **Navigate to Your App**: Open your application’s dashboard on Heroku.
2.   **Go to Resources**: Select the “Resources” tab.
3.   **Add Managed Inference and Agents**: Search for “Managed Inference and Agents” in the add-ons section and add it to your app.

### What plan to select

Each Managed Inference and Agents plan has a corresponding model (ex. Claude 3.5 Haiku or Stable Image Ultra). You should select the model that aligns with your needs. If your goal is to give your model access to MCP tools, then you will need to select one of the Claude chat models. If you have no need for a model, and only want to host MCP tools for external use, that can be done by selecting any plan. Inference usage is metered, so you will incur no cost if there is no usage of Heroku managed models.

As far as the MCP servers are concerned, you will pay for the dyno units consumed by the [one-off dynos](https://devcenter.heroku.com/articles/one-off-dynos) that are spun up. The cost of tool calls depends on the specific dyno tier selected for your app, but the default eco dynos, that is about .0008 cents/second. Each individual tool call is capped at 300 seconds.

If you decide to host your inference on Heroku, your inference model will have the following [default tools](https://devcenter.heroku.com/articles/heroku-inference-tools) free of charge. This includes tools like Code Execution and Document/Web Reader.

Managing and using your MCP Toolkit
-----------------------------------

The MCP Toolkit configuration can be viewed and managed through a user-friendly tab in the Heroku Managed Inference and Agents add-on. As with all add-ons, navigate to the App Resources page, and click on the Managed Inference and Agents add-on that you provisioned. Navigate to the Tools tab. Here, you will find the following information:

1.   The list of registered servers, and their statuses
2.   The list of tools per server, along with their request schemas

These tools are all available to your selected Managed Inference model with no extra configuration. Additionally, you will find the MCP Toolkit URL and MCP Toolkit Token on this page, which can be used for integration with external MCP Clients. The MCP Toolkit Token is masked by default for security.

Caution: Your MCP Toolkit Token can be used to trigger actions in your registered MCP servers, so avoid sharing it unless necessary.

For more information, check out the [dev center](https://devcenter.heroku.com/articles/heroku-inference-tools) documentation.

![Image 2: Screenshot of a web dashboard showing model information, CLI usage instructions, toolkit integration details with an inference URL and key, and a list of MCP servers.](https://www.heroku.com/wp-content/uploads/2025/05/mcp-toolkit-integration-e1747259347259.png)

Coming soon
-----------

We are actively working on simplifying the process of building SSE/HTTP servers with auth endpoints – both for Heroku Managed Inference and Agents, and for external MCP clients. This will make it possible for servers to access user specific resources, while adhering to the recommended security standards. Additionally, we are building an in-dashboard playground for Managed Inference and Agents so you can run quick experiments with your models and tools.

We are excited to see what you build with Heroku Managed Inference and Agents and MCP on Heroku! Attend our [webinar on May 28](https://invite.salesforce.com/aipaasintroducingherokumiaandm) to see a demo and get your questions answered!
