import urllib.request
import json
import time

# Probe health endpoint first
print("--- PROMASTER BACKEND HEALTH PROBE ---")
try:
    health_req = urllib.request.Request("http://127.0.0.1:8000/health")
    with urllib.request.urlopen(health_req, timeout=5) as resp:
        h = json.loads(resp.read().decode("utf-8"))
        print(f"Status: {h.get('status')} | Model: {h.get('model')} | Gemini Configured: {h.get('geminiConfigured')}")
except Exception as e:
    print(f"Health check failed: {e}")

modes = [
    ("AI Friend", "Hello! How are you doing today?"),
    ("Language Coach", "How can I practice English past tense?"),
    ("Grammar Correction", "She do not like coffee."),
    ("Speaking Feedback", "Can you give me a tongue twister for pronunciation?"),
    ("Vocabulary Help", "What does serendipity mean?"),
    ("Translation", "Translate: Good morning friend"),
    ("General Conversation", "What is your favorite topic to talk about?"),
    ("AI Assistant", "Open YouTube")
]

results = []
headers = {
    "Content-Type": "application/json",
    "Authorization": "Bearer test_user_modes_gemini"
}

for i, (mode_name, msg) in enumerate(modes):
    mode_headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer test_user_mode_{i}"
    }
    payload = {
        "message": msg,
        "conversationId": f"mode_{mode_name.lower().replace(' ', '_')}",
        "targetLanguage": "English",
        "motherTongue": "Tamil",
        "level": "Beginner",
        "mode": mode_name
    }
    req = urllib.request.Request(
        "http://127.0.0.1:8000/v1/ai/chat",
        data=json.dumps(payload).encode("utf-8"),
        headers=mode_headers,
        method="POST"
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            clean_summary = data.get("reply", "")[:80].replace("\n", " ").encode("ascii", errors="replace").decode("ascii").strip()
            results.append({
                "mode": mode_name,
                "http_status": resp.status,
                "success": data.get("success"),
                "isFallback": data.get("isFallback"),
                "summary": clean_summary
            })
    except Exception as e:
        results.append({
            "mode": mode_name,
            "http_status": "ERROR",
            "success": False,
            "isFallback": None,
            "summary": str(e)
        })
    time.sleep(1.0)

print("\n--- ALL 8 AI MODES VERIFICATION RESULTS ---")
for r in results:
    print(f"Mode: {r['mode']:<22} | HTTP: {r['http_status']} | Success: {r['success']} | isFallback: {r['isFallback']} | Summary: {r['summary'][:50]}...")
