Title: Build and Deploy a Remote MCP Server to Google Cloud Run in Under 10 Minutes

URL Source: https://cloud.google.com/blog/topics/developers-practitioners/build-and-deploy-a-remote-mcp-server-to-google-cloud-run-in-under-10-minutes

Published Time: 2025-06-17

Markdown Content:
##### Jack Wotherspoon

Developer Advocate

Integrating context from tools and data sources into LLMs can be challenging, which impacts ease-of-use in the development of AI agents. To address this challenge, Anthropic introduced the [Model Context Protocol (MCP)](https://modelcontextprotocol.io/introduction), which standardizes how applications provide context to LLMs. Imagine you want to build an MCP server for your API to make it available to fellow developers so they can use it as context in their own AI applications. But where do you deploy it? Google Cloud Run could be a great option.

Drawing directly from the official [Cloud Run documentation](https://cloud.google.com/run/docs/host-mcp-servers) for hosting MCP servers, this guide shows you the straightforward process of setting up your very own remote MCP server. Get ready to transform how you leverage context in your AI endeavors!

MCP Transports
--------------

MCP follows a client-server architecture, and for a while, only supported running the server locally using the `stdio` transport.

![Image 1: https://storage.googleapis.com/gweb-cloudblog-publish/images/MCP-blog-image.max-2100x2100.png](https://storage.googleapis.com/gweb-cloudblog-publish/images/MCP-blog-image.max-2100x2100.png)

MCP has evolved and now supports remote access transports: `streamable-http` and `sse`. Server-Sent Events (SSE) has been deprecated in favor of Streamable HTTP in the latest [MCP specification](https://modelcontextprotocol.io/specification/2025-03-26/basic/transports#streamable-http) but is still supported for backwards compatibility. Both of these two transports allow for running MCP servers remotely.

With Streamable HTTP, the server operates as an independent process that can handle multiple client connections. This transport uses HTTP POST and GET requests.

The server **MUST** provide a single HTTP endpoint path (hereafter referred to as the MCP endpoint) that supports both POST and GET methods. For example, this could be a URL like `https://example.com/mcp`.

You can read more about the different transports in the [official MCP docs](https://modelcontextprotocol.io/docs/concepts/architecture#transport-layer).

Benefits of running an MCP server remotely
------------------------------------------

Running an MCP server remotely on Cloud Run can provide several benefits:

*   **Scalability**: Cloud Run is built to [rapidly scale out to handle all incoming requests](https://cloud.google.com/run/docs/about-instance-autoscaling). Cloud Run will scale your MCP server automatically based on demand.

*   **Centralized server**: You can share access to a centralized MCP server with team members through IAM privileges, allowing them to connect to it from their local machines instead of all running their own servers locally. If a change is made to the MCP server, all team members will benefit from it.

*   **Security**: Cloud Run provides an easy way to force authenticated requests. This allows only secure connections to your MCP server, preventing unauthorized access.

**IMPORTANT:** The security benefit is critical. If you don't enforce authentication, anyone on the public internet can potentially access and call your MCP server.

Prerequisites
-------------

*   Python 3.10+

*   Uv (for package and project management, see [docs for installation](https://docs.astral.sh/uv/getting-started/installation/))

*   Google Cloud SDK (gcloud)

Installation
------------

Create a folder, `mcp-on-cloudrun`, t o store the code for our server and deployment:

Let’s get started by using `uv` to create a project. [Uv](https://docs.astral.sh/uv/) is a powerful and fast package and project manager.

After running the above command, you should see the following `pyproject.toml`:

Next, let’s create the additional files we will need: a server.py for our MCP server code, a test_server.py that we will use to test our remote server, and a Dockerfile for our Cloud Run deployment.

Our file structure should now be complete:

Now that we have our file structure taken care of, let's configure our Google Cloud credentials and set our project:

Math MCP Server
---------------

LLMs are great at **non-deterministic tasks**: understanding intent, generating creative text, summarizing complex ideas, and reasoning about abstract concepts. However, they are notoriously unreliable for **deterministic tasks**– things that have one, and only one, correct answer.

Enabling LLMs with **deterministic tools** (such as math operations) is one example of how tools can provide valuable context to improve the use of LLMs using MCP.

We will use [FastMCP](https://gofastmcp.com/getting-started/welcome) to create a simple math MCP server that has two tools: `add` and `subtract`. FastMCP provides a fast, Pythonic way to build MCP servers and clients.

Add FastMCP as a dependency to our `pyproject.toml`:

Copy and paste the following code into `server.py`for our math MCP server:

lang-py

### Transport

We are using the `streamable-http` transport for this example as it is the recommended transport for remote servers, but you can also still use `sse` if you prefer as it is backwards compatible.

If you want to use `sse`, you will need to update the last line of `server.py` to use `transport="sse"`.

Deploying to Cloud Run
----------------------

Now let's deploy our simple MCP server to Cloud Run.

Copy and paste the below code into our empty `Dockerfile`; it uses `uv` to run our `server.py`:

You can deploy directly from source, or by using a container image.

For both options we will use the `--no-allow-unauthenticated` flag to **require** authentication.

This is important for security reasons. If you don't require authentication, anyone can call your MCP server and potentially cause damage to your system.

#### Option 1 - Deploy from source

#### Option 2 - Deploy from a container image

Create an [Artifact Registry](https://cloud.google.com/artifact-registry/docs) repository to store the container image.

Build the container image and push it to Artifact Registry with [Cloud Build](https://cloud.google.com/build).

Deploy our MCP server container image to Cloud Run.

Once you have completed either option, if your service has successfully deployed you will see a message like the following:

Authenticating MCP Clients
--------------------------

Since we specified `--no-allow-unauthenticated` to require authentication, any MCP client connecting to our remote MCP server will need to authenticate.

The official docs for [Host MCP servers on Cloud Run](https://cloud.google.com/run/docs/host-mcp-servers#authenticate_mcp_clients) provides more information on this topic depending on where you are running your MCP client.

For this example, we will run the [Cloud Run proxy](https://cloud.google.com/sdk/gcloud/reference/run/services/proxy) to create an authenticated tunnel to our remote MCP server on our local machines.

By default, the URL of Cloud Run services requires all requests to be authorized with the [Cloud Run Invoker](https://cloud.google.com/run/docs/securing/managing-access#invoker) (`roles/run.invoker`) IAM role. This IAM policy binding ensures that a strong security mechanism is used to authenticate your local MCP client.

Make sure that you or any team members trying to access the remote MCP server have the `roles/run.invoker` IAM role bound to their IAM principal (Google Cloud account).

**NOTE:** The following command may prompt you to download the Cloud Run proxy if it is not already installed. Follow the prompts to download and install it.

You should see the following output:

All traffic to `http://127.0.0.1:8080` will now be authenticated and forwarded to our remote MCP server.

Testing the remote MCP server
-----------------------------

Let's test and connect to the remote MCP server using the FastMCP client to connect to `http://127.0.0.1:8080/mcp` (note the `/mcp` at the end as we are using the Streamable HTTP transport) and call the `add` and `subtract` tools.

Add the following code to the empty `test_server.py`file:

lang-py

**NOTE:** Make sure you have the Cloud Run proxy running before running the test server.

In a **new terminal** run:

You should see the following output:

Posted in
*   [Developers & Practitioners](https://cloud.google.com/blog/topics/developers-practitioners)
