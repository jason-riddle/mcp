Title: Host MCP servers on Cloud Run

URL Source: https://cloud.google.com/run/docs/host-mcp-servers

Markdown Content:
Host MCP servers on Cloud Run | Cloud Run Documentation | Google Cloud

===============
[Skip to main content](https://cloud.google.com/run/docs/host-mcp-servers#main-content)

Host MCP servers on Cloud Run

bookmark_border bookmark Stay organized with collections  Save and categorize content based on your preferences.
===============================================================================================================================================

*   On this page
*   [Before you begin](https://cloud.google.com/run/docs/host-mcp-servers#before_you_begin)
*   [Host remote SSE or streamable HTTP MCP servers](https://cloud.google.com/run/docs/host-mcp-servers#host-remote-streamable-http-mcp-servers)
*   [Authenticate MCP clients](https://cloud.google.com/run/docs/host-mcp-servers#authenticate_mcp_clients)
    *   [Authenticate local MCP clients](https://cloud.google.com/run/docs/host-mcp-servers#authentic-local-mcp-clients)
    *   [Authenticate MCP clients running on Cloud Run](https://cloud.google.com/run/docs/host-mcp-servers#authenticate-clients-running-on-cloud-run)

*   [What's next](https://cloud.google.com/run/docs/host-mcp-servers#whats_next)

This guide shows how to host a [Model Context Protocol](https://modelcontextprotocol.io/) (MCP) server with streamable HTTP transport on Cloud Run, and provides guidance for authenticating MCP clients. If you're new to MCP, read the [Introduction guide](https://modelcontextprotocol.io/) for more context.

MCP is an open protocol that standardizes how AI agents interact with their environment. The AI agent hosts an _MCP client_, and the tools and resources it interacts with are _MCP servers_. The MCP client can communicate with the MCP server over two distinct transport types:

*   [Server Sent Events (SSE) or Streamable HTTP](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http)
*   [Standard Input/Output (stdio)](https://modelcontextprotocol.io/docs/concepts/transports#standard-input%2Foutput-stdio)

You can host MCP clients and servers on the same local machine, host an MCP client locally and have it communicate with remote MCP servers hosted on a cloud platform like Cloud Run, or host both the MCP client and server on a cloud platform.

Cloud Run supports hosting MCP servers with streamable HTTP transport, but not MCP servers with stdio transport.

The guidance on this page applies if you are developing your own MCP server or if you are using an existing MCP server.

*   **Developing your own MCP server**: We recommended that you use an MCP server SDK to develop your MCP server, such as the [official Python, TypeScript, Java, or Kotlin SDKs](https://modelcontextprotocol.io/) or [FastMCP](https://gofastmcp.com/).
*   **Existing MCP servers**: You can find a list of official and community MCP servers on the [MCP servers GitHub repository](https://github.com/modelcontextprotocol/servers#readme). Docker Hub also provides a [curated list of MCP servers](https://hub.docker.com/u/mcp).

Before you begin
----------------

1.    Sign in to your Google Cloud account. If you're new to Google Cloud, [create an account](https://console.cloud.google.com/freetrial) to evaluate how our products perform in real-world scenarios. New customers also get $300 in free credits to run, test, and deploy workloads.
2.   In the Google Cloud console, on the project selector page, select or create a Google Cloud project.

**Note**: If you don't plan to keep the resources that you create in this procedure, create a project instead of selecting an existing project. After you finish these steps, you can delete the project, removing all resources associated with the project.
[Go to project selector](https://console.cloud.google.com/projectselector2/home/dashboard)

3.   [Make sure that billing is enabled for your Google Cloud project](https://cloud.google.com/billing/docs/how-to/verify-billing-enabled#confirm_billing_is_enabled_on_a_project).

4.   [Set up your Cloud Run development environment](https://cloud.google.com/run/docs/setup) in your Google Cloud project.
5.   Ensure you have the appropriate [permissions to deploy services](https://cloud.google.com/run/docs/reference/iam/roles#additional-configuration), and the [Cloud Run Admin](https://cloud.google.com/iam/docs/roles-permissions/run#run.admin) (`roles/run.admin`) and [Service Account User](https://cloud.google.com/iam/docs/roles-permissions/iam#iam.serviceAccountUser) (`roles/iam.serviceAccountUser`) roles granted to your account.

#### Learn how to grant the roles

[Console](https://cloud.google.com/run/docs/host-mcp-servers#console)[gcloud](https://cloud.google.com/run/docs/host-mcp-servers#gcloud)More

    1.   In the Google Cloud console, go to the **IAM** page.

[Go to IAM](https://console.cloud.google.com/projectselector/iam-admin/iam?supportedpurview=project)
    2.    Select the project.
    3.    Click person_add**Grant access**.
    4.   In the **New principals** field, enter your user identifier. This is typically the Google Account email address that is used to deploy the Cloud Run service.

    5.    In the **Select a role** list, select a role.
    6.    To grant additional roles, click add**Add another role** and add each additional role.
    7.    Click **Save**.

To grant the required IAM roles to your account on your project:

 gcloud projects add-iam-policy-binding PROJECT_ID \
 --member=PRINCIPAL \
 --role=ROLE

Replace:

    *   PROJECT_NUMBER with your Google Cloud project number.
    *   PROJECT_ID with your Google Cloud project ID.
    *   PRINCIPAL with the account you are adding the binding for. This is typically the Google Account email address that is used to deploy the Cloud Run service.
    *   ROLE with the role you are adding to the deployer account.

Host remote SSE or streamable HTTP MCP servers
----------------------------------------------

MCP servers that use the Server-sent events (SSE) or streamable HTTP transport can be hosted remotely from their MCP clients.

To deploy this type of MCP server to Cloud Run, you can deploy the MCP server as a container image or as source code (commonly Node.js or Python), depending on how the MCP server is packaged.

[Container images](https://cloud.google.com/run/docs/host-mcp-servers#container-images)[Sources](https://cloud.google.com/run/docs/host-mcp-servers#sources)More

Remote MCP servers distributed as container images are web servers that listen for HTTP requests on a specific port, which means they adhere to Cloud Run's [container runtime contract](https://cloud.google.com/run/docs/container-contract#port) and can be deployed to a Cloud Run service.

To deploy an MCP server packaged as a container image, you need to have the URL of the container image and the port on which it expects to receive requests. These can be [deployed](https://cloud.google.com/run/docs/deploying) using the following gcloud CLI command:

gcloud run deploy --image IMAGE_URL --port PORT
Replace:

*   `IMAGE_URL` with the container image URL, for example `us-docker.pkg.dev/cloudrun/container/mcp`.
*   `PORT` with the port it listens on, for example `3000`.

Remote MCP servers that are not provided as container images can be deployed to Cloud Run [from their sources](https://cloud.google.com/run/docs/deploying-source-code), notably if they are written in Node.js or Python.

1.   Clone the Git repository of the MCP server:

 git clone https://github.com/ORGANIZATION/REPOSITORY.git
2.   Navigate to the root of the MCP server:

 cd REPOSITORY
3.   Deploy to Cloud Run with the following gcloud CLI command:

 gcloud run deploy --source .

After you deploy your HTTP MCP server to Cloud Run, the MCP server gets a [HTTPS URL](https://cloud.google.com/run/docs/triggering/https-request#url) and communication can use Cloud Run's built in support for HTTP response streaming.

Authenticate MCP clients
------------------------

Depending on where you hosted the MCP client, see the section that is relevant for you:

*   [Authenticate local MCP clients](https://cloud.google.com/run/docs/host-mcp-servers#authentic-local-mcp-clients)
*   [Authenticate MCP clients running on Cloud Run](https://cloud.google.com/run/docs/host-mcp-servers#authenticate-clients-running-on-cloud-run)

### Authenticate local MCP clients

If the AI agent hosting the MCP client runs on a local machine, use one of the following methods to authenticate the MCP client:

*   [IAM invoker permission](https://cloud.google.com/run/docs/host-mcp-servers#iam-invoker)
*   [OIDC ID token](https://cloud.google.com/run/docs/host-mcp-servers#oidc-id-token)

For more information, refer to the [MCP specification on Authentication](https://modelcontextprotocol.io/specification/2025-03-26/basic/authorization).

#### IAM invoker permission

By default, the URL of Cloud Run services requires all requests to be authorized with the [Cloud Run Invoker](https://cloud.google.com/run/docs/securing/managing-access#invoker) (`roles/run.invoker`) IAM role. This IAM policy binding ensures that a strong security mechanism is used to authenticate your local MCP client.

After deploying your MCP server to a Cloud Run service in a region, run the [Cloud Run proxy](https://cloud.google.com/sdk/gcloud/reference/run/services/proxy) on your local machine to securely expose the remote MCP server to your client using your own credentials:

```
gcloud run services proxy MCP_SERVER_NAME --region REGION --port=3000
```

Replace:

*   MCP_SERVER_NAME with the name of your Cloud Run service.
*   REGION with the Google Cloud [region](https://cloud.google.com/run/docs/locations) where you deployed your service. For example, `europe-west1`.

The Cloud Run proxy command creates a local proxy on port `3000` that forwards requests to the remote MCP server and injects your identity.

Update the MCP configuration file of your MCP client with the following:

```
{
  "mcpServers": {
    "cloud-run": {
      "url": "http://localhost:3000/sse"
    }
  }
}
```

If your MCP client does not support the `url` attribute, use the [`mcp-remote` npm package](https://www.npmjs.com/package/mcp-remote):

```
{
  "mcpServers": {
    "cloud-run": {
      "command": "npx",
      "args": [
        "-y",
        "mcp-remote",
        "http://localhost:3000/sse"
      ]
    }
  }
}
```

#### OIDC ID token

Depending on whether the MCP client exposes headers or uses a way of providing a custom authenticated transport, you might consider authenticating the MCP client with an [OIDC ID token](https://cloud.google.com/docs/authentication/get-id-token).

You can use various Google authentication libraries to get an ID token from the runtime environment, for example the [Google Auth Library for Python](https://google-auth.readthedocs.io/en/stable/reference/google.oauth2.id_token.html). This token must have the correct audience claim that matches the receiving service's `*.run.app` URL, unless you use [custom audiences](https://cloud.google.com/run/docs/configuring/custom-audiences). You must also include the ID token in client requests, such as `Authorization: Bearer <token value>`.

If the MCP client does not expose either headers or transport, use a different authentication method.

### Authenticate MCP clients running on Cloud Run

If the AI agent hosting the MCP client runs on Cloud Run, use one of the following methods to authenticate the MCP client:

*   [Deploy as a sidecar](https://cloud.google.com/run/docs/host-mcp-servers#deploy-as-sidecar)
*   [Authenticate service to service](https://cloud.google.com/run/docs/host-mcp-servers#authenticate-service-to-service)
*   [Use Cloud Service Mesh](https://cloud.google.com/run/docs/host-mcp-servers#use-service-mesh)

#### Deploy the MCP server as a sidecar

The MCP server can be [deployed as a sidecar](https://cloud.google.com/run/docs/deploying#sidecars) where the MCP client runs.

No specific authentication is required for this use case, since the MCP client and MCP server are on the same instance. The client can connect to the MCP server using a port on `http://localhost:PORT`. Replace PORT with a different port than the one used to send requests to the Cloud Run service.

#### Authenticate service to service

If the MCP server and MCP client run as distinct Cloud Run services, see [Authenticating service-to-service](https://cloud.google.com/run/docs/authenticating/service-to-service).

#### Use Cloud Service Mesh

An agent hosting an MCP client can connect to a remote MCP server using [Cloud Service Mesh](https://cloud.google.com/service-mesh/docs/configure-cloud-service-mesh-for-cloud-run).

You can configure the MCP server service to have a short name on the mesh, and the MCP client can communicate to the MCP server using the short name `http://mcp-server`. Authentication is managed by the mesh.

What's next
-----------

*   [Host AI agents on Cloud Run](https://cloud.google.com/run/docs/ai-agents).
*   Follow a tutorial to [build and deploy a remote MCP server to Cloud Run](https://cloud.google.com/blog/topics/developers-practitioners/build-and-deploy-a-remote-mcp-server-to-google-cloud-run-in-under-10-minutes) in under 10 minutes.
