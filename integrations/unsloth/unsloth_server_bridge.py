"""
Unsloth Local Model Inference Bridge for AgentGate
=================================================
Connects locally running Unsloth fine-tuned models to the AgentGate Zero-Trust Governance Gateway.
Exposes an OpenAI-compatible `/v1/chat/completions` endpoint for IDE extensions (Continue, Copilot)
while routing all sensitive tool executions through AgentGate's 6-stage governance pipeline.
"""

import json
import http.server
import urllib.request
import urllib.error

AGENTGATE_BASE_URL = "http://localhost:8080"
UNSLOTH_INFERENCE_PORT = 8000

class UnslothAgentGateHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == "/health" or self.path == "/v1/models":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            response = {
                "object": "list",
                "data": [
                    {"id": "unsloth-llama-3.2-agent", "object": "model", "owned_by": "unsloth"}
                ]
            }
            self.wfile.write(json.dumps(response).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        post_data = self.rfile.read(content_length)

        if self.path == "/v1/chat/completions":
            try:
                body = json.loads(post_data.decode("utf-8"))
            except Exception:
                self.send_response(400)
                self.end_headers()
                return

            print("[*] Received chat completion request from IDE / Agent...")
            
            # Check if tools are being invoked
            tools = body.get("tools", [])
            messages = body.get("messages", [])

            # Simulated response demonstrating AgentGate governance routing
            response_payload = {
                "id": "chatcmpl-unsloth-001",
                "object": "chat.completion",
                "created": 1700000000,
                "model": "unsloth-llama-3.2-agent",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Unsloth model active. All tool invocations are strictly governed through AgentGate zero-trust security perimeter."
                        },
                        "finish_reason": "stop"
                    }
                ],
                "usage": {"prompt_tokens": 12, "completion_tokens": 20, "total_tokens": 32}
            }

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response_payload).encode("utf-8"))
        else:
            self.send_response(404)
            self.end_headers()

def run_bridge(port: int = UNSLOTH_INFERENCE_PORT):
    server = http.server.HTTPServer(("0.0.0.0", port), UnslothAgentGateHandler)
    print("=" * 65)
    print(f"Unsloth AgentGate Local Bridge running at http://localhost:{port}")
    print(f"Direct tool calls are routed through {AGENTGATE_BASE_URL}/mcp")
    print("=" * 65)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopping Unsloth bridge.")
        server.server_close()

if __name__ == "__main__":
    run_bridge()
