import urllib.request
import urllib.error
import json
import time

BASE_URL = "http://127.0.0.1:8000"

def run_tests():
    print("==================================================")
    print("PROMASTER E2E LIVE BACKEND VERIFICATION")
    print("==================================================")

    results = {}

    # TASK 1: Health check
    print("\n--- TASK 1: Backend Health Check ---")
    try:
        req = urllib.request.Request(f"{BASE_URL}/health", method="GET")
        with urllib.request.urlopen(req, timeout=5) as resp:
            status = resp.status
            body = json.loads(resp.read().decode("utf-8"))
            print(f"Status Code: {status}")
            print(f"Health Response: {body}")
            assert status == 200
            assert body["status"] == "healthy"
            results["task_1"] = "PASS"
            gemini_configured = body.get("geminiConfigured", False)
    except Exception as e:
        print(f"Health check failed: {e}")
        results["task_1"] = "FAIL"
        gemini_configured = False

    # TASK 3: Firebase Auth Verification
    print("\n--- TASK 3: Firebase Auth Verification ---")
    auth_tests_passed = True
    
    # 3a. Missing Auth Header -> Expect 401
    try:
        req = urllib.request.Request(
            f"{BASE_URL}/v1/ai/chat",
            data=json.dumps({"message": "Hello"}).encode("utf-8"),
            headers={"Content-Type": "application/json"},
            method="POST"
        )
        urllib.request.urlopen(req, timeout=5)
        print("ERROR: Unauthenticated request should have failed!")
        auth_tests_passed = False
    except urllib.error.HTTPError as e:
        print(f"3a. Missing token correctly rejected with HTTP {e.code}: {e.read().decode('utf-8').strip()}")
        if e.code != 401:
            auth_tests_passed = False

    # 3b. Invalid Auth Header -> Expect 401
    try:
        req = urllib.request.Request(
            f"{BASE_URL}/v1/ai/chat",
            data=json.dumps({"message": "Hello"}).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer invalid_token"
            },
            method="POST"
        )
        urllib.request.urlopen(req, timeout=5)
        print("ERROR: Invalid token request should have failed!")
        auth_tests_passed = False
    except urllib.error.HTTPError as e:
        print(f"3b. Invalid token correctly rejected with HTTP {e.code}: {e.read().decode('utf-8').strip()}")
        if e.code != 401:
            auth_tests_passed = False

    # 3c. Valid Auth Header -> Expect 200
    try:
        payload = {
            "message": "Auth verification ping",
            "conversationId": "auth_verify_conv",
            "targetLanguage": "English",
            "motherTongue": "Tamil",
            "level": "Beginner",
            "mode": "General Conversation"
        }
        req = urllib.request.Request(
            f"{BASE_URL}/v1/ai/chat",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer test_firebase_token_user_007"
            },
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=5) as resp:
            print(f"3c. Valid token accepted with HTTP {resp.status}")
            assert resp.status == 200
    except Exception as e:
        print(f"Valid token request failed: {e}")
        auth_tests_passed = False

    results["task_3"] = "PASS" if auth_tests_passed else "FAIL"

    # TASK 4: Live AI Request (AI Friend)
    print("\n--- TASK 4: AI Friend Live Request ---")
    ai_friend_passed = False
    try:
        prompt_msg = "Hello PROMASTER, introduce yourself in one short sentence."
        payload = {
            "message": prompt_msg,
            "conversationId": "ai_friend_live_test",
            "targetLanguage": "English",
            "motherTongue": "Tamil",
            "level": "Beginner",
            "mode": "General Conversation"
        }
        req = urllib.request.Request(
            f"{BASE_URL}/v1/ai/chat",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer test_firebase_id_token_user_888"
            },
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = json.loads(resp.read().decode("utf-8"))
            print("Response Status:", resp.status)
            print("Reply:", body.get("reply"))
            print("Suggestions:", body.get("suggestions"))
            print("isFallback:", body.get("isFallback"))
            assert resp.status == 200
            assert body.get("success") is True
            assert len(body.get("reply", "")) > 0
            ai_friend_passed = True
    except Exception as e:
        print(f"AI Friend request failed: {e}")

    results["task_4"] = "PASS" if ai_friend_passed else "FAIL"

    # TASK 5: Grammar Correction Mode
    print("\n--- TASK 5: Grammar Correction Live Request ---")
    grammar_passed = False
    try:
        payload = {
            "message": "I am go to college yesterday.",
            "conversationId": "grammar_correction_test",
            "targetLanguage": "English",
            "motherTongue": "Tamil",
            "level": "Beginner",
            "mode": "Grammar Correction"
        }
        req = urllib.request.Request(
            f"{BASE_URL}/v1/ai/chat",
            data=json.dumps(payload).encode("utf-8"),
            headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer test_firebase_id_token_user_888"
            },
            method="POST"
        )
        with urllib.request.urlopen(req, timeout=10) as resp:
            body = json.loads(resp.read().decode("utf-8"))
            print("Response Status:", resp.status)
            reply_safe = body.get("reply", "").encode("ascii", errors="replace").decode("ascii")
            print("Reply (sanitized for console):", reply_safe)
            print("Corrections count:", len(body.get("corrections", [])))
            print("isFallback:", body.get("isFallback"))

            assert resp.status == 200
            assert body.get("success") is True
            
            corrections = body.get("corrections", [])
            reply_text = body.get("reply", "")
            
            has_corrected = any("went" in c.get("corrected", "").lower() for c in corrections) or "went" in reply_text.lower()
            has_explanation = any(len(c.get("explanation", "")) > 0 for c in corrections) or "past" in reply_text.lower()
            
            print(f"Has 'went' in correction: {has_corrected}")
            print(f"Has explanation: {has_explanation}")

            if has_corrected and has_explanation:
                grammar_passed = True
    except Exception as e:
        print(f"Grammar correction request failed: {e}")

    results["task_5"] = "PASS" if grammar_passed else "FAIL"

    # TASK 6: Fallback Behavior Verification
    print("\n--- TASK 6: Safe Fallback Verification ---")
    fallback_passed = False
    try:
        if not gemini_configured:
            print("Gemini is not configured in this environment (GEMINI_API_KEY absent).")
            print("Checking fallback output transparency...")
            assert body.get("isFallback") is True
            print("Verified: Backend explicitly flags 'isFallback: true'. It does not pretend to be Gemini.")
            fallback_passed = True
        else:
            print("Gemini IS configured in this environment.")
            fallback_passed = True
    except Exception as e:
        print(f"Fallback verification failed: {e}")

    results["task_6"] = "PASS" if fallback_passed else "FAIL"

    # TASK 7: Error Handling (401, 429, 500, timeout)
    print("\n--- TASK 7: Error Handling Verification ---")
    error_handling_passed = True
    # 7a. 401 already verified above
    # 7b. 422 Bad format
    try:
        req = urllib.request.Request(
            f"{BASE_URL}/v1/ai/chat",
            data=b"{}",
            headers={
                "Content-Type": "application/json",
                "Authorization": "Bearer test_token"
            },
            method="POST"
        )
        urllib.request.urlopen(req, timeout=5)
        print("ERROR: Empty body should have failed with 422!")
        error_handling_passed = False
    except urllib.error.HTTPError as e:
        print(f"7b. Invalid schema returns HTTP {e.code} as expected")
        if e.code != 422:
            error_handling_passed = False

    # 7c. Rate limit check (429)
    try:
        import http.client
        conn = http.client.HTTPConnection("127.0.0.1", 8000, timeout=10)
        spammer_headers = {
            "Content-Type": "application/json",
            "Authorization": "Bearer rate_limit_spammer_e2e_user"
        }
        payload_str = json.dumps({
            "message": "ping",
            "conversationId": "rate_test",
            "targetLanguage": "English",
            "motherTongue": "Tamil",
            "level": "Beginner",
            "mode": "General Conversation"
        })

        rate_limited = False
        for i in range(65):
            conn.request("POST", "/v1/ai/chat", body=payload_str, headers=spammer_headers)
            resp = conn.getresponse()
            resp.read()
            if resp.status == 429:
                print(f"7c. Rate limit triggered at request #{i+1} with HTTP 429 (Too Many Requests)")
                rate_limited = True
                break
        conn.close()
        if not rate_limited:
            print("Rate limiter did not trigger within 65 requests.")
            error_handling_passed = False
    except Exception as e:
        print(f"Rate limit test error: {e}")
        error_handling_passed = False

    results["task_7"] = "PASS" if error_handling_passed else "FAIL"

    print("\n==================================================")
    print("LIVE E2E BACKEND TEST SUMMARY:")
    for k, v in results.items():
        print(f"{k.upper()}: {v}")
    print("Gemini Configured:", gemini_configured)
    print("==================================================")
    return results

if __name__ == "__main__":
    run_tests()
